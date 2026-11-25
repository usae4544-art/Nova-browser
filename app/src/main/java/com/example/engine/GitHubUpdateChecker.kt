package com.example.engine

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTag: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String,
    val apkSizeBytes: Long = 0L
) {
    val formattedApkSize: String
        get() = if (apkSizeBytes > 0L) {
            String.format(Locale.US, "%.1f MB", apkSizeBytes / (1024.0 * 1024.0))
        } else {
            ""
        }
}

object GitHubUpdateChecker {
    const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/usae4544-art/Nova-browser/releases/latest"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Checks GitHub Releases API synchronously (call on Dispatchers.IO).
     * Returns [AppUpdateInfo] when a newer release with an .apk asset is published, or null otherwise.
     */
    fun fetchLatestUpdate(currentVersion: String): Result<AppUpdateInfo?> {
        return runCatching {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "NovaBrowser-Android/$currentVersion")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("GitHub API returned HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                parseLatestReleaseJson(body, currentVersion)
            }
        }
    }

    fun parseLatestReleaseJson(jsonBody: String, currentVersion: String): AppUpdateInfo? {
        if (jsonBody.isBlank()) return null
        val json = runCatching { JSONObject(jsonBody) }.getOrNull() ?: return null
        if (json.optBoolean("draft", false)) return null

        val rawTag = json.optString("tag_name", "").trim()
        val normalizedLatest = normalizeVersionString(rawTag)
        if (normalizedLatest.isBlank() || !isNewerVersion(normalizedLatest, currentVersion)) {
            return null
        }

        val assets = json.optJSONArray("assets")
        var apkDownloadUrl = ""
        var apkSize = 0L
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val assetUrl = asset.optString("browser_download_url", "")
                if (assetUrl.isNotBlank() &&
                    (assetName.endsWith(".apk", ignoreCase = true) ||
                        assetUrl.endsWith(".apk", ignoreCase = true))
                ) {
                    apkDownloadUrl = assetUrl
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }
        if (apkDownloadUrl.isBlank()) return null

        val releaseTitle = json.optString("name", rawTag).ifBlank { "Nova Browser $rawTag" }
        val rawBody = json.optString("body", "").trim()
        val cleanedNotes = cleanReleaseNotes(rawBody)
        val releasePageUrl = json.optString(
            "html_url",
            "https://github.com/usae4544-art/Nova-browser/releases/latest"
        )

        return AppUpdateInfo(
            currentVersion = normalizeVersionString(currentVersion).ifBlank { currentVersion },
            latestVersion = normalizedLatest,
            releaseTag = rawTag,
            releaseTitle = releaseTitle,
            releaseNotes = cleanedNotes,
            apkDownloadUrl = apkDownloadUrl,
            releasePageUrl = releasePageUrl,
            apkSizeBytes = apkSize
        )
    }

    fun isNewerVersion(latestTagOrVersion: String, currentVersion: String): Boolean {
        val latestParts = parseNumericParts(latestTagOrVersion)
        val currentParts = parseNumericParts(currentVersion)
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun normalizeVersionString(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
            .trim()
    }

    private fun parseNumericParts(version: String): List<Int> {
        val norm = normalizeVersionString(version)
        if (norm.isBlank()) return emptyList()
        return norm.split('.').map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    private fun cleanReleaseNotes(raw: String): String {
        if (raw.isBlank()) return "Performance improvements, Liquid Glass polish, and bug fixes."
        return raw.lines()
            .asSequence()
            .map { line ->
                line.trim()
                    .removePrefix("### ")
                    .removePrefix("## ")
                    .removePrefix("# ")
                    .replace("**", "")
                    .replace("`", "")
            }
            .filter { it.isNotBlank() && !it.startsWith("APK SHA-256", ignoreCase = true) }
            .take(8)
            .joinToString("\n")
            .ifBlank { "Performance improvements, Liquid Glass polish, and bug fixes." }
    }
}
