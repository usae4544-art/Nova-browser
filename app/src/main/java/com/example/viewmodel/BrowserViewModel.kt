package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AdBlockRuleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.NovaDatabase
import com.example.data.local.QuickNoteEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.ShortcutEntity
import com.example.data.local.SiteNotificationRuleEntity
import com.example.data.local.UserAccountEntity
import com.example.data.preferences.BrowserPreferences
import com.example.data.preferences.BrowserSettings
import com.example.data.preferences.SearchEngine
import com.example.data.repository.BrowserRepository
import com.example.downloads.NovaDownloadManager
import com.example.downloads.OpenFileOutcome
import com.example.data.local.DownloadStatus
import com.example.engine.AdBlockEngine
import com.example.engine.AdBlockStats
import com.example.engine.AppUpdateInfo
import com.example.engine.BiometricHelper
import com.example.engine.ChromiumEngineInfo
import com.example.engine.GitHubUpdateChecker
import com.example.engine.PageSecurityState
import com.example.engine.SecurityUtils
import com.example.engine.SiteCertificateSummary
import com.example.engine.TabWebEventCallbacks
import com.example.engine.TabWebViewManager
import com.example.engine.UrlResolver
import com.example.engine.WebAppInstaller
import com.example.notifications.NovaNotificationHelper
import com.example.notifications.NovaNotificationSound
import com.example.ui.theme.AppThemeMode
import com.example.vpn.NovaVpnService
import com.example.vpn.VpnConnectionStatus
import com.example.vpn.VpnRuntimeState
import com.example.vpn.VpnStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NovaDatabase.getInstance(application)
    private val preferences = BrowserPreferences(application)
    val repository = BrowserRepository(database, preferences)
    val adBlockEngine = AdBlockEngine()
    val downloadManager = NovaDownloadManager(application, repository)
    val webViewManager = TabWebViewManager(application, adBlockEngine)
    val chromiumInfo: ChromiumEngineInfo = UrlResolver.detectChromiumWebViewInfo(application)

    val settings: StateFlow<BrowserSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = BrowserSettings()
    )

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val history: StateFlow<List<HistoryEntity>> = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloads: StateFlow<List<DownloadEntity>> = repository.downloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val quickNotes: StateFlow<List<QuickNoteEntity>> = repository.quickNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val adBlockRules: StateFlow<List<AdBlockRuleEntity>> = repository.adBlockRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val shortcuts: StateFlow<List<ShortcutEntity>> = repository.shortcuts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NovaDatabase.defaultShortcuts
    )

    val siteNotificationRules: StateFlow<List<SiteNotificationRuleEntity>> =
        repository.siteNotificationRules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val reviews: StateFlow<List<ReviewEntity>> = repository.reviews.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NovaDatabase.defaultReviews
    )

    val ownReview: StateFlow<ReviewEntity?> = repository.ownReview.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val userAccount: StateFlow<UserAccountEntity?> = repository.activeAccount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val adBlockStats: StateFlow<AdBlockStats> = adBlockEngine.stats
    val vpnState: StateFlow<VpnRuntimeState> = VpnStateHolder.state
    private val _vpnPublicIp = MutableStateFlow(VpnPublicIpState())
    val vpnPublicIp: StateFlow<VpnPublicIpState> = _vpnPublicIp.asStateFlow()
    private val vpnIpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }
    val completedDownloads: SharedFlow<DownloadEntity> = downloadManager.completedDownloads

    private val _availableAppUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableAppUpdate: StateFlow<AppUpdateInfo?> = _availableAppUpdate.asStateFlow()

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    private var autoInstallUpdateUrl: String? = null
    private var dismissedUpdateTagThisSession: String? = null

    private val initialTab = BrowserTab()
    private val _tabs = MutableStateFlow(listOf(initialTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _tabVisitHistory = MutableStateFlow<List<String>>(emptyList())
    val hasPreviousTabInHistory: StateFlow<Boolean> = combine(_tabVisitHistory, _tabs, _activeTabId) { stack, currentTabs, currentActiveId ->
        val validIds = currentTabs.map { it.id }.toSet()
        stack.any { it in validIds && it != currentActiveId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val activeTab: StateFlow<BrowserTab> = combine(_tabs, _activeTabId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull() ?: BrowserTab()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initialTab
    )

    private val _activeOverlay = MutableStateFlow<BrowserOverlay>(BrowserOverlay.None)
    val activeOverlay: StateFlow<BrowserOverlay> = _activeOverlay.asStateFlow()

    private val _isFullscreenMode = MutableStateFlow(false)
    val isFullscreenMode: StateFlow<Boolean> = _isFullscreenMode.asStateFlow()

    private val _findInPage = MutableStateFlow(FindInPageState())
    val findInPage: StateFlow<FindInPageState> = _findInPage.asStateFlow()

    private val _sslWarning = MutableStateFlow<SslWarningRequest?>(null)
    val sslWarning: StateFlow<SslWarningRequest?> = _sslWarning.asStateFlow()

    private val _sitePermissionPrompt = MutableStateFlow<SitePermissionPrompt?>(null)
    val sitePermissionPrompt: StateFlow<SitePermissionPrompt?> = _sitePermissionPrompt.asStateFlow()

    private val _readingArticle = MutableStateFlow(ReadingArticle())
    val readingArticle: StateFlow<ReadingArticle> = _readingArticle.asStateFlow()

    private val _incognitoUnlocked = MutableStateFlow(false)
    val incognitoUnlocked: StateFlow<Boolean> = _incognitoUnlocked.asStateFlow()

    private val _isIncognitoVaultLocked = MutableStateFlow(false)
    val isIncognitoVaultLocked: StateFlow<Boolean> = _isIncognitoVaultLocked.asStateFlow()

    private val _biometricPromptNonce = MutableStateFlow(0)
    val biometricPromptNonce: StateFlow<Int> = _biometricPromptNonce.asStateFlow()

    private val _incognitoPinChallenge = MutableStateFlow<IncognitoPinChallenge?>(null)
    val incognitoPinChallenge: StateFlow<IncognitoPinChallenge?> = _incognitoPinChallenge.asStateFlow()

    private val _activeDownloadBannerId = MutableStateFlow<Long?>(null)
    private val _downloadBannerDismissed = MutableStateFlow(false)
    val activeDownloadBanner: StateFlow<DownloadEntity?> = combine(
        downloads,
        _activeDownloadBannerId,
        _downloadBannerDismissed
    ) { list, targetId, dismissed ->
        if (dismissed) null
        else {
            val selected = targetId?.let { id -> list.firstOrNull { it.id == id } }
            selected ?: list.firstOrNull {
                it.downloadStatus == DownloadStatus.DOWNLOADING || it.downloadStatus == DownloadStatus.QUEUED
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    private var lastSeenTopDownloadId: Long = -1L

    private val _fullscreenVideoView = MutableStateFlow<View?>(null)
    val fullscreenVideoView: StateFlow<View?> = _fullscreenVideoView.asStateFlow()
    private var fullscreenVideoCallback: WebChromeClient.CustomViewCallback? = null

    private val _pendingFileChooser = MutableStateFlow<Pair<ValueCallback<Array<Uri>>, Array<String>>?>(null)
    val pendingFileChooser: StateFlow<Pair<ValueCallback<Array<Uri>>, Array<String>>?> = _pendingFileChooser.asStateFlow()

    private val _barsHiddenByScroll = MutableStateFlow(false)
    val barsHiddenByScroll: StateFlow<Boolean> = _barsHiddenByScroll.asStateFlow()

    private val _pendingUnknownSourcesInstall = MutableStateFlow<DownloadEntity?>(null)
    val pendingUnknownSourcesInstall: StateFlow<DownloadEntity?> = _pendingUnknownSourcesInstall.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private var lastNavigationSignature: String = ""
    private var lastNavigationTimeMs: Long = 0L
    private val bypassedSslHosts = ConcurrentHashMap<String, MutableSet<String>>()
    private var failedPinAttempts: Int = 0
    private var pinLockoutUntilMs: Long = 0L

    private fun isSslBypassed(tabId: String, url: String): Boolean {
        val host = UrlResolver.extractHost(url)
        return host.isNotBlank() && bypassedSslHosts[tabId]?.contains(host) == true
    }

    init {
        viewModelScope.launch {
            repository.ensureDefaultShortcuts()
            repository.ensureDefaultReviews()
        }

        // Automatically open Package Installer once an in-app GitHub APK update finishes downloading
        viewModelScope.launch {
            downloadManager.completedDownloads.collect { finished ->
                val targetUrl = autoInstallUpdateUrl
                if (targetUrl != null && finished.url == targetUrl) {
                    autoInstallUpdateUrl = null
                    openDownloadedFile(finished)
                }
            }
        }

        // Non-blocking startup GitHub Release check after UI settles cleanly
        viewModelScope.launch {
            delay(1800L)
            checkForAppUpdates(silent = true)
        }

        // Automatically surface the animated Liquid Glass Live Download Card when any download starts
        viewModelScope.launch {
            repository.downloads.collect { list ->
                val newest = list.firstOrNull()
                if (newest != null && newest.id > lastSeenTopDownloadId) {
                    val isInitialLoad = lastSeenTopDownloadId == -1L
                    lastSeenTopDownloadId = newest.id
                    if (!isInitialLoad ||
                        newest.downloadStatus == DownloadStatus.DOWNLOADING ||
                        newest.downloadStatus == DownloadStatus.QUEUED
                    ) {
                        _activeDownloadBannerId.value = newest.id
                        _downloadBannerDismissed.value = false
                    }
                }
            }
        }

        // Sync settings & rules into AdBlockEngine and WebView pool continuously
        viewModelScope.launch {
            combine(repository.settings, repository.adBlockRules) { s, rules -> s to rules }
                .collect { (s, rules) ->
                    adBlockEngine.updateConfig(
                        adBlockOn = s.adBlockEnabled,
                        trackerBlockOn = s.trackerBlockEnabled,
                        rules = rules
                    )
                    webViewManager.updateGlobalSettings(s)
                    if (s.incognitoLockEnabled && !_incognitoUnlocked.value && activeTab.value.isIncognito) {
                        _isIncognitoVaultLocked.value = true
                    }
                }
        }

        webViewManager.attachCallbacks(
            TabWebEventCallbacks(
                onPageStarted = { tabId, url ->
                    updateTab(tabId) { tab ->
                        tab.copy(
                            url = url,
                            isLoading = true,
                            progress = 12,
                            networkErrorMessage = null,
                            securityState = SecurityUtils.classifyUrlSecurity(
                                url = url,
                                hasSslError = isSslBypassed(tabId, url)
                            )
                        )
                    }
                },
                onPageFinished = { tabId, url, title, canBack, canForward ->
                    val current = _tabs.value.firstOrNull { it.id == tabId }
                    updateTab(tabId) { tab ->
                        tab.copy(
                            url = url,
                            title = title.ifBlank { UrlResolver.displayHostOrTitle(url, tab.title) },
                            isLoading = false,
                            progress = 100,
                            canGoBack = canBack,
                            canGoForward = canForward,
                            securityState = SecurityUtils.classifyUrlSecurity(
                                url = url,
                                hasSslError = isSslBypassed(tabId, url)
                            ),
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    if (current != null && !current.isIncognito && url != UrlResolver.NOVA_HOME_URL) {
                        viewModelScope.launch {
                            repository.recordHistory(title.ifBlank { url }, url)
                        }
                    }
                },
                onProgressChanged = { tabId, progress ->
                    updateTab(tabId) { tab ->
                        tab.copy(
                            progress = progress,
                            isLoading = progress in 1..99
                        )
                    }
                },
                onTitleChanged = { tabId, title ->
                    updateTab(tabId) { it.copy(title = title) }
                },
                onNavigationStateChanged = { tabId, url, canBack, canForward ->
                    val resolved = url.ifBlank { activeTab.value.url }
                    updateTab(tabId) { tab ->
                        val targetUrl = url.ifBlank { tab.url }
                        tab.copy(
                            url = targetUrl,
                            canGoBack = canBack,
                            canGoForward = canForward,
                            securityState = SecurityUtils.classifyUrlSecurity(
                                url = targetUrl,
                                hasSslError = isSslBypassed(tabId, resolved)
                            )
                        )
                    }
                },
                onNetworkError = { tabId, failingUrl, description ->
                    updateTab(tabId) { tab ->
                        tab.copy(
                            isLoading = false,
                            progress = 0,
                            url = failingUrl.ifBlank { tab.url },
                            networkErrorMessage = description
                        )
                    }
                },
                onSslError = { tabId, host, primaryError, handler ->
                    val prev = _sslWarning.value
                    if (prev != null && prev.handler !== handler) {
                        prev.handler.cancel()
                    }
                    updateTab(tabId) {
                        it.copy(securityState = PageSecurityState.CERT_ERROR, isLoading = false)
                    }
                    _sslWarning.value = SslWarningRequest(tabId, host, primaryError, handler)
                },
                onDownloadRequested = { url, userAgent, contentDisposition, mimeType ->
                    startFileDownload(url, userAgent, contentDisposition, mimeType, fromWebView = true)
                },
                onOpenNewTabUrl = { url, isIncognito ->
                    openNewTab(
                        isIncognito = isIncognito,
                        initialUrl = url,
                        parentTabId = _activeTabId.value
                    )
                },
                onFullscreenVideoShow = { view, callback ->
                    fullscreenVideoCallback?.onCustomViewHidden()
                    fullscreenVideoCallback = callback
                    _fullscreenVideoView.value = view
                },
                onFullscreenVideoHide = {
                    _fullscreenVideoView.value = null
                    fullscreenVideoCallback?.onCustomViewHidden()
                    fullscreenVideoCallback = null
                },
                onFileChooserRequested = { callback, acceptTypes ->
                    _pendingFileChooser.value?.first?.onReceiveValue(null)
                    _pendingFileChooser.value = callback to acceptTypes
                },
                onSitePermissionRequested = { origin, resources, request ->
                    _sitePermissionPrompt.value = SitePermissionPrompt.WebMedia(origin, resources, request)
                },
                onGeolocationRequested = { origin, callback ->
                    _sitePermissionPrompt.value = SitePermissionPrompt.Geolocation(origin, callback)
                },
                onFindMatchesUpdated = { activeOrdinal, totalMatches ->
                    _findInPage.update {
                        it.copy(
                            activeMatchIndex = if (totalMatches > 0) activeOrdinal + 1 else 0,
                            totalMatches = totalMatches
                        )
                    }
                },
                onExternalIntentError = { message ->
                    emitMessage(message)
                },
                onScrollDirectionChanged = { hideBars ->
                    if (!activeTab.value.isHome) {
                        _barsHiddenByScroll.value = hideBars
                    } else {
                        _barsHiddenByScroll.value = false
                    }
                },
                onSiteNotificationPermissionRequested = { tabId, origin, host, jsCallbackId ->
                    if (!settings.value.notificationsEnabled) {
                        webViewManager.resolveJsNotificationPermission(tabId, jsCallbackId, false)
                    } else {
                        _sitePermissionPrompt.value = SitePermissionPrompt.WebNotification(
                            origin = origin,
                            host = host,
                            tabId = tabId,
                            jsCallbackId = jsCallbackId
                        )
                    }
                },
                onSiteNotificationPosted = { host, title, body, url ->
                    val s = settings.value
                    if (!s.notificationsEnabled) return@TabWebEventCallbacks
                    val rule = siteNotificationRules.value.firstOrNull { it.host == host }
                    if (rule != null && rule.allowed) {
                        val sound = NovaNotificationSound.fromId(
                            rule.soundId.ifBlank { s.siteNotificationSoundId }
                        )
                        NovaNotificationHelper.showSiteNotification(
                            context = getApplication(),
                            host = host,
                            title = title,
                            body = body,
                            sound = sound,
                            targetUrl = url
                        )
                    }
                },
                getSiteNotificationAllowed = { host ->
                    if (!settings.value.notificationsEnabled) {
                        false
                    } else {
                        val clean = host.trim().lowercase().removePrefix("www.")
                        siteNotificationRules.value.firstOrNull { it.host == clean }?.allowed
                    }
                },
                onBlobDownloadReady = { dataUrl, filename, mime ->
                    downloadManager.saveBase64OrBlobDownload(
                        payload = dataUrl,
                        suggestedFileName = filename.ifBlank { null },
                        suggestedMime = mime.ifBlank { null },
                        onStarted = { name -> emitMessage("Downloaded $name") }
                    )
                }
            )
        )
    }

    fun updateEffectiveWebsiteDarkMode(enabled: Boolean) {
        webViewManager.updateWebsiteDarkMode(enabled)
    }

    fun revealFloatingBars() {
        _barsHiddenByScroll.value = false
    }

    fun onActivityResume() {
        webViewManager.onActivityResume()
    }

    fun onActivityPause() {
        webViewManager.onActivityPause()
    }

    fun onAppBackgrounded() {
        val s = settings.value
        val hasBiometric = s.incognitoBiometricEnabled && BiometricHelper.isBiometricAvailable(getApplication())
        val shouldLock = (activeTab.value.isIncognito && (s.incognitoLockEnabled || hasBiometric)) ||
            (s.incognitoLockEnabled && _tabs.value.any { it.isIncognito })
        if (shouldLock) {
            _incognitoUnlocked.value = false
            if (activeTab.value.isIncognito) {
                _isIncognitoVaultLocked.value = true
            }
        }
    }

    fun onAppForegrounded() {
        if (_isIncognitoVaultLocked.value && activeTab.value.isIncognito) {
            triggerVaultUnlockPrompt()
        }
    }

    fun triggerVaultUnlockPrompt() {
        val s = settings.value
        if (s.incognitoBiometricEnabled && BiometricHelper.isBiometricAvailable(getApplication())) {
            _biometricPromptNonce.update { it + 1 }
        } else if (s.incognitoLockEnabled) {
            _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.UNLOCK_INCOGNITO_TABS)
        } else {
            _isIncognitoVaultLocked.value = false
            _incognitoUnlocked.value = true
        }
    }

    fun dismissDownloadBanner() {
        _downloadBannerDismissed.value = true
    }

    fun getActiveSiteCertificateSummary(): SiteCertificateSummary {
        val tab = activeTab.value
        val summary = webViewManager.getCertificateSummary(tab.id, tab.url)
        return if (tab.securityState == PageSecurityState.CERT_ERROR && summary.threatWarning == null) {
            summary.copy(threatWarning = "Warning: You proceeded past an untrusted TLS certificate on this host.")
        } else {
            summary
        }
    }

    fun emitMessage(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    fun openOverlay(overlay: BrowserOverlay) {
        _activeOverlay.value = overlay
    }

    fun closeOverlay() {
        _activeOverlay.value = BrowserOverlay.None
    }

    /**
     * Immediately navigates the active tab to a URL or Search query.
     * Prevents accidental duplicate triggers within 250ms for identical inputs.
     */
    fun navigateActiveTab(rawInput: String) {
        val currentSettings = settings.value
        val resolved = UrlResolver.resolveInput(
            rawInput = rawInput,
            searchEngine = currentSettings.searchEngine,
            httpsOnlyMode = currentSettings.httpsOnlyMode
        )
        val tab = activeTab.value
        val signature = "${tab.id}:$resolved"
        val now = System.currentTimeMillis()
        if (signature == lastNavigationSignature && (now - lastNavigationTimeMs) < 250L) {
            return
        }
        lastNavigationSignature = signature
        lastNavigationTimeMs = now

        _activeOverlay.value = BrowserOverlay.None
        _findInPage.value = FindInPageState()
        _barsHiddenByScroll.value = false

        if (resolved == UrlResolver.NOVA_HOME_URL) {
            goHome()
            return
        }

        SecurityUtils.detectSuspiciousUrlWarning(resolved)?.let { warning ->
            emitMessage(warning)
        }

        updateTab(tab.id) {
            it.copy(
                url = resolved,
                title = UrlResolver.displayHostOrTitle(resolved, it.title),
                isLoading = true,
                progress = 10,
                networkErrorMessage = null,
                securityState = SecurityUtils.classifyUrlSecurity(resolved)
            )
        }
        webViewManager.loadUrl(tab.id, resolved)
    }

    fun navigateFromExternalIntent(url: String) {
        if (url.isBlank()) return
        val current = activeTab.value
        if (current.isHome && !current.isIncognito) {
            navigateActiveTab(url)
        } else {
            openNewTab(isIncognito = false, initialUrl = url)
        }
    }

    /**
     * Deterministic Site-First Back Logic:
     * 1. First goes back inside the active website (skipping redirect loops & about:blank).
     * 2. If at the first page of a child tab opened from another tab, closes child tab & returns to parent tab.
     * 3. If currently on a website in a root tab, returns cleanly to NOVA Home.
     * 4. If already on NOVA Home and user came from another open tab, switches back to that previous tab.
     * Returns true if back navigation was consumed, false if system should exit app.
     */
    fun handleSystemBack(): Boolean {
        val tab = activeTab.value

        // Step 1: Go back inside current website first!
        if (!tab.isHome) {
            val result = webViewManager.goBackSmart(tab.id)
            if (result.handled) {
                updateTab(tab.id) { current ->
                    val nextUrl = result.targetUrl.ifBlank { current.url }
                    current.copy(
                        url = nextUrl,
                        title = result.targetTitle.ifBlank { UrlResolver.displayHostOrTitle(nextUrl, current.title) },
                        canGoBack = result.canGoBackAfter,
                        canGoForward = result.canGoForwardAfter,
                        networkErrorMessage = null,
                        securityState = SecurityUtils.classifyUrlSecurity(nextUrl)
                    )
                }
                return true
            }

            // Step 2: If this tab was spawned from a parent tab (e.g., link opened in new tab), return to parent tab
            val parentId = tab.parentTabId
            if (parentId != null && _tabs.value.any { it.id == parentId }) {
                closeTab(tab.id)
                _activeTabId.value = parentId
                return true
            }

            // Step 3: Otherwise return to NOVA Home instead of exiting the browser
            goHome(forceNovaHome = true)
            return true
        }

        // Step 4: Already on NOVA Home — if there is a previous open tab in visit history, return to it
        val validIds = _tabs.value.map { it.id }.toSet()
        val cleanedHistory = _tabVisitHistory.value.filter { it in validIds && it != tab.id }
        val previousTabId = cleanedHistory.lastOrNull()
        if (previousTabId != null) {
            _tabVisitHistory.value = cleanedHistory.dropLast(1)
            _activeTabId.value = previousTabId
            return true
        }

        return false
    }

    fun goBack() {
        handleSystemBack()
    }

    fun goForward() {
        val tab = activeTab.value
        if (!tab.isHome) {
            webViewManager.goForward(tab.id)
        }
    }

    fun reloadOrStop() {
        val tab = activeTab.value
        if (tab.isHome) return
        if (tab.isLoading) {
            webViewManager.stopLoading(tab.id)
            updateTab(tab.id) { it.copy(isLoading = false, progress = 0) }
        } else {
            updateTab(tab.id) { it.copy(networkErrorMessage = null, isLoading = true, progress = 15) }
            val existingWv = webViewManager.getExistingWebView(tab.id)
            if (existingWv != null && !existingWv.url.isNullOrBlank()) {
                webViewManager.reload(tab.id)
            } else {
                webViewManager.loadUrl(tab.id, tab.url)
            }
        }
    }

    fun goHome(forceNovaHome: Boolean = false) {
        val tab = activeTab.value
        _activeOverlay.value = BrowserOverlay.None
        _findInPage.value = FindInPageState()
        _barsHiddenByScroll.value = false
        val customHomeRaw = settings.value.customHomepageUrl.trim()
        val resolvedCustomHome = if (customHomeRaw.isNotEmpty()) {
            UrlResolver.resolveInput(customHomeRaw, settings.value.searchEngine, settings.value.httpsOnlyMode)
        } else {
            ""
        }
        if (!forceNovaHome &&
            resolvedCustomHome.startsWith("http") &&
            !tab.isHome &&
            !resolvedCustomHome.equals(tab.url, ignoreCase = true)
        ) {
            navigateActiveTab(resolvedCustomHome)
            return
        }
        webViewManager.stopLoading(tab.id)
        webViewManager.markHomeAnchor(tab.id)
        updateTab(tab.id) {
            it.copy(
                url = UrlResolver.NOVA_HOME_URL,
                title = if (it.isIncognito) "Private Home" else "NOVA Home",
                isLoading = false,
                progress = 0,
                canGoBack = false,
                canGoForward = false,
                networkErrorMessage = null,
                securityState = PageSecurityState.NOVA_HOME
            )
        }
    }

    fun openNewTab(
        isIncognito: Boolean = false,
        initialUrl: String = UrlResolver.NOVA_HOME_URL,
        parentTabId: String? = null
    ) {
        val s = settings.value
        val needsLockCheck = isIncognito && !_incognitoUnlocked.value && s.incognitoLockEnabled
        if (needsLockCheck) {
            if (s.incognitoBiometricEnabled && BiometricHelper.isBiometricAvailable(getApplication())) {
                _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.OPEN_INCOGNITO_TAB)
                _biometricPromptNonce.update { it + 1 }
            } else {
                _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.OPEN_INCOGNITO_TAB)
            }
            return
        }
        val previousId = _activeTabId.value
        val resolved = if (initialUrl == UrlResolver.NOVA_HOME_URL) {
            UrlResolver.NOVA_HOME_URL
        } else {
            UrlResolver.resolveInput(initialUrl, settings.value.searchEngine, settings.value.httpsOnlyMode)
        }
        val newTab = BrowserTab(
            parentTabId = parentTabId,
            title = if (resolved == UrlResolver.NOVA_HOME_URL) {
                if (isIncognito) "Private Home" else "NOVA Home"
            } else {
                UrlResolver.displayHostOrTitle(resolved, "Loading...")
            },
            url = resolved,
            isIncognito = isIncognito,
            isLoading = resolved != UrlResolver.NOVA_HOME_URL,
            progress = if (resolved != UrlResolver.NOVA_HOME_URL) 10 else 0,
            securityState = SecurityUtils.classifyUrlSecurity(resolved)
        )
        if (resolved != UrlResolver.NOVA_HOME_URL) {
            webViewManager.primeTabUrl(newTab.id, resolved)
        }
        _tabVisitHistory.update { (it.filterNot { id -> id == previousId } + previousId).takeLast(12) }
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        _activeOverlay.value = BrowserOverlay.None
        _findInPage.value = FindInPageState()
        _barsHiddenByScroll.value = false
    }

    fun selectTab(tabId: String) {
        val target = _tabs.value.firstOrNull { it.id == tabId } ?: return
        val s = settings.value
        if (target.isIncognito && !_incognitoUnlocked.value && s.incognitoLockEnabled) {
            if (s.incognitoBiometricEnabled && BiometricHelper.isBiometricAvailable(getApplication())) {
                _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.UNLOCK_INCOGNITO_TABS)
                _biometricPromptNonce.update { it + 1 }
            } else {
                _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.UNLOCK_INCOGNITO_TABS)
            }
            return
        }
        val previousId = _activeTabId.value
        if (previousId != tabId) {
            _tabVisitHistory.update { (it.filterNot { id -> id == previousId } + previousId).takeLast(12) }
        }
        _activeTabId.value = tabId
        _isIncognitoVaultLocked.value = false
        _activeOverlay.value = BrowserOverlay.None
        _findInPage.value = FindInPageState()
        _barsHiddenByScroll.value = false
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        val index = currentList.indexOfFirst { it.id == tabId }
        if (index < 0) return

        bypassedSslHosts.remove(tabId)
        webViewManager.destroyTab(tabId)
        _tabVisitHistory.update { it.filterNot { id -> id == tabId } }
        val remaining = currentList.filterNot { it.id == tabId }
        if (remaining.isEmpty()) {
            val fresh = BrowserTab()
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
            _incognitoUnlocked.value = false
        } else {
            _tabs.value = remaining
            if (_activeTabId.value == tabId) {
                val remainingIds = remaining.map { it.id }.toSet()
                val recentTabId = _tabVisitHistory.value.lastOrNull { it in remainingIds }
                val nextIndex = (index - 1).coerceAtLeast(0).coerceAtMost(remaining.lastIndex)
                _activeTabId.value = recentTabId ?: remaining[nextIndex].id
            }
            if (remaining.none { it.isIncognito }) {
                _incognitoUnlocked.value = false
            }
        }
    }

    fun closeAllTabs(incognitoOnly: Boolean) {
        val toClose = _tabs.value.filter { it.isIncognito == incognitoOnly }
        val closedIds = toClose.map { it.id }.toSet()
        toClose.forEach {
            bypassedSslHosts.remove(it.id)
            webViewManager.destroyTab(it.id)
        }
        _tabVisitHistory.update { it.filterNot { id -> id in closedIds } }
        val remaining = _tabs.value.filterNot { it.isIncognito == incognitoOnly }
        if (incognitoOnly) {
            _incognitoUnlocked.value = false
            webViewManager.clearIncognitoSessionArtifacts(clearPersistentIfSafe = true)
        }
        if (remaining.isEmpty()) {
            val fresh = BrowserTab()
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
        } else {
            _tabs.value = remaining
            if (remaining.none { it.id == _activeTabId.value }) {
                _activeTabId.value = remaining.first().id
            }
        }
    }

    fun toggleDesktopMode() {
        val tab = activeTab.value
        val next = !tab.desktopMode
        updateTab(tab.id) { it.copy(desktopMode = next) }
        webViewManager.setDesktopMode(tab.id, next)
        emitMessage(if (next) "Desktop site enabled" else "Mobile site enabled")
    }

    fun toggleFullscreenMode() {
        _isFullscreenMode.update { !it }
    }

    fun exitFullscreenVideo() {
        _fullscreenVideoView.value = null
        fullscreenVideoCallback?.onCustomViewHidden()
        fullscreenVideoCallback = null
    }

    // Find in Page
    fun openFindInPage() {
        _activeOverlay.value = BrowserOverlay.None
        _findInPage.value = FindInPageState(isActive = true)
    }

    fun updateFindQuery(query: String) {
        _findInPage.update { it.copy(query = query) }
        webViewManager.findInPage(activeTab.value.id, query)
    }

    fun findNextMatch(forward: Boolean) {
        webViewManager.findNext(activeTab.value.id, forward)
    }

    fun closeFindInPage() {
        webViewManager.clearFindMatches(activeTab.value.id)
        _findInPage.value = FindInPageState()
    }

    // Reading Mode
    fun openReadingMode() {
        val tab = activeTab.value
        if (tab.isHome) {
            emitMessage("Open a webpage first to use Reading Mode.")
            return
        }
        _activeOverlay.value = BrowserOverlay.ReadingMode
        _readingArticle.value = ReadingArticle(isLoading = true, title = tab.title)
        webViewManager.extractReadableArticle(tab.id) { title, byline, paragraphs ->
            _readingArticle.value = ReadingArticle(
                isLoading = false,
                title = title,
                byline = byline,
                paragraphs = paragraphs
            )
        }
    }

    // Translate current webpage
    fun translateCurrentPage() {
        val tab = activeTab.value
        if (tab.isHome) {
            emitMessage("Open a webpage first to translate it.")
            return
        }
        val translateUrl = UrlResolver.buildTranslateUrl(tab.url)
        navigateActiveTab(translateUrl)
        emitMessage("Opening translated page...")
    }

    // Bookmarks & History & Shortcuts & Notes
    fun toggleCurrentBookmark() {
        val tab = activeTab.value
        if (tab.isHome) {
            emitMessage("Navigate to a website to bookmark it.")
            return
        }
        viewModelScope.launch {
            val added = repository.toggleBookmark(tab.title, tab.url)
            emitMessage(if (added) "Saved to Bookmarks" else "Removed from Bookmarks")
        }
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch { repository.deleteBookmark(id) }

    fun deleteHistoryItem(id: Long) = viewModelScope.launch { repository.deleteHistoryItem(id) }

    fun clearAllBrowsingData() = viewModelScope.launch {
        repository.clearHistory()
        webViewManager.clearAllBrowsingData()
        adBlockEngine.resetStats()
        emitMessage("History, cache, and site cookies cleared")
    }

    fun addShortcut(title: String, rawUrl: String, accentHex: String = "#38BDF8") {
        val resolved = UrlResolver.resolveInput(rawUrl, settings.value.searchEngine, settings.value.httpsOnlyMode)
        viewModelScope.launch {
            repository.addShortcut(title.ifBlank { UrlResolver.displayHostOrTitle(resolved, "Site") }, resolved, accentHex)
            emitMessage("Shortcut added")
        }
    }

    fun deleteShortcut(id: Long) = viewModelScope.launch { repository.deleteShortcut(id) }

    fun saveQuickNote(id: Long = 0L, title: String, content: String, sourceUrl: String = "") {
        viewModelScope.launch {
            repository.saveQuickNote(
                QuickNoteEntity(
                    id = id,
                    title = title.trim().ifBlank { "Untitled Note" },
                    content = content.trim(),
                    sourceUrl = sourceUrl.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            emitMessage("Note saved")
        }
    }

    fun deleteQuickNote(id: Long) = viewModelScope.launch { repository.deleteQuickNote(id) }

    // Downloads
    fun startFileDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        fromWebView: Boolean = false
    ) {
        val currentTab = activeTab.value
        val safeReferer = if (fromWebView && !currentTab.isIncognito) currentTab.url else null
        downloadManager.startDownload(
            url = url,
            userAgent = userAgent,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            refererUrl = safeReferer
        ) { statusMessage ->
            if (statusMessage.startsWith("Blob ") || statusMessage.startsWith("Unsupported ")) {
                emitMessage(statusMessage)
            } else {
                emitMessage("Downloading $statusMessage")
            }
        }
    }

    fun pauseDownload(id: Long) = downloadManager.pauseDownload(id)
    fun resumeDownload(id: Long) = downloadManager.resumeDownload(id)
    fun retryDownload(id: Long) = downloadManager.retryDownload(id)
    fun deleteDownload(id: Long) = downloadManager.deleteDownload(id)

    fun openDownloadedFile(item: DownloadEntity) {
        when (val outcome = downloadManager.openDownloadedFile(item)) {
            OpenFileOutcome.Opened -> Unit
            is OpenFileOutcome.NeedsUnknownSourcesPermission -> {
                _pendingUnknownSourcesInstall.value = outcome.item
            }
            is OpenFileOutcome.Error -> emitMessage(outcome.message)
        }
    }

    fun dismissUnknownSourcesPrompt() {
        _pendingUnknownSourcesInstall.value = null
    }

    fun resumePendingApkInstallAfterSettings() {
        val pending = _pendingUnknownSourcesInstall.value ?: return
        _pendingUnknownSourcesInstall.value = null
        if (downloadManager.canInstallUnknownApps()) {
            openDownloadedFile(pending)
        } else {
            emitMessage("Enable 'Allow from this source' in Settings to install ${pending.fileName}")
        }
    }

    fun shareDownloadedFile(item: DownloadEntity) {
        val res = downloadManager.shareDownloadedFile(item)
        res.onFailure { emitMessage(it.localizedMessage ?: "Unable to share file") }
    }

    fun burnIncognitoSession() {
        closeAllTabs(incognitoOnly = true)
        emitMessage("Private session burned & cookies wiped")
    }

    fun installSiteAsWebApp(title: String, url: String) {
        val result = WebAppInstaller.installWebsiteToHomeScreen(getApplication(), title, url)
        result.onSuccess { pinned ->
            if (pinned) {
                emitMessage("Installing '$title' to Home Screen...")
            } else {
                addShortcut(title, url)
                emitMessage("Launcher pin unavailable — added '$title' to NOVA Quick Launch")
            }
        }.onFailure { err ->
            emitMessage(err.localizedMessage ?: "Unable to install website")
        }
    }

    // Ad Blocker
    fun setAdBlockEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setAdBlockEnabled(enabled)
        emitMessage(if (enabled) "Nova Ad Shield enabled" else "Nova Ad Shield paused")
    }

    fun setTrackerBlockEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setTrackerBlockEnabled(enabled)
    }

    fun toggleCurrentSiteAllowlist() {
        val host = UrlResolver.extractHost(activeTab.value.url).removePrefix("www.")
        if (host.isBlank()) return
        viewModelScope.launch {
            if (adBlockEngine.isHostAllowlisted(host)) {
                repository.removeAllowlistHost(host)
                emitMessage("Shield re-enabled on $host")
            } else {
                repository.addAdBlockRule(host, isAllowlist = true)
                emitMessage("Allowlisted $host")
            }
        }
    }

    fun addAdBlockRule(pattern: String, isAllowlist: Boolean) = viewModelScope.launch {
        repository.addAdBlockRule(pattern, isAllowlist)
    }

    fun toggleAdBlockRule(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setAdBlockRuleEnabled(id, enabled)
    }

    fun deleteAdBlockRule(id: Long) = viewModelScope.launch {
        repository.deleteAdBlockRule(id)
    }

    // SSL Warning resolution
    fun cancelSslConnection() {
        _sslWarning.value?.handler?.cancel()
        _sslWarning.value = null
        emitMessage("Blocked untrusted HTTPS certificate")
    }

    fun proceedUnsafeSslConnection() {
        val req = _sslWarning.value ?: return
        bypassedSslHosts.getOrPut(req.tabId) { ConcurrentHashMap.newKeySet() }.add(req.host)
        updateTab(req.tabId) { it.copy(securityState = PageSecurityState.CERT_ERROR) }
        req.handler.proceed()
        _sslWarning.value = null
        emitMessage("Proceeding with unverified certificate")
    }

    // Site Permission Prompt resolution
    fun resolveSitePermission(granted: Boolean) {
        when (val prompt = _sitePermissionPrompt.value) {
            is SitePermissionPrompt.WebMedia -> {
                if (granted) prompt.request.grant(prompt.resources) else prompt.request.deny()
            }
            is SitePermissionPrompt.Geolocation -> {
                prompt.callback.invoke(prompt.origin, granted, false)
            }
            is SitePermissionPrompt.WebNotification -> {
                viewModelScope.launch {
                    repository.setSiteNotificationRule(
                        host = prompt.host,
                        allowed = granted,
                        soundId = settings.value.siteNotificationSoundId
                    )
                }
                if (prompt.tabId != null && prompt.jsCallbackId != null) {
                    webViewManager.resolveJsNotificationPermission(prompt.tabId, prompt.jsCallbackId, granted)
                }
                emitMessage(
                    if (granted) "Notifications enabled for ${prompt.host}"
                    else "Notifications blocked for ${prompt.host}"
                )
            }
            null -> Unit
        }
        _sitePermissionPrompt.value = null
    }

    fun setSiteNotificationEnabled(host: String, allowed: Boolean, soundId: String = settings.value.siteNotificationSoundId) {
        val clean = host.trim().lowercase().removePrefix("www.")
        if (clean.isEmpty()) return
        viewModelScope.launch {
            if (allowed && !settings.value.notificationsEnabled) {
                repository.setNotificationsEnabled(true)
            }
            repository.setSiteNotificationRule(clean, allowed, soundId)
            emitMessage(if (allowed) "Site notifications enabled for $clean" else "Site notifications disabled for $clean")
        }
    }

    fun setSiteNotificationSoundForHost(host: String, soundId: String) {
        val clean = host.trim().lowercase().removePrefix("www.")
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val current = siteNotificationRules.value.firstOrNull { it.host == clean }
            repository.setSiteNotificationRule(clean, current?.allowed ?: true, soundId)
            NovaNotificationHelper.previewSound(getApplication(), NovaNotificationSound.fromId(soundId))
        }
    }

    fun deleteSiteNotificationRule(host: String) = viewModelScope.launch {
        repository.deleteSiteNotificationRule(host)
    }

    fun sendTestSiteNotification(host: String = "nova.browser", soundId: String = settings.value.siteNotificationSoundId) {
        val sound = NovaNotificationSound.fromId(soundId)
        NovaNotificationHelper.previewSound(getApplication(), sound)
        NovaNotificationHelper.showSiteNotification(
            context = getApplication(),
            host = host,
            title = "Site Alert · $host",
            body = "Notifications active using ${sound.label} sound",
            sound = sound,
            targetUrl = "https://$host"
        )
    }

    // File chooser resolution
    fun completeFileChooser(uris: List<Uri>?) {
        val pending = _pendingFileChooser.value ?: return
        pending.first.onReceiveValue(uris?.toTypedArray())
        _pendingFileChooser.value = null
    }

    // Incognito PIN Vault
    fun requestUnlockIncognitoSection() {
        val s = settings.value
        if (!s.incognitoLockEnabled || _incognitoUnlocked.value) return
        if (s.incognitoBiometricEnabled && BiometricHelper.isBiometricAvailable(getApplication())) {
            _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.UNLOCK_INCOGNITO_TABS)
            _biometricPromptNonce.update { it + 1 }
        } else {
            _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.UNLOCK_INCOGNITO_TABS)
        }
    }

    fun requestDisableIncognitoPin() {
        val s = settings.value
        if (!s.incognitoLockEnabled) {
            disableIncognitoPin()
            return
        }
        _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.DISABLE_INCOGNITO_PIN)
        if (s.incognitoBiometricEnabled && BiometricHelper.isBiometricAvailable(getApplication())) {
            _biometricPromptNonce.update { it + 1 }
        }
    }

    fun onBiometricUnlockSucceeded() {
        failedPinAttempts = 0
        pinLockoutUntilMs = 0L
        val pendingPurpose = _incognitoPinChallenge.value?.purpose
        _incognitoUnlocked.value = true
        _isIncognitoVaultLocked.value = false
        _incognitoPinChallenge.value = null
        when (pendingPurpose) {
            IncognitoVaultPurpose.OPEN_INCOGNITO_TAB -> {
                openNewTab(isIncognito = true)
                emitMessage("Private Vault unlocked")
            }
            IncognitoVaultPurpose.DISABLE_INCOGNITO_PIN -> {
                disableIncognitoPin()
            }
            else -> emitMessage("Private Vault unlocked")
        }
    }

    fun onBiometricFallbackToPin() {
        val s = settings.value
        if (s.incognitoLockEnabled) {
            if (_incognitoPinChallenge.value == null) {
                _incognitoPinChallenge.value = IncognitoPinChallenge(IncognitoVaultPurpose.UNLOCK_INCOGNITO_TABS)
            }
        }
    }

    fun exitLockedIncognitoToNormalTab() {
        val normal = _tabs.value.firstOrNull { !it.isIncognito }
        _isIncognitoVaultLocked.value = false
        _incognitoPinChallenge.value = null
        if (normal != null) {
            _activeTabId.value = normal.id
        } else {
            openNewTab(isIncognito = false)
        }
    }

    fun submitIncognitoPin(pin: String) {
        val s = settings.value
        val challenge = _incognitoPinChallenge.value ?: IncognitoPinChallenge(IncognitoVaultPurpose.UNLOCK_INCOGNITO_TABS)
        val now = System.currentTimeMillis()
        if (now < pinLockoutUntilMs) {
            val remainingSec = ((pinLockoutUntilMs - now) / 1000L).coerceAtLeast(1L)
            _incognitoPinChallenge.value = challenge.copy(
                errorMessage = "Too many attempts. Try again in ${remainingSec}s."
            )
            return
        }

        if (SecurityUtils.verifyPin(pin, s.incognitoPinHash, s.incognitoPinSalt)) {
            failedPinAttempts = 0
            pinLockoutUntilMs = 0L
            _incognitoUnlocked.value = true
            _isIncognitoVaultLocked.value = false
            _incognitoPinChallenge.value = null
            when (challenge.purpose) {
                IncognitoVaultPurpose.OPEN_INCOGNITO_TAB -> {
                    openNewTab(isIncognito = true)
                    emitMessage("Private Vault unlocked")
                }
                IncognitoVaultPurpose.UNLOCK_INCOGNITO_TABS -> {
                    emitMessage("Private Vault unlocked")
                }
                IncognitoVaultPurpose.DISABLE_INCOGNITO_PIN -> {
                    disableIncognitoPin()
                }
            }
        } else {
            failedPinAttempts++
            if (failedPinAttempts >= 5) {
                pinLockoutUntilMs = now + 30_000L
                failedPinAttempts = 0
                _incognitoPinChallenge.value = challenge.copy(
                    errorMessage = "Too many failed attempts. Wait 30 seconds."
                )
            } else {
                val left = 5 - failedPinAttempts
                _incognitoPinChallenge.value = challenge.copy(
                    errorMessage = "Incorrect PIN ($left attempts remaining)."
                )
            }
        }
    }

    fun dismissIncognitoPinChallenge() {
        _incognitoPinChallenge.value = null
    }

    fun setupIncognitoPin(pin: String) {
        if (pin.length < 4) {
            emitMessage("PIN must be at least 4 digits")
            return
        }
        viewModelScope.launch {
            val salt = SecurityUtils.generateSalt()
            val hash = SecurityUtils.hashPin(pin, salt)
            repository.setIncognitoPin(hash, salt)
            failedPinAttempts = 0
            pinLockoutUntilMs = 0L
            _incognitoUnlocked.value = true
            _isIncognitoVaultLocked.value = false
            emitMessage("Private tab PIN lock enabled")
        }
    }

    private fun disableIncognitoPin() = viewModelScope.launch {
        repository.clearIncognitoPin()
        failedPinAttempts = 0
        pinLockoutUntilMs = 0L
        _incognitoUnlocked.value = true
        _isIncognitoVaultLocked.value = false
        emitMessage("Private tab PIN lock disabled")
    }

    // VPN Controls
    fun startVpnConnection() {
        val s = settings.value
        NovaVpnService.startConnect(
            context = getApplication(),
            dnsServer = s.vpnDnsServer,
            gateway = s.vpnGatewayAddress,
            netShieldEnabled = s.vpnNetShieldEnabled,
            dohEnabled = s.vpnDohEnabled
        )
    }

    fun stopVpnConnection() {
        NovaVpnService.startDisconnect(getApplication())
    }

    fun setVpnNetShieldEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVpnNetShieldEnabled(enabled)
            if (vpnState.value.status == VpnConnectionStatus.CONNECTED_LOCAL_TUN) {
                val s = settings.value
                NovaVpnService.startConnect(
                    context = getApplication(),
                    dnsServer = s.vpnDnsServer,
                    gateway = s.vpnGatewayAddress,
                    netShieldEnabled = enabled,
                    dohEnabled = s.vpnDohEnabled
                )
            }
            emitMessage(if (enabled) "VPN NetShield Ad & Tracker Filter enabled" else "VPN NetShield paused")
        }
    }

    fun setVpnDohEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVpnDohEnabled(enabled)
            if (vpnState.value.status == VpnConnectionStatus.CONNECTED_LOCAL_TUN) {
                val s = settings.value
                NovaVpnService.startConnect(
                    context = getApplication(),
                    dnsServer = s.vpnDnsServer,
                    gateway = s.vpnGatewayAddress,
                    netShieldEnabled = s.vpnNetShieldEnabled,
                    dohEnabled = enabled
                )
            }
            emitMessage(if (enabled) "Encrypted DNS-over-HTTPS (DoH) enabled" else "Switched to standard UDP DNS")
        }
    }

    fun checkVpnPublicIp() {
        if (_vpnPublicIp.value.isChecking) return
        _vpnPublicIp.update { it.copy(isChecking = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val req = Request.Builder().url("https://ipwho.is/").build()
                vpnIpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    if (resp.isSuccessful && json.optBoolean("success", true)) {
                        val ip = json.optString("ip", "")
                        val country = json.optString("country", "")
                        val isp = json.optJSONObject("connection")?.optString("isp", "").orEmpty()
                        if (ip.isNotBlank()) {
                            return@runCatching VpnPublicIpState(
                                isChecking = false,
                                ipAddress = ip,
                                country = country,
                                isp = isp
                            )
                        }
                    }
                    null
                }
            }.getOrNull() ?: runCatching {
                val req = Request.Builder().url("https://api.ipify.org?format=json").build()
                vpnIpClient.newCall(req).execute().use { resp ->
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val ip = json.optString("ip", "")
                    if (resp.isSuccessful && ip.isNotBlank()) {
                        VpnPublicIpState(isChecking = false, ipAddress = ip)
                    } else null
                }
            }.getOrNull()

            _vpnPublicIp.value = result ?: VpnPublicIpState(
                isChecking = false,
                errorMessage = "Unable to check exit IP right now. Verify internet connection."
            )
        }
    }

    private fun installedAppVersionName(): String {
        val ctx = getApplication<Application>()
        return runCatching {
            val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            }
            info.versionName ?: "1.0.3"
        }.getOrDefault("1.0.3")
    }

    fun checkForAppUpdates(silent: Boolean = false) {
        if (_isCheckingForUpdates.value) return
        _isCheckingForUpdates.value = true
        viewModelScope.launch {
            val currentVersion = installedAppVersionName()
            val result = withContext(Dispatchers.IO) {
                GitHubUpdateChecker.fetchLatestUpdate(currentVersion)
            }
            _isCheckingForUpdates.value = false
            result.onSuccess { update ->
                if (update != null) {
                    if (!silent || dismissedUpdateTagThisSession != update.releaseTag) {
                        _availableAppUpdate.value = update
                    }
                } else if (!silent) {
                    emitMessage("Nova Browser v$currentVersion is already up to date")
                }
            }.onFailure {
                if (!silent) {
                    emitMessage("Could not check GitHub updates right now")
                }
            }
        }
    }

    fun dismissAvailableUpdate() {
        dismissedUpdateTagThisSession = _availableAppUpdate.value?.releaseTag
        _availableAppUpdate.value = null
    }

    fun startAppUpdateInstall(info: AppUpdateInfo) {
        _availableAppUpdate.value = null
        autoInstallUpdateUrl = info.apkDownloadUrl
        downloadManager.startDownload(
            url = info.apkDownloadUrl,
            mimeType = "application/vnd.android.package-archive",
            onStarted = { fileName ->
                emitMessage("Downloading update $fileName...")
            }
        )
    }

    // Settings updates
    fun updateThemeMode(mode: AppThemeMode) = viewModelScope.launch { repository.updateThemeMode(mode) }
    fun updateSearchEngine(engine: SearchEngine) = viewModelScope.launch { repository.updateSearchEngine(engine) }
    fun updateCustomHomepage(url: String) = viewModelScope.launch {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            repository.updateCustomHomepage("")
            emitMessage("Homepage reset to NOVA Liquid Glass")
            return@launch
        }
        val resolved = UrlResolver.resolveInput(trimmed, settings.value.searchEngine, settings.value.httpsOnlyMode)
        if (!resolved.startsWith("http://") && !resolved.startsWith("https://")) {
            emitMessage("Enter a valid http:// or https:// website address")
            return@launch
        }
        repository.updateCustomHomepage(resolved)
        emitMessage("Custom homepage saved")
    }
    fun setHttpsOnlyMode(enabled: Boolean) = viewModelScope.launch { repository.setHttpsOnlyMode(enabled) }
    fun setCookiesEnabled(enabled: Boolean) = viewModelScope.launch { repository.setCookiesEnabled(enabled) }
    fun setThirdPartyCookiesEnabled(enabled: Boolean) = viewModelScope.launch { repository.setThirdPartyCookiesEnabled(enabled) }
    fun setJavaScriptEnabled(enabled: Boolean) = viewModelScope.launch { repository.setJavaScriptEnabled(enabled) }
    fun setPopupBlockingEnabled(enabled: Boolean) = viewModelScope.launch { repository.setPopupBlockingEnabled(enabled) }
    fun setAutoDarkWebsites(enabled: Boolean) = viewModelScope.launch { repository.setAutoDarkWebsites(enabled) }
    fun setShowTopBar(enabled: Boolean) = viewModelScope.launch {
        repository.setShowTopBar(enabled)
        _barsHiddenByScroll.value = false
    }
    fun setShowBottomBar(enabled: Boolean) = viewModelScope.launch {
        repository.setShowBottomBar(enabled)
        _barsHiddenByScroll.value = false
    }
    fun setTextZoomPercent(zoom: Int) = viewModelScope.launch { repository.setTextZoomPercent(zoom) }
    fun setAllowCameraSites(enabled: Boolean) = viewModelScope.launch { repository.setAllowCameraSites(enabled) }
    fun setAllowMicSites(enabled: Boolean) = viewModelScope.launch { repository.setAllowMicSites(enabled) }
    fun setAllowLocationSites(enabled: Boolean) = viewModelScope.launch { repository.setAllowLocationSites(enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    fun setSiteNotificationSound(soundId: String) = viewModelScope.launch { repository.setSiteNotificationSound(soundId) }
    fun setIncognitoBiometricEnabled(enabled: Boolean) = viewModelScope.launch { repository.setIncognitoBiometricEnabled(enabled) }
    fun setVpnDnsServer(dns: String) = viewModelScope.launch { repository.setVpnDnsServer(dns) }
    fun setVpnGatewayAddress(address: String) = viewModelScope.launch { repository.setVpnGatewayAddress(address) }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    fun submitOwnReview(rating: Int, comment: String) {
        viewModelScope.launch {
            val account = userAccount.value
            val authorName = account?.displayName ?: account?.username ?: "Nova User"
            val authorAvatarHex = account?.avatarColorHex ?: "#8B5CF6"
            val authorId = account?.userId ?: "device_user"
            repository.addOrUpdateOwnReview(
                rating = rating,
                comment = comment,
                authorName = authorName,
                authorAvatarHex = authorAvatarHex,
                authorId = authorId
            )
            emitMessage("Review submitted! Thank you.")
        }
    }

    fun likeReview(id: Long) {
        viewModelScope.launch {
            repository.likeReview(id)
        }
    }

    fun deleteReview(id: Long) {
        viewModelScope.launch {
            repository.deleteReview(id)
            emitMessage("Review deleted")
        }
    }

    fun loginOrRegisterAccount(
        email: String,
        username: String,
        displayName: String,
        avatarColorHex: String = "#8B5CF6"
    ) {
        viewModelScope.launch {
            val acc = repository.loginOrRegisterAccount(email, username, displayName, avatarColorHex)
            emitMessage("Logged in as ${acc.displayName}")
        }
    }

    fun logoutAccount() {
        viewModelScope.launch {
            repository.logoutAccount()
            emitMessage("Signed out of Nova Account")
        }
    }

    fun updateAccountProfile(displayName: String, bio: String, avatarColorHex: String) {
        viewModelScope.launch {
            repository.updateAccountProfile(displayName, bio, avatarColorHex)
            emitMessage("Profile updated")
        }
    }

    override fun onCleared() {
        webViewManager.destroyAll()
        super.onCleared()
    }
}
