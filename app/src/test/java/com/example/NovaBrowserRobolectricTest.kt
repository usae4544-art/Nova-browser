package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AdBlockRuleEntity
import com.example.engine.AdBlockEngine
import com.example.engine.UrlResolver
import com.example.viewmodel.BrowserOverlay
import com.example.viewmodel.BrowserViewModel
import com.example.vpn.VpnConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NovaBrowserRobolectricTest {

    @Test
    fun `app name matches Nova Browser`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Nova Browser", context.getString(R.string.app_name))
    }

    @Test
    fun `ad block engine blocks known ad and tracker hosts and respects allowlist and custom rules`() {
        val engine = AdBlockEngine()

        // Blocks built-in ad network
        val adReason = engine.checkShouldBlock(
            requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            pageUrl = "https://news.example.org/article"
        )
        assertNotNull(adReason)

        // Blocks built-in tracker
        val trackerReason = engine.checkShouldBlock(
            requestUrl = "https://www.google-analytics.com/g/collect",
            pageUrl = "https://news.example.org/article"
        )
        assertNotNull(trackerReason)
        assertEquals(2, engine.stats.value.totalBlocked)

        // Allows legitimate website asset
        val allowed = engine.checkShouldBlock(
            requestUrl = "https://news.example.org/images/hero.png",
            pageUrl = "https://news.example.org/article"
        )
        assertNull(allowed)

        // Respects website allowlist and custom block rules
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
    fun `viewModel supports multi-tab navigation, incognito isolation, overlays, and honest VPN default`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BrowserViewModel(app)

        // Starts with 1 NOVA Home tab & disconnected VPN
        assertEquals(1, vm.tabs.value.size)
        assertTrue(vm.activeTab.value.isHome)
        assertEquals(VpnConnectionStatus.DISCONNECTED, vm.vpnState.value.status)

        // Search navigation updates active tab immediately
        vm.navigateActiveTab("kotlin coroutines")
        assertTrue(vm.activeTab.value.url.contains("google.com/search?q=kotlin+coroutines"))
        assertFalse(vm.activeTab.value.isHome)

        // Open second normal tab and switch back & forth
        val firstTabId = vm.activeTab.value.id
        vm.openNewTab(isIncognito = false, initialUrl = "wikipedia.org")
        assertEquals(2, vm.tabs.value.size)
        assertEquals("https://wikipedia.org", vm.activeTab.value.url)
        val secondTabId = vm.activeTab.value.id

        // Open incognito tab
        vm.openNewTab(isIncognito = true)
        assertEquals(3, vm.tabs.value.size)
        assertTrue(vm.activeTab.value.isIncognito)
        assertEquals(UrlResolver.NOVA_HOME_URL, vm.activeTab.value.url)

        // Switch back to first tab and verify URL preserved
        vm.selectTab(firstTabId)
        assertEquals(firstTabId, vm.activeTab.value.id)
        assertTrue(vm.activeTab.value.url.contains("kotlin+coroutines"))

        // Close second tab
        vm.closeTab(secondTabId)
        assertEquals(2, vm.tabs.value.size)

        // Overlay state transitions
        vm.openOverlay(BrowserOverlay.Settings)
        assertEquals(BrowserOverlay.Settings, vm.activeOverlay.value)
        vm.closeOverlay()
        assertEquals(BrowserOverlay.None, vm.activeOverlay.value)
    }
}
