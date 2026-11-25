package com.example.engine

import okhttp3.OkHttpClient
import okhttp3.Request
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
        @Suppress("UNCHECKED_CAST")
        val json = runCatching { LightweightJsonParser(jsonBody).parseValue() as? Map<String, Any?> }
            .getOrNull() ?: return null
        if ((json["draft"] as? Boolean) == true) return null

        val rawTag = (json["tag_name"] as? String).orEmpty().trim()
        val normalizedLatest = normalizeVersionString(rawTag)
        if (normalizedLatest.isBlank() || !isNewerVersion(normalizedLatest, currentVersion)) {
            return null
        }

        val assets = json["assets"] as? List<*>
        var apkDownloadUrl = ""
        var apkSize = 0L
        if (assets != null) {
            for (item in assets) {
                val asset = item as? Map<*, *> ?: continue
                val assetName = asset["name"] as? String ?: ""
                val assetUrl = asset["browser_download_url"] as? String ?: ""
                if (assetUrl.isNotBlank() &&
                    (assetName.endsWith(".apk", ignoreCase = true) ||
                        assetUrl.endsWith(".apk", ignoreCase = true))
                ) {
                    apkDownloadUrl = assetUrl
                    apkSize = (asset["size"] as? Number)?.toLong() ?: 0L
                    break
                }
            }
        }
        if (apkDownloadUrl.isBlank()) return null

        val releaseTitle = (json["name"] as? String).orEmpty().ifBlank { rawTag }
            .ifBlank { "Nova Browser $rawTag" }
        val rawBody = (json["body"] as? String).orEmpty().trim()
        val cleanedNotes = cleanReleaseNotes(rawBody)
        val releasePageUrl = (json["html_url"] as? String).orEmpty()
            .ifBlank { "https://github.com/usae4544-art/Nova-browser/releases/latest" }

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

    /** Minimal dependency-free RFC-8259 JSON parser so release parsing works identically on Android and JVM tests. */
    private class LightweightJsonParser(private val input: String) {
        private var index = 0

        fun parseValue(depth: Int = 0): Any? {
            if (depth > 32) error("JSON nesting too deep")
            skipWhitespace()
            if (index >= input.length) error("Unexpected end of JSON")
            return when (val ch = input[index]) {
                '{' -> parseObject(depth + 1)
                '[' -> parseArray(depth + 1)
                '"' -> parseString()
                't' -> expectLiteral("true", true)
                'f' -> expectLiteral("false", false)
                'n' -> expectLiteral("null", null)
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected character '$ch' at index $index")
            }
        }

        private fun parseObject(depth: Int): Map<String, Any?> {
            index++ // '{'
            skipWhitespace()
            val map = LinkedHashMap<String, Any?>()
            if (index < input.length && input[index] == '}') {
                index++
                return map
            }
            while (true) {
                skipWhitespace()
                if (index >= input.length || input[index] != '"') error("Expected object key")
                val key = parseString()
                skipWhitespace()
                if (index >= input.length || input[index] != ':') error("Expected ':'")
                index++
                map[key] = parseValue(depth)
                skipWhitespace()
                if (index >= input.length) error("Unterminated object")
                when (input[index]) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return map
                    }
                    else -> error("Expected ',' or '}' in object")
                }
            }
        }

        private fun parseArray(depth: Int): List<Any?> {
            index++ // '['
            skipWhitespace()
            val list = ArrayList<Any?>()
            if (index < input.length && input[index] == ']') {
                index++
                return list
            }
            while (true) {
                list.add(parseValue(depth))
                skipWhitespace()
                if (index >= input.length) error("Unterminated array")
                when (input[index]) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return list
                    }
                    else -> error("Expected ',' or ']' in array")
                }
            }
        }

        private fun parseString(): String {
            index++ // opening '"'
            val sb = StringBuilder()
            while (index < input.length) {
                val ch = input[index++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (index >= input.length) error("Unterminated escape")
                        when (val esc = input[index++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (index + 4 > input.length) error("Invalid unicode escape")
                                val hex = input.substring(index, index + 4)
                                sb.append(hex.toInt(16).toChar())
                                index += 4
                            }
                            else -> error("Unknown escape \\$esc")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            error("Unterminated string")
        }

        private fun parseNumber(): Number {
            val start = index
            if (input[index] == '-') index++
            while (index < input.length && input[index].isDigit()) index++
            var isFloat = false
            if (index < input.length && input[index] == '.') {
                isFloat = true
                index++
                while (index < input.length && input[index].isDigit()) index++
            }
            if (index < input.length && (input[index] == 'e' || input[index] == 'E')) {
                isFloat = true
                index++
                if (index < input.length && (input[index] == '+' || input[index] == '-')) index++
                while (index < input.length && input[index].isDigit()) index++
            }
            val token = input.substring(start, index)
            return if (!isFloat) {
                token.toLongOrNull() ?: token.toDouble()
            } else {
                token.toDouble()
            }
        }

        private fun <T> expectLiteral(literal: String, value: T): T {
            if (!input.startsWith(literal, index)) error("Expected $literal")
            index += literal.length
            return value
        }

        private fun skipWhitespace() {
            while (index < input.length) {
                when (input[index]) {
                    ' ', '\n', '\r', '\t' -> index++
                    else -> return
                }
            }
        }
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
                    .let { if (it.startsWith("- ")) "• " + it.removePrefix("- ").trim() else it }
            }
            .filter {
                it.isNotBlank() &&
                    !it.equals("What's New", ignoreCase = true) &&
                    !it.startsWith("Nova Browser v", ignoreCase = true) &&
                    !it.startsWith("APK SHA-256", ignoreCase = true)
            }
            .take(8)
            .joinToString("\n")
            .ifBlank { "Performance improvements, Liquid Glass polish, and bug fixes." }
    }
}
