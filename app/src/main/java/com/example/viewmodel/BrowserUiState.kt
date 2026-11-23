package com.example.viewmodel

import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import com.example.engine.PageSecurityState
import com.example.engine.UrlResolver
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val parentTabId: String? = null,
    val title: String = "NOVA Home",
    val url: String = UrlResolver.NOVA_HOME_URL,
    val isIncognito: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val desktopMode: Boolean = false,
    val securityState: PageSecurityState = PageSecurityState.NOVA_HOME,
    val networkErrorMessage: String? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    val isHome: Boolean
        get() = url.isBlank() || url == UrlResolver.NOVA_HOME_URL
}

sealed interface BrowserOverlay {
    data object None : BrowserOverlay
    data object TabSwitcher : BrowserOverlay
    data object Menu : BrowserOverlay
    data object Bookmarks : BrowserOverlay
    data object History : BrowserOverlay
    data object Downloads : BrowserOverlay
    data object AdBlockDashboard : BrowserOverlay
    data object Settings : BrowserOverlay
    data object VpnSettings : BrowserOverlay
    data object QuickNotes : BrowserOverlay
    data object QrScanner : BrowserOverlay
    data object ReadingMode : BrowserOverlay
}

data class FindInPageState(
    val isActive: Boolean = false,
    val query: String = "",
    val activeMatchIndex: Int = 0,
    val totalMatches: Int = 0
)

data class SslWarningRequest(
    val tabId: String,
    val host: String,
    val primaryError: Int,
    val handler: SslErrorHandler
)

sealed interface SitePermissionPrompt {
    data class WebMedia(
        val origin: String,
        val resources: Array<String>,
        val request: PermissionRequest
    ) : SitePermissionPrompt

    data class Geolocation(
        val origin: String,
        val callback: GeolocationPermissions.Callback
    ) : SitePermissionPrompt
}

data class ReadingArticle(
    val isLoading: Boolean = false,
    val title: String = "",
    val byline: String = "",
    val paragraphs: List<String> = emptyList()
)

enum class IncognitoVaultPurpose {
    OPEN_INCOGNITO_TAB,
    UNLOCK_INCOGNITO_TABS
}

data class IncognitoPinChallenge(
    val purpose: IncognitoVaultPurpose,
    val errorMessage: String? = null
)
