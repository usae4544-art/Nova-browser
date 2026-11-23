package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_browser_prefs")

enum class SearchEngine(
    val displayName: String,
    val searchUrlTemplate: String,
    val homeUrl: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=%s", "https://www.google.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com"),
    BING("Bing", "https://www.bing.com/search?q=%s", "https://www.bing.com"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s", "https://search.brave.com"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=%s", "https://search.yahoo.com")
}

data class BrowserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val customHomepageUrl: String = "",
    val adBlockEnabled: Boolean = true,
    val trackerBlockEnabled: Boolean = true,
    val httpsOnlyMode: Boolean = false,
    val cookiesEnabled: Boolean = true,
    val thirdPartyCookiesEnabled: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val popupBlockingEnabled: Boolean = true,
    val autoDarkWebsites: Boolean = true,
    val showTopBar: Boolean = true,
    val showBottomBar: Boolean = true,
    val textZoomPercent: Int = 100,
    val allowCameraSites: Boolean = true,
    val allowMicSites: Boolean = true,
    val allowLocationSites: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val incognitoLockEnabled: Boolean = false,
    val incognitoPinHash: String = "",
    val incognitoPinSalt: String = "",
    val vpnDnsServer: String = "1.1.1.1",
    val vpnGatewayAddress: String = ""
)

class BrowserPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val CUSTOM_HOMEPAGE = stringPreferencesKey("custom_homepage")
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val TRACKER_BLOCK_ENABLED = booleanPreferencesKey("tracker_block_enabled")
        val HTTPS_ONLY_MODE = booleanPreferencesKey("https_only_mode")
        val COOKIES_ENABLED = booleanPreferencesKey("cookies_enabled")
        val THIRD_PARTY_COOKIES = booleanPreferencesKey("third_party_cookies")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val POPUP_BLOCKING = booleanPreferencesKey("popup_blocking")
        val AUTO_DARK_WEBSITES = booleanPreferencesKey("auto_dark_websites")
        val SHOW_TOP_BAR = booleanPreferencesKey("show_top_bar")
        val SHOW_BOTTOM_BAR = booleanPreferencesKey("show_bottom_bar")
        val TEXT_ZOOM = intPreferencesKey("text_zoom")
        val ALLOW_CAMERA = booleanPreferencesKey("allow_camera")
        val ALLOW_MIC = booleanPreferencesKey("allow_mic")
        val ALLOW_LOCATION = booleanPreferencesKey("allow_location")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val INCOGNITO_LOCK = booleanPreferencesKey("incognito_lock_enabled")
        val INCOGNITO_PIN_HASH = stringPreferencesKey("incognito_pin_hash")
        val INCOGNITO_PIN_SALT = stringPreferencesKey("incognito_pin_salt")
        val VPN_DNS_SERVER = stringPreferencesKey("vpn_dns_server")
        val VPN_GATEWAY = stringPreferencesKey("vpn_gateway")
    }

    val settingsFlow: Flow<BrowserSettings> = context.novaDataStore.data.map { prefs ->
        BrowserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { AppThemeMode.valueOf(it) }.getOrNull()
            } ?: AppThemeMode.SYSTEM,
            searchEngine = prefs[Keys.SEARCH_ENGINE]?.let {
                runCatching { SearchEngine.valueOf(it) }.getOrNull()
            } ?: SearchEngine.GOOGLE,
            customHomepageUrl = prefs[Keys.CUSTOM_HOMEPAGE] ?: "",
            adBlockEnabled = prefs[Keys.AD_BLOCK_ENABLED] ?: true,
            trackerBlockEnabled = prefs[Keys.TRACKER_BLOCK_ENABLED] ?: true,
            httpsOnlyMode = prefs[Keys.HTTPS_ONLY_MODE] ?: false,
            cookiesEnabled = prefs[Keys.COOKIES_ENABLED] ?: true,
            thirdPartyCookiesEnabled = prefs[Keys.THIRD_PARTY_COOKIES] ?: false,
            javascriptEnabled = prefs[Keys.JAVASCRIPT_ENABLED] ?: true,
            popupBlockingEnabled = prefs[Keys.POPUP_BLOCKING] ?: true,
            autoDarkWebsites = prefs[Keys.AUTO_DARK_WEBSITES] ?: true,
            showTopBar = prefs[Keys.SHOW_TOP_BAR] ?: true,
            showBottomBar = prefs[Keys.SHOW_BOTTOM_BAR] ?: true,
            textZoomPercent = (prefs[Keys.TEXT_ZOOM] ?: 100).coerceIn(75, 200),
            allowCameraSites = prefs[Keys.ALLOW_CAMERA] ?: true,
            allowMicSites = prefs[Keys.ALLOW_MIC] ?: true,
            allowLocationSites = prefs[Keys.ALLOW_LOCATION] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: false,
            incognitoLockEnabled = prefs[Keys.INCOGNITO_LOCK] ?: false,
            incognitoPinHash = prefs[Keys.INCOGNITO_PIN_HASH] ?: "",
            incognitoPinSalt = prefs[Keys.INCOGNITO_PIN_SALT] ?: "",
            vpnDnsServer = prefs[Keys.VPN_DNS_SERVER] ?: "1.1.1.1",
            vpnGatewayAddress = prefs[Keys.VPN_GATEWAY] ?: ""
        )
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.novaDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateSearchEngine(engine: SearchEngine) {
        context.novaDataStore.edit { it[Keys.SEARCH_ENGINE] = engine.name }
    }

    suspend fun updateCustomHomepage(url: String) {
        context.novaDataStore.edit { it[Keys.CUSTOM_HOMEPAGE] = url.trim() }
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.AD_BLOCK_ENABLED] = enabled }
    }

    suspend fun setTrackerBlockEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.TRACKER_BLOCK_ENABLED] = enabled }
    }

    suspend fun setHttpsOnlyMode(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.HTTPS_ONLY_MODE] = enabled }
    }

    suspend fun setCookiesEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.COOKIES_ENABLED] = enabled }
    }

    suspend fun setThirdPartyCookiesEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.THIRD_PARTY_COOKIES] = enabled }
    }

    suspend fun setJavaScriptEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun setPopupBlockingEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.POPUP_BLOCKING] = enabled }
    }

    suspend fun setAutoDarkWebsites(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.AUTO_DARK_WEBSITES] = enabled }
    }

    suspend fun setShowTopBar(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.SHOW_TOP_BAR] = enabled }
    }

    suspend fun setShowBottomBar(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.SHOW_BOTTOM_BAR] = enabled }
    }

    suspend fun setTextZoomPercent(zoom: Int) {
        context.novaDataStore.edit { it[Keys.TEXT_ZOOM] = zoom.coerceIn(75, 200) }
    }

    suspend fun setAllowCameraSites(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.ALLOW_CAMERA] = enabled }
    }

    suspend fun setAllowMicSites(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.ALLOW_MIC] = enabled }
    }

    suspend fun setAllowLocationSites(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.ALLOW_LOCATION] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setIncognitoPin(hash: String, salt: String) {
        context.novaDataStore.edit {
            it[Keys.INCOGNITO_PIN_HASH] = hash
            it[Keys.INCOGNITO_PIN_SALT] = salt
            it[Keys.INCOGNITO_LOCK] = true
        }
    }

    suspend fun clearIncognitoPin() {
        context.novaDataStore.edit {
            it[Keys.INCOGNITO_PIN_HASH] = ""
            it[Keys.INCOGNITO_PIN_SALT] = ""
            it[Keys.INCOGNITO_LOCK] = false
        }
    }

    suspend fun setVpnDnsServer(dns: String) {
        context.novaDataStore.edit { it[Keys.VPN_DNS_SERVER] = dns.trim() }
    }

    suspend fun setVpnGatewayAddress(address: String) {
        context.novaDataStore.edit { it[Keys.VPN_GATEWAY] = address.trim() }
    }
}
