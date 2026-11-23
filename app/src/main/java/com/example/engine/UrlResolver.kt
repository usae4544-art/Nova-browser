package com.example.engine

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import com.example.data.preferences.SearchEngine
import java.net.URLEncoder

data class ChromiumEngineInfo(
    val engineName: String,
    val packageName: String,
    val versionName: String,
    val limitationsNote: String
)

object UrlResolver {
    const val NOVA_HOME_URL = "nova://home"

    private val domainPattern = Regex(
        "^(localhost(:\\d+)?|(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?|([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:\\d+)?)(/.*)?$"
    )

    fun resolveInput(
        rawInput: String,
        searchEngine: SearchEngine,
        httpsOnlyMode: Boolean = false
    ): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return NOVA_HOME_URL
        if (trimmed.equals(NOVA_HOME_URL, ignoreCase = true) || trimmed.equals("about:blank", ignoreCase = true)) {
            return NOVA_HOME_URL
        }

        // Explicit scheme
        if (trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            return if (httpsOnlyMode) {
                "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
            } else {
                trimmed
            }
        }
        if (trimmed.startsWith("about:", ignoreCase = true)) {
            return trimmed
        }

        // Domain or IP without spaces
        if (!trimmed.contains(' ') && domainPattern.matches(trimmed)) {
            val scheme = if (trimmed.startsWith("localhost") || trimmed.startsWith("127.") || trimmed.startsWith("192.168.")) {
                if (httpsOnlyMode) "https://" else "http://"
            } else {
                "https://"
            }
            return "$scheme$trimmed"
        }

        // Otherwise route to selected search engine
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        return searchEngine.searchUrlTemplate.replace("%s", encoded)
    }

    fun extractHost(url: String?): String {
        if (url.isNullOrBlank() || url == NOVA_HOME_URL) return ""
        return runCatching {
            val host = java.net.URI(url).host
            if (!host.isNullOrBlank()) host.lowercase()
            else url.substringAfter("://", "").substringBefore("/").substringBefore(":").lowercase()
        }.getOrDefault("")
    }

    fun displayHostOrTitle(url: String?, title: String?): String {
        if (url.isNullOrBlank() || url == NOVA_HOME_URL) return "NOVA Home"
        val host = extractHost(url).removePrefix("www.")
        return host.ifBlank { title?.ifBlank { url } ?: url }
    }

    fun buildTranslateUrl(sourceUrl: String, targetLang: String = "en"): String {
        val encoded = URLEncoder.encode(sourceUrl, "UTF-8")
        return "https://translate.google.com/translate?sl=auto&tl=$targetLang&u=$encoded"
    }

    fun detectChromiumWebViewInfo(context: Context): ChromiumEngineInfo {
        val pkg = runCatching { WebViewCompat.getCurrentWebViewPackage(context) }.getOrNull()
        return ChromiumEngineInfo(
            engineName = "Chromium-based Android System WebView",
            packageName = pkg?.packageName ?: "com.google.android.webview",
            versionName = pkg?.versionName ?: "System Managed",
            limitationsNote = "Uses Android's built-in Chromium WebView engine rather than a standalone bundled Chromium binary. " +
                "Supports modern HTML5, CSS3, ES2023+ JavaScript, WebGL, cookies, media playback, and Safe Browsing; " +
                "Chrome browser extensions and multi-profile sync are not exposed by Android WebView."
        )
    }

    fun extractUserAgentChromiumVersion(context: Context): String {
        val pkg = runCatching { WebViewCompat.getCurrentWebViewPackage(context) }.getOrNull()
        return pkg?.versionName ?: runCatching {
            android.webkit.WebSettings.getDefaultUserAgent(context)
                .substringAfter("Chrome/", "")
                .substringBefore(" ")
                .ifBlank { "System WebView" }
        }.getOrDefault("System WebView")
    }
}
