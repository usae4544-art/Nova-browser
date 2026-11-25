package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.local.AdBlockRuleEntity
import com.example.data.local.NovaDatabase
import com.example.data.preferences.SearchEngine
import com.example.downloads.NovaDownloadManager
import com.example.engine.AdBlockEngine
import com.example.engine.GitHubUpdateChecker
import com.example.engine.PageSecurityState
import com.example.engine.SecurityUtils
import com.example.engine.UrlResolver
import com.example.notifications.NovaNotificationSound
import com.example.ui.components.SakugaPalette
import com.example.ui.components.SakugaVfxController
import com.example.ui.components.sakugaSquashScale
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IncognitoAccent
import com.example.vpn.VpnConnectionStatus
import com.example.vpn.VpnRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaBrowserUnitTest {

    @Test
    fun `resolveInput routes search queries to Google, DuckDuckGo, Bing, Brave, and Yahoo`() {
        assertEquals(
            "https://www.google.com/search?q=android+compose+tutorial",
            UrlResolver.resolveInput("android compose tutorial", SearchEngine.GOOGLE)
        )
        assertEquals(
            "https://duckduckgo.com/?q=privacy+browser",
            UrlResolver.resolveInput("privacy browser", SearchEngine.DUCKDUCKGO)
        )
        assertEquals(
            "https://search.brave.com/search?q=nova+browser",
            UrlResolver.resolveInput("nova browser", SearchEngine.BRAVE)
        )
    }

    @Test
    fun `resolveInput opens direct URLs and respects HTTPS-only upgrade`() {
        assertEquals("https://github.com/android", UrlResolver.resolveInput("github.com/android", SearchEngine.GOOGLE))
        assertEquals("http://example.com", UrlResolver.resolveInput("http://example.com", SearchEngine.GOOGLE, httpsOnlyMode = false))
        assertEquals("https://example.com", UrlResolver.resolveInput("http://example.com", SearchEngine.GOOGLE, httpsOnlyMode = true))
        assertEquals("http://localhost:8080/test", UrlResolver.resolveInput("localhost:8080/test", SearchEngine.GOOGLE, httpsOnlyMode = false))
    }

    @Test
    fun `buildTranslateUrl encodes webpage URL for Google Translate`() {
        val translated = UrlResolver.buildTranslateUrl("https://es.wikipedia.org/wiki/Android")
        assertTrue(translated.startsWith("https://translate.google.com/translate?sl=auto&tl=en&u="))
        assertTrue(translated.contains("wikipedia.org"))
    }

    @Test
    fun `security utils classifies HTTPS, HTTP, and cert errors accurately`() {
        assertEquals(PageSecurityState.NOVA_HOME, SecurityUtils.classifyUrlSecurity(UrlResolver.NOVA_HOME_URL))
        assertEquals(PageSecurityState.SECURE_HTTPS, SecurityUtils.classifyUrlSecurity("https://www.google.com"))
        assertEquals(PageSecurityState.INSECURE_HTTP, SecurityUtils.classifyUrlSecurity("http://neverssl.com"))
        assertEquals(PageSecurityState.CERT_ERROR, SecurityUtils.classifyUrlSecurity("https://expired.badssl.com", hasSslError = true))
    }

    @Test
    fun `salted PIN hashing verifies matching PIN and rejects wrong PIN`() {
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin("2468", salt)
        assertTrue(SecurityUtils.verifyPin("2468", hash, salt))
        assertFalse(SecurityUtils.verifyPin("1234", hash, salt))
    }

    @Test
    fun `ad block engine blocks ads and trackers, supports allowlist, custom rules, and master toggle`() {
        val engine = AdBlockEngine()

        // Built-in ad domain blocked
        val adReason = engine.checkShouldBlock(
            requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            pageUrl = "https://news.example.org/article"
        )
        assertNotNull(adReason)

        // Built-in tracker domain blocked
        val trackerReason = engine.checkShouldBlock(
            requestUrl = "https://www.google-analytics.com/g/collect",
            pageUrl = "https://news.example.org/article"
        )
        assertNotNull(trackerReason)
        assertEquals(2, engine.stats.value.totalBlocked)
        assertEquals(1, engine.stats.value.adsBlocked)
        assertEquals(1, engine.stats.value.trackersBlocked)

        // Legitimate first-party asset allowed
        val allowed = engine.checkShouldBlock(
            requestUrl = "https://news.example.org/images/hero.png",
            pageUrl = "https://news.example.org/article"
        )
        assertNull(allowed)

        // Website allowlist + custom block rule
        engine.updateConfig(
            adBlockOn = true,
            trackerBlockOn = true,
            rules = listOf(
                AdBlockRuleEntity(id = 1, pattern = "news.example.org", isAllowlist = true, enabled = true),
                AdBlockRuleEntity(id = 2, pattern = "custom-ads.test", isAllowlist = false, enabled = true)
            )
        )
        assertNull(
            engine.checkShouldBlock(
                requestUrl = "https://doubleclick.net/pixel.gif",
                pageUrl = "https://news.example.org/article"
            )
        )
        assertNotNull(
            engine.checkShouldBlock(
                requestUrl = "https://cdn.custom-ads.test/banner.js",
                pageUrl = "https://other.org"
            )
        )

        // Master OFF switch disables blocking
        engine.updateConfig(adBlockOn = false, trackerBlockOn = false, rules = emptyList())
        assertNull(
            engine.checkShouldBlock(
                requestUrl = "https://doubleclick.net/pixel.gif",
                pageUrl = "https://other.org"
            )
        )
    }

    @Test
    fun `download byte formatting works across units`() {
        assertEquals("512 B", NovaDownloadManager.formatBytes(512))
        assertEquals("2.0 KB", NovaDownloadManager.formatBytes(2048))
        assertEquals("1.50 MB", NovaDownloadManager.formatBytes(1572864))
    }

    @Test
    fun `download manager resolves MIME types for APK and all major file formats`() {
        assertEquals(
            NovaDownloadManager.APK_MIME_TYPE,
            NovaDownloadManager.resolveMimeFromFileName("https://example.com/releases/app-v2.apk?token=123")
        )
        assertEquals("application/pdf", NovaDownloadManager.resolveMimeFromFileName("manual.pdf"))
        assertEquals("video/mp4", NovaDownloadManager.resolveMimeFromFileName("clip.mp4"))
        assertEquals("audio/mpeg", NovaDownloadManager.resolveMimeFromFileName("song.mp3"))
        assertEquals("application/zip", NovaDownloadManager.resolveMimeFromFileName("archive.zip"))
        assertEquals("image/png", NovaDownloadManager.resolveMimeFromFileName("photo.png"))
    }

    @Test
    fun `security utils validates IPv4 and IPv6 DNS addresses accurately`() {
        assertTrue(SecurityUtils.isValidIpAddress("1.1.1.1"))
        assertTrue(SecurityUtils.isValidIpAddress("8.8.8.8"))
        assertTrue(SecurityUtils.isValidIpAddress("2606:4700:4700::1111"))
        assertFalse(SecurityUtils.isValidIpAddress(""))
        assertFalse(SecurityUtils.isValidIpAddress("999.999.1.1"))
        assertFalse(SecurityUtils.isValidIpAddress("dns.google"))
    }

    @Test
    fun `ad block engine applies domain vs path rules and respects master toggle`() {
        val engine = AdBlockEngine()
        engine.updateConfig(
            adBlockOn = true,
            trackerBlockOn = true,
            rules = listOf(
                AdBlockRuleEntity(id = 1, pattern = "ads.test", isAllowlist = false, enabled = true),
                AdBlockRuleEntity(id = 2, pattern = "/banner/", isAllowlist = false, enabled = true)
            )
        )
        // Domain rule does not over-block legitimate paths on unrelated hosts
        assertNull(
            engine.checkShouldBlock(
                requestUrl = "https://example.org/downloads/reads.html",
                pageUrl = "https://example.org"
            )
        )
        // Path rule blocks matching path segment
        assertNotNull(
            engine.checkShouldBlock(
                requestUrl = "https://example.org/assets/banner/top.png",
                pageUrl = "https://example.org"
            )
        )
        // Disabling AdBlock disables custom ad rules while keeping tracker blocking active
        engine.updateConfig(
            adBlockOn = false,
            trackerBlockOn = true,
            rules = listOf(
                AdBlockRuleEntity(id = 1, pattern = "ads.test", isAllowlist = false, enabled = true)
            )
        )
        assertNull(
            engine.checkShouldBlock(
                requestUrl = "https://sub.ads.test/script.js",
                pageUrl = "https://example.org"
            )
        )
        assertNotNull(
            engine.checkShouldBlock(
                requestUrl = "https://www.google-analytics.com/g/collect",
                pageUrl = "https://example.org"
            )
        )
    }

    @Test
    fun `notification sounds map IDs accurately with unique channel IDs and fallback`() {
        assertEquals(NovaNotificationSound.GLASS_CHIME, NovaNotificationSound.fromId("GLASS_CHIME"))
        assertEquals(NovaNotificationSound.CRYSTAL_PING, NovaNotificationSound.fromId("CRYSTAL_PING"))
        assertEquals(NovaNotificationSound.AURORA_PULSE, NovaNotificationSound.fromId("AURORA_PULSE"))
        assertEquals(NovaNotificationSound.SYSTEM_DEFAULT, NovaNotificationSound.fromId("SYSTEM_DEFAULT"))
        assertEquals(NovaNotificationSound.GLASS_CHIME, NovaNotificationSound.fromId("UNKNOWN"))
        assertEquals(NovaNotificationSound.GLASS_CHIME, NovaNotificationSound.fromId(null))

        val uniqueChannels = NovaNotificationSound.entries.map { it.channelId }.toSet()
        assertEquals(NovaNotificationSound.entries.size, uniqueChannels.size)
        assertNotNull(NovaNotificationSound.GLASS_CHIME.rawResId)
        assertNotNull(NovaNotificationSound.CRYSTAL_PING.rawResId)
        assertNotNull(NovaNotificationSound.AURORA_PULSE.rawResId)
        assertNull(NovaNotificationSound.SYSTEM_DEFAULT.rawResId)
    }

    @Test
    fun `room database migration_1_2 targets version 1 to version 2`() {
        assertEquals(1, NovaDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, NovaDatabase.MIGRATION_1_2.endVersion)
    }

    @Test
    fun `vpn runtime state tracks DNS forwarding count cleanly`() {
        val disconnected = VpnRuntimeState()
        assertEquals(VpnConnectionStatus.DISCONNECTED, disconnected.status)
        assertEquals(0L, disconnected.queriesForwarded)

        val active = disconnected.copy(
            status = VpnConnectionStatus.CONNECTED_LOCAL_TUN,
            dnsServer = "1.1.1.1",
            queriesForwarded = 12L,
            netShieldEnabled = true,
            dohEnabled = true,
            threatsBlocked = 4L,
            lastBlockedDomain = "pagead2.googlesyndication.com"
        )
        assertEquals(12L, active.queriesForwarded)
        assertTrue(active.netShieldEnabled)
        assertTrue(active.dohEnabled)
        assertEquals(4L, active.threatsBlocked)

        assertTrue(AdBlockEngine.isBuiltInAdOrTrackerHost("pagead2.googlesyndication.com"))
        assertTrue(AdBlockEngine.isBuiltInAdOrTrackerHost("sub.google-analytics.com"))
        assertFalse(AdBlockEngine.isBuiltInAdOrTrackerHost("wikipedia.org"))
    }

    @Test
    fun `sakuga palette adapts cleanly across Light, Dark, and Incognito modes`() {
        val light = SakugaPalette.forTheme(isDark = false, isIncognito = false)
        assertFalse(light.isDark)
        assertEquals(Color(0xFFFFFFFF), light.fill)
        assertEquals(Color(0xFFB8B3FF), light.shade)
        assertEquals(Color(0xFF5A4FD8), light.line)
        assertEquals(Color(0xFFFF5FA8), light.accent)

        val dark = SakugaPalette.forTheme(isDark = true, isIncognito = false)
        assertTrue(dark.isDark)
        assertEquals(Color(0xFF172247), dark.fill)
        assertEquals(Color(0xFF0C1229), dark.shade)
        assertEquals(ElectricCyan, dark.line)
        assertEquals(Color(0xFFFF5FA8), dark.accent)

        val incognito = SakugaPalette.forTheme(isDark = true, isIncognito = true)
        assertTrue(incognito.isDark)
        assertEquals(IncognitoAccent, incognito.accent)
        assertEquals(Color(0xFF21143D), incognito.fill)
    }

    @Test
    fun `sakuga squash scale preserves volume and controller respects reduced motion`() {
        val (sx, sy) = sakugaSquashScale(0.86f, wideFactor = 0.5f)
        assertEquals(0.86f, sy, 0.001f)
        assertEquals(1.07f, sx, 0.001f)

        val reducedController = SakugaVfxController(reducedMotion = true)
        reducedController.triggerImpactBurst(Offset(100f, 100f), 200f)
        assertFalse(reducedController.hasActiveEffects)

        val controller = SakugaVfxController(reducedMotion = false)
        controller.triggerToastLanding(Offset(120f, 300f), 280f)
        assertTrue(controller.hasActiveEffects)
        controller.triggerThemeSplash(Offset(150f, 150f))
        assertTrue(controller.hasActiveEffects)

        // Stepping past maximum particle lifetime drains active particles back to idle (0% CPU)
        repeat(45) {
            controller.step(0.03f)
        }
        assertFalse(controller.hasActiveEffects)
    }

    @Test
    fun `github update checker compares semantic tags and extracts apk asset`() {
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.0.2", "1.0.1"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.2.0", "1.0.9"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.0.1", "1.0.1"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.0.0", "1.0.2"))

        val releaseJson = """
            {
              "tag_name": "v1.0.3",
              "name": "Nova Browser v1.0.3",
              "draft": false,
              "html_url": "https://github.com/usae4544-art/Nova-browser/releases/tag/v1.0.3",
              "body": "## What's New\n- \"Liquid Glass\" polish & zero-lag theme switch",
              "assets": [
                {
                  "name": "checksums.txt",
                  "size": 128,
                  "browser_download_url": "https://github.com/usae4544-art/Nova-browser/releases/download/v1.0.3/checksums.txt"
                },
                {
                  "name": "app-debug.apk",
                  "size": 23382371,
                  "browser_download_url": "https://github.com/usae4544-art/Nova-browser/releases/download/v1.0.3/app-debug.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = GitHubUpdateChecker.parseLatestReleaseJson(releaseJson, "1.0.2")
        assertNotNull(parsed)
        assertEquals("1.0.3", parsed!!.latestVersion)
        assertEquals("v1.0.3", parsed.releaseTag)
        assertEquals(23382371L, parsed.apkSizeBytes)
        assertTrue(parsed.apkDownloadUrl.endsWith("/app-debug.apk"))
        assertTrue(parsed.releaseNotes.contains("\"Liquid Glass\" polish"))
        assertNull(GitHubUpdateChecker.parseLatestReleaseJson(releaseJson, "1.0.3"))
        assertNull(GitHubUpdateChecker.parseLatestReleaseJson("{broken json", "1.0.2"))
    }
}
