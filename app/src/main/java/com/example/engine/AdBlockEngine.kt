package com.example.engine

import com.example.data.local.AdBlockRuleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

data class BlockedRequestEntry(
    val host: String,
    val pageHost: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AdBlockStats(
    val totalBlocked: Int = 0,
    val adsBlocked: Int = 0,
    val trackersBlocked: Int = 0,
    val recentBlocked: List<BlockedRequestEntry> = emptyList()
)

class AdBlockEngine {

    private val adBlockEnabled = AtomicBoolean(true)
    private val trackerBlockEnabled = AtomicBoolean(true)

    private val allowlistedHosts = CopyOnWriteArraySet<String>()
    private val customDomainBlockRules = CopyOnWriteArraySet<String>()
    private val customPathBlockRules = CopyOnWriteArraySet<String>()

    private val _stats = MutableStateFlow(AdBlockStats())
    val stats: StateFlow<AdBlockStats> = _stats.asStateFlow()

    private val builtInAdDomains = BUILT_IN_AD_DOMAINS

    private val builtInTrackerDomains = BUILT_IN_TRACKER_DOMAINS

    private val adPathKeywords = listOf(
        "/pagead/",
        "/adserver/",
        "/ads/banner",
        "/sponsored_ads/",
        "/popunder",
        "doubleclick.net/gampad"
    )

    // Never block video streaming CDNs or core media segment requests so HTML5/OTT videos play cleanly
    private val protectedVideoHosts = setOf(
        "googlevideo.com",
        "ytimg.com",
        "youtube.com",
        "youtu.be",
        "youtube-nocookie.com",
        "vimeo.com",
        "vimeocdn.com",
        "dailymotion.com",
        "dmcdn.net",
        "twitch.tv",
        "ttvnw.net",
        "jwpcdn.com",
        "jwplayer.com",
        "cloudflarestream.com",
        "mux.com",
        "brightcove.com",
        "bitmovin.com"
    )

    private val protectedMediaExtensions = listOf(
        ".mp4", ".m3u8", ".mpd", ".webm", ".m4s", ".m4v", ".m4a", ".mp3", ".ogg", ".ts"
    )

    fun updateConfig(
        adBlockOn: Boolean,
        trackerBlockOn: Boolean,
        rules: List<AdBlockRuleEntity>
    ) {
        adBlockEnabled.set(adBlockOn)
        trackerBlockEnabled.set(trackerBlockOn)

        allowlistedHosts.clear()
        customDomainBlockRules.clear()
        customPathBlockRules.clear()
        for (rule in rules) {
            if (!rule.enabled) continue
            val normalized = rule.pattern.trim().lowercase()
            if (normalized.isEmpty()) continue
            if (rule.isAllowlist) {
                allowlistedHosts.add(normalized.removePrefix("www."))
            } else if (normalized.contains("/")) {
                customPathBlockRules.add(normalized)
            } else {
                customDomainBlockRules.add(normalized.removePrefix("www."))
            }
        }
    }

    fun isHostAllowlisted(pageHost: String): Boolean {
        val clean = pageHost.trim().lowercase().removePrefix("www.")
        if (clean.isEmpty()) return false
        return matchesDomainSet(clean, allowlistedHosts)
    }

    /**
     * Thread-safe, fast check invoked on WebView IO threads during shouldInterceptRequest.
     * Returns a non-null block reason if the request should be blocked, or null to allow.
     */
    fun checkShouldBlock(requestUrl: String, pageUrl: String?): String? {
        if (!adBlockEnabled.get() && !trackerBlockEnabled.get()) return null
        if (requestUrl.isBlank()) return null

        val reqHost = UrlResolver.extractHost(requestUrl).removePrefix("www.")
        if (reqHost.isBlank()) return null
        val pageHost = UrlResolver.extractHost(pageUrl).removePrefix("www.")

        // Respect website allowlist
        if (pageHost.isNotEmpty() && isHostAllowlisted(pageHost)) {
            return null
        }

        val lowerUrl = requestUrl.lowercase()
        val cleanPath = lowerUrl.substringBefore("?").substringBefore("#")
        val pathOnly = "/" + cleanPath.substringAfter("://", "").substringAfter("/", "")

        // Custom block rules respect the master AdBlock toggle and distinguish domain vs path patterns
        if (adBlockEnabled.get()) {
            if (matchesDomainSet(reqHost, customDomainBlockRules)) {
                recordBlock(reqHost, pageHost, "Custom rule", isTracker = false)
                return "Custom rule"
            }
            for (pathPattern in customPathBlockRules) {
                if (pathOnly.contains(pathPattern)) {
                    recordBlock(reqHost, pageHost, "Custom rule: $pathPattern", isTracker = false)
                    return "Custom rule"
                }
            }
        }

        // Always allow video CDN segments & media files so HTML5/YouTube/streaming videos never stall
        if (matchesDomainSet(reqHost, protectedVideoHosts) ||
            protectedMediaExtensions.any { cleanPath.endsWith(it) }
        ) {
            return null
        }

        if (adBlockEnabled.get()) {
            if (matchesDomainSet(reqHost, builtInAdDomains)) {
                recordBlock(reqHost, pageHost, "Ad network", isTracker = false)
                return "Ad network"
            }
            if (adPathKeywords.any { lowerUrl.contains(it) }) {
                recordBlock(reqHost, pageHost, "Ad script", isTracker = false)
                return "Ad script"
            }
        }

        if (trackerBlockEnabled.get()) {
            if (matchesDomainSet(reqHost, builtInTrackerDomains)) {
                recordBlock(reqHost, pageHost, "Tracker", isTracker = true)
                return "Tracker"
            }
        }

        return null
    }

    fun resetStats() {
        _stats.value = AdBlockStats()
    }

    private fun recordBlock(host: String, pageHost: String, reason: String, isTracker: Boolean) {
        _stats.update { current ->
            val updatedList = (listOf(
                BlockedRequestEntry(
                    host = host,
                    pageHost = pageHost.ifBlank { "Web" },
                    reason = reason
                )
            ) + current.recentBlocked).take(40)
            AdBlockStats(
                totalBlocked = current.totalBlocked + 1,
                adsBlocked = current.adsBlocked + if (!isTracker) 1 else 0,
                trackersBlocked = current.trackersBlocked + if (isTracker) 1 else 0,
                recentBlocked = updatedList
            )
        }
    }

    private fun matchesDomainSet(host: String, domains: Set<String>): Boolean =
        hostMatchesDomainSet(host, domains)

    companion object {
        val BUILT_IN_AD_DOMAINS: Set<String> = setOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com",
            "tpc.googlesyndication.com",
            "amazon-adsystem.com",
            "adnxs.com",
            "adsrvr.org",
            "advertising.com",
            "adroll.com",
            "adcolony.com",
            "applovin.com",
            "criteo.com",
            "criteo.net",
            "outbrain.com",
            "taboola.com",
            "pubmatic.com",
            "rubiconproject.com",
            "openx.net",
            "casalemedia.com",
            "indexww.com",
            "media.net",
            "moatads.com",
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "revcontent.com",
            "mgid.com",
            "zedo.com",
            "smartadserver.com",
            "bidswitch.net",
            "sharethrough.com",
            "spotxchange.com",
            "teads.tv",
            "yieldmo.com",
            "unityads.unity3d.com",
            "vungle.com",
            "inmobi.com",
            "chartboost.com",
            "flurry.com",
            "ads.yahoo.com",
            "ads.twitter.com",
            "ads.linkedin.com",
            "ads.tiktok.com"
        )

        val BUILT_IN_TRACKER_DOMAINS: Set<String> = setOf(
            "google-analytics.com",
            "googletagmanager.com",
            "analytics.google.com",
            "connect.facebook.net",
            "pixel.facebook.com",
            "scorecardresearch.com",
            "quantserve.com",
            "hotjar.com",
            "hotjar.io",
            "mixpanel.com",
            "segment.io",
            "segment.com",
            "amplitude.com",
            "clarity.ms",
            "fullstory.com",
            "mouseflow.com",
            "crazyegg.com",
            "newrelic.com",
            "nr-data.net",
            "branch.io",
            "appsflyer.com",
            "adjust.com",
            "kochava.com",
            "omtrdc.net",
            "demdex.net",
            "krxd.net",
            "bluekai.com",
            "exelator.com",
            "rlcdn.com",
            "tapad.com",
            "chartbeat.com",
            "chartbeat.net",
            "parsely.com",
            "kissmetrics.com",
            "optimizely.com",
            "bugsnag.com",
            "doubleverify.com",
            "iasds01.com",
            "adsafeprotected.com"
        )

        fun hostMatchesDomainSet(host: String, domains: Set<String>): Boolean {
            if (domains.isEmpty() || host.isBlank()) return false
            var candidate = host.trim().lowercase().removePrefix("www.")
            while (candidate.isNotEmpty()) {
                if (domains.contains(candidate)) return true
                val dotIndex = candidate.indexOf('.')
                if (dotIndex < 0) return false
                candidate = candidate.substring(dotIndex + 1)
            }
            return false
        }

        fun isBuiltInAdOrTrackerHost(host: String): Boolean {
            return hostMatchesDomainSet(host, BUILT_IN_AD_DOMAINS) ||
                hostMatchesDomainSet(host, BUILT_IN_TRACKER_DOMAINS)
        }
    }
}
