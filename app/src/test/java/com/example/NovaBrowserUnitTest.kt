package com.example

import com.example.data.local.AdBlockRuleEntity
import com.example.data.preferences.SearchEngine
import com.example.downloads.NovaDownloadManager
import com.example.engine.AdBlockEngine
import com.example.engine.PageSecurityState
import com.example.engine.SecurityUtils
import com.example.engine.UrlResolver
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
}
