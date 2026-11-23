package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.webkit.PermissionRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.UrlResolver
import com.example.engine.WebAppInstaller
import com.example.ui.screens.AdBlockDashboardScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.BrowserMainScreen
import com.example.ui.screens.BrowserMenuOverlay
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.IncognitoPinDialog
import com.example.ui.screens.InstallWebAppDialog
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.QuickNotesScreen
import com.example.ui.screens.ReadingModeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SitePermissionDialog
import com.example.ui.screens.SiteSecurityInspectorDialog
import com.example.ui.screens.SplashOverlay
import com.example.ui.screens.SslWarningDialog
import com.example.ui.screens.TabSwitcherScreen
import com.example.ui.screens.UnknownSourcesPermissionDialog
import com.example.ui.screens.VpnSettingsScreen
import com.example.ui.theme.NovaBrowserTheme
import com.example.ui.theme.resolveEffectiveDarkTheme
import com.example.viewmodel.BrowserOverlay
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.SitePermissionPrompt

class MainActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            NovaBrowserApp(viewModel = browserViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        browserViewModel.onActivityResume()
    }

    override fun onPause() {
        browserViewModel.onActivityPause()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
                browserViewModel.navigateFromExternalIntent(url)
                if (intent.getBooleanExtra(WebAppInstaller.EXTRA_STANDALONE_WEB_APP, false)) {
                    browserViewModel.emitMessage("Opened Installed Web App")
                }
            }
        }
    }
}

@Composable
fun NovaBrowserApp(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val hasPreviousTabInHistory by viewModel.hasPreviousTabInHistory.collectAsStateWithLifecycle()
    val activeOverlay by viewModel.activeOverlay.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val quickNotes by viewModel.quickNotes.collectAsStateWithLifecycle()
    val adBlockRules by viewModel.adBlockRules.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val adBlockStats by viewModel.adBlockStats.collectAsStateWithLifecycle()
    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val findInPage by viewModel.findInPage.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreenMode.collectAsStateWithLifecycle()
    val fullscreenVideoView by viewModel.fullscreenVideoView.collectAsStateWithLifecycle()
    val sslWarning by viewModel.sslWarning.collectAsStateWithLifecycle()
    val sitePermissionPrompt by viewModel.sitePermissionPrompt.collectAsStateWithLifecycle()
    val readingArticle by viewModel.readingArticle.collectAsStateWithLifecycle()
    val incognitoUnlocked by viewModel.incognitoUnlocked.collectAsStateWithLifecycle()
    val incognitoPinChallenge by viewModel.incognitoPinChallenge.collectAsStateWithLifecycle()
    val pendingFileChooser by viewModel.pendingFileChooser.collectAsStateWithLifecycle()
    val barsHiddenByScroll by viewModel.barsHiddenByScroll.collectAsStateWithLifecycle()
    val pendingUnknownSourcesInstall by viewModel.pendingUnknownSourcesInstall.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSplash by remember { mutableStateOf(true) }
    var showInstallWebAppDialog by remember { mutableStateOf(false) }
    var showSiteSecurityDialog by remember { mutableStateOf(false) }

    // Enterprise privacy: block screenshots, screen capture & Recent Apps preview in Stealth Vault (Incognito)
    LaunchedEffect(activeTab.isIncognito) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        if (activeTab.isIncognito) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Automatically sync ALL websites' Dark Mode with Phone System Dark Mode / Nova Theme
    val effectiveDarkTheme = resolveEffectiveDarkTheme(
        themeMode = settings.themeMode,
        isIncognito = activeTab.isIncognito
    )
    val shouldDarkenWebsites = effectiveDarkTheme && settings.autoDarkWebsites
    LaunchedEffect(shouldDarkenWebsites) {
        viewModel.updateEffectiveWebsiteDarkMode(shouldDarkenWebsites)
    }

    // Launches Android's official "Install unknown apps" Settings screen and auto-installs APK upon return
    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.resumePendingApkInstallAfterSettings()
    }

    // Collect general snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Immediate "Install / Open" action prompt right after ANY download finishes!
    LaunchedEffect(Unit) {
        viewModel.completedDownloads.collect { finished ->
            val isApk = finished.fileName.endsWith(".apk", ignoreCase = true)
            val result = snackbarHostState.showSnackbar(
                message = "Downloaded ${finished.fileName}",
                actionLabel = if (isApk) "Install" else "Open",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.openDownloadedFile(finished)
            }
        }
    }

    // Zero-permission SAF file upload launcher for HTML <input type="file">
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.completeFileChooser(uris)
    }

    LaunchedEffect(pendingFileChooser) {
        val pending = pendingFileChooser ?: return@LaunchedEffect
        val mimeTypes = pending.second.filter { it.isNotBlank() }.ifEmpty { listOf("*/*") }.toTypedArray()
        runCatching {
            fileChooserLauncher.launch(mimeTypes)
        }.onFailure {
            viewModel.completeFileChooser(null)
            viewModel.emitMessage("Could not open system file picker.")
        }
    }

    // Runtime permission launcher for Website Camera / Microphone / Location
    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.resolveSitePermission(allGranted)
    }

    fun shareCurrentPage() {
        if (activeTab.isHome) {
            viewModel.emitMessage("Open a website to share it.")
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
            putExtra(Intent.EXTRA_TEXT, activeTab.url)
        }
        runCatching {
            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        }.onFailure {
            viewModel.emitMessage("No app available to share.")
        }
    }

    val isCurrentBookmarked = remember(bookmarks, activeTab.url) {
        bookmarks.any { it.url == activeTab.url }
    }

    val currentHostAllowlisted = remember(adBlockRules, activeTab.url) {
        val host = UrlResolver.extractHost(activeTab.url).removePrefix("www.")
        host.isNotBlank() && viewModel.adBlockEngine.isHostAllowlisted(host)
    }

    NovaBrowserTheme(
        themeMode = settings.themeMode,
        isIncognito = activeTab.isIncognito
    ) {
        // Use a GPU-safe translucent scrim instead of RenderEffect Modifier.blur() over WebView/video surfaces
        val overlayScrimAlpha by animateFloatAsState(
            targetValue = if (activeOverlay != BrowserOverlay.None) 0.34f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "glass_overlay_scrim"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            BrowserMainScreen(
                activeTab = activeTab,
                tabCount = tabs.size,
                hasPreviousTabInHistory = hasPreviousTabInHistory,
                settings = settings,
                shortcuts = shortcuts,
                adBlockStats = adBlockStats,
                vpnState = vpnState,
                chromiumInfo = viewModel.chromiumInfo,
                findInPage = findInPage,
                isFullscreenMode = isFullscreen,
                barsHiddenByScroll = barsHiddenByScroll,
                fullscreenVideoView = fullscreenVideoView,
                webViewManager = viewModel.webViewManager,
                snackbarHostState = snackbarHostState,
                onNavigate = viewModel::navigateActiveTab,
                onBack = viewModel::goBack,
                onHome = { viewModel.goHome() },
                onNewTab = { viewModel.openNewTab(isIncognito = activeTab.isIncognito) },
                onReloadOrStop = viewModel::reloadOrStop,
                onOpenTabSwitcher = { viewModel.openOverlay(BrowserOverlay.TabSwitcher) },
                onOpenMenu = { viewModel.openOverlay(BrowserOverlay.Menu) },
                onOpenAdBlock = { viewModel.openOverlay(BrowserOverlay.AdBlockDashboard) },
                onInspectSiteSecurity = { showSiteSecurityDialog = true },
                onOpenVpn = { viewModel.openOverlay(BrowserOverlay.VpnSettings) },
                onOpenDownloads = { viewModel.openOverlay(BrowserOverlay.Downloads) },
                onOpenBookmarks = { viewModel.openOverlay(BrowserOverlay.Bookmarks) },
                onOpenHistory = { viewModel.openOverlay(BrowserOverlay.History) },
                onOpenNotes = { viewModel.openOverlay(BrowserOverlay.QuickNotes) },
                onOpenQrScanner = { viewModel.openOverlay(BrowserOverlay.QrScanner) },
                onOpenIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                onBurnIncognitoSession = viewModel::burnIncognitoSession,
                onAddShortcut = { title, url -> viewModel.addShortcut(title, url) },
                onDeleteShortcut = viewModel::deleteShortcut,
                onUpdateFindQuery = viewModel::updateFindQuery,
                onFindNext = viewModel::findNextMatch,
                onCloseFindInPage = viewModel::closeFindInPage,
                onExitFullscreen = viewModel::toggleFullscreenMode,
                onExitFullscreenVideo = viewModel::exitFullscreenVideo
            )

            if (overlayScrimAlpha > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            (if (effectiveDarkTheme) Color(0xFF030712) else Color(0xFF0F172A))
                                .copy(alpha = overlayScrimAlpha)
                        )
                )
            }

            // Smooth Animated Liquid Glass Overlay Transitions
            AnimatedContent(
                targetState = activeOverlay,
                transitionSpec = {
                    if (targetState == BrowserOverlay.Menu) {
                        (slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 3 } +
                            fadeIn(tween(200)))
                            .togetherWith(fadeOut(tween(150)))
                    } else if (initialState == BrowserOverlay.Menu && targetState == BrowserOverlay.None) {
                        fadeIn(tween(160))
                            .togetherWith(
                                slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it / 3 } +
                                    fadeOut(tween(170))
                            )
                    } else {
                        (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(tween(170)) + scaleOut(targetScale = 0.97f, animationSpec = tween(170))
                            )
                    }
                },
                label = "browser_overlay_transition"
            ) { overlay ->
                when (overlay) {
                    BrowserOverlay.None -> Unit
                    BrowserOverlay.TabSwitcher -> {
                        TabSwitcherScreen(
                            tabs = tabs,
                            activeTabId = activeTab.id,
                            incognitoLockEnabled = settings.incognitoLockEnabled,
                            incognitoUnlocked = incognitoUnlocked,
                            onSelectTab = viewModel::selectTab,
                            onCloseTab = viewModel::closeTab,
                            onNewTab = { incognito -> viewModel.openNewTab(isIncognito = incognito) },
                            onCloseAll = viewModel::closeAllTabs,
                            onRequestUnlockIncognito = viewModel::requestUnlockIncognitoSection,
                            onDismiss = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.Menu -> {
                        BrowserMenuOverlay(
                            activeTab = activeTab,
                            tabCount = tabs.size,
                            themeMode = settings.themeMode,
                            autoDarkWebsites = settings.autoDarkWebsites,
                            showTopBar = settings.showTopBar,
                            showBottomBar = settings.showBottomBar,
                            isBookmarked = isCurrentBookmarked,
                            adBlockEnabled = settings.adBlockEnabled,
                            blockedCount = adBlockStats.totalBlocked,
                            isFullscreen = isFullscreen,
                            onDismiss = viewModel::closeOverlay,
                            onUpdateTheme = { mode ->
                                viewModel.updateThemeMode(mode)
                                viewModel.emitMessage("Theme: ${mode.label}")
                            },
                            onToggleAutoDarkWebsites = {
                                val next = !settings.autoDarkWebsites
                                viewModel.setAutoDarkWebsites(next)
                                viewModel.emitMessage(
                                    if (next) "Dark Mode enabled for all websites"
                                    else "Website Dark Mode turned off"
                                )
                            },
                            onToggleShowTopBar = {
                                val next = !settings.showTopBar
                                viewModel.setShowTopBar(next)
                                viewModel.emitMessage(if (next) "Top Floating Bar shown" else "Top Floating Bar hidden")
                            },
                            onToggleShowBottomBar = {
                                val next = !settings.showBottomBar
                                viewModel.setShowBottomBar(next)
                                viewModel.emitMessage(if (next) "Bottom Floating Dock shown" else "Bottom Floating Dock hidden")
                            },
                            onGoHome = { viewModel.goHome() },
                            onOpenTabSwitcher = { viewModel.openOverlay(BrowserOverlay.TabSwitcher) },
                            onForward = viewModel::goForward,
                            onReloadOrStop = viewModel::reloadOrStop,
                            onToggleBookmark = viewModel::toggleCurrentBookmark,
                            onShare = ::shareCurrentPage,
                            onNewTab = { viewModel.openNewTab(isIncognito = false) },
                            onNewIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                            onOpenBookmarks = { viewModel.openOverlay(BrowserOverlay.Bookmarks) },
                            onOpenHistory = { viewModel.openOverlay(BrowserOverlay.History) },
                            onOpenDownloads = { viewModel.openOverlay(BrowserOverlay.Downloads) },
                            onOpenAdBlock = { viewModel.openOverlay(BrowserOverlay.AdBlockDashboard) },
                            onOpenVpn = { viewModel.openOverlay(BrowserOverlay.VpnSettings) },
                            onOpenNotes = { viewModel.openOverlay(BrowserOverlay.QuickNotes) },
                            onOpenFindInPage = viewModel::openFindInPage,
                            onOpenReadingMode = viewModel::openReadingMode,
                            onTranslatePage = viewModel::translateCurrentPage,
                            onToggleDesktopMode = viewModel::toggleDesktopMode,
                            onToggleFullscreen = viewModel::toggleFullscreenMode,
                            onInstallWebApp = {
                                if (activeTab.isHome) {
                                    viewModel.emitMessage("Open any website first to install it to your Home Screen.")
                                } else {
                                    showInstallWebAppDialog = true
                                }
                            },
                            onOpenQrScanner = { viewModel.openOverlay(BrowserOverlay.QrScanner) },
                            onOpenSettings = { viewModel.openOverlay(BrowserOverlay.Settings) }
                        )
                    }
                    BrowserOverlay.Bookmarks -> {
                        BookmarksScreen(
                            bookmarks = bookmarks,
                            isIncognito = activeTab.isIncognito,
                            onOpenUrl = viewModel::navigateActiveTab,
                            onDeleteBookmark = viewModel::deleteBookmark,
                            onBack = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.History -> {
                        HistoryScreen(
                            history = history,
                            isIncognito = activeTab.isIncognito,
                            onOpenUrl = viewModel::navigateActiveTab,
                            onDeleteItem = viewModel::deleteHistoryItem,
                            onClearAll = viewModel::clearAllBrowsingData,
                            onBack = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.Downloads -> {
                        DownloadsScreen(
                            downloads = downloads,
                            isIncognito = activeTab.isIncognito,
                            onStartDownloadUrl = { url -> viewModel.startFileDownload(url) },
                            onPause = viewModel::pauseDownload,
                            onResume = viewModel::resumeDownload,
                            onRetry = viewModel::retryDownload,
                            onOpen = viewModel::openDownloadedFile,
                            onShare = viewModel::shareDownloadedFile,
                            onDelete = viewModel::deleteDownload,
                            onBack = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.AdBlockDashboard -> {
                        AdBlockDashboardScreen(
                            adBlockEnabled = settings.adBlockEnabled,
                            trackerBlockEnabled = settings.trackerBlockEnabled,
                            stats = adBlockStats,
                            rules = adBlockRules,
                            currentSiteUrl = activeTab.url,
                            isCurrentSiteAllowlisted = currentHostAllowlisted,
                            isIncognito = activeTab.isIncognito,
                            onSetAdBlockEnabled = viewModel::setAdBlockEnabled,
                            onSetTrackerBlockEnabled = viewModel::setTrackerBlockEnabled,
                            onToggleCurrentSiteAllowlist = viewModel::toggleCurrentSiteAllowlist,
                            onAddRule = viewModel::addAdBlockRule,
                            onToggleRule = viewModel::toggleAdBlockRule,
                            onDeleteRule = viewModel::deleteAdBlockRule,
                            onBack = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.VpnSettings -> {
                        VpnSettingsScreen(
                            vpnState = vpnState,
                            savedDns = settings.vpnDnsServer,
                            savedGateway = settings.vpnGatewayAddress,
                            isIncognito = activeTab.isIncognito,
                            onSaveDns = viewModel::setVpnDnsServer,
                            onSaveGateway = viewModel::setVpnGatewayAddress,
                            onStartVpn = viewModel::startVpnConnection,
                            onStopVpn = viewModel::stopVpnConnection,
                            onShowMessage = viewModel::emitMessage,
                            onBack = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.QuickNotes -> {
                        QuickNotesScreen(
                            notes = quickNotes,
                            currentPageUrl = activeTab.url,
                            isIncognito = activeTab.isIncognito,
                            onSaveNote = viewModel::saveQuickNote,
                            onDeleteNote = viewModel::deleteQuickNote,
                            onOpenUrl = viewModel::navigateActiveTab,
                            onBack = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.QrScanner -> {
                        QrScannerScreen(
                            onCodeScanned = { code ->
                                viewModel.closeOverlay()
                                viewModel.navigateActiveTab(code)
                            },
                            onBack = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.ReadingMode -> {
                        ReadingModeScreen(
                            article = readingArticle,
                            onBack = viewModel::closeOverlay
                        )
                    }
                    BrowserOverlay.Settings -> {
                        SettingsScreen(
                            settings = settings,
                            vpnState = vpnState,
                            chromiumInfo = viewModel.chromiumInfo,
                            isIncognito = activeTab.isIncognito,
                            onUpdateTheme = viewModel::updateThemeMode,
                            onUpdateSearchEngine = viewModel::updateSearchEngine,
                            onUpdateCustomHomepage = viewModel::updateCustomHomepage,
                            onSetAutoDarkWebsites = viewModel::setAutoDarkWebsites,
                            onSetShowTopBar = viewModel::setShowTopBar,
                            onSetShowBottomBar = viewModel::setShowBottomBar,
                            onSetTextZoom = viewModel::setTextZoomPercent,
                            onSetAdBlock = viewModel::setAdBlockEnabled,
                            onSetHttpsOnly = viewModel::setHttpsOnlyMode,
                            onSetCookies = viewModel::setCookiesEnabled,
                            onSetThirdPartyCookies = viewModel::setThirdPartyCookiesEnabled,
                            onSetJavaScript = viewModel::setJavaScriptEnabled,
                            onSetPopupBlocking = viewModel::setPopupBlockingEnabled,
                            onSetAllowCamera = viewModel::setAllowCameraSites,
                            onSetAllowMic = viewModel::setAllowMicSites,
                            onSetAllowLocation = viewModel::setAllowLocationSites,
                            onSetNotifications = viewModel::setNotificationsEnabled,
                            onSetupIncognitoPin = viewModel::setupIncognitoPin,
                            onDisableIncognitoPin = viewModel::disableIncognitoPin,
                            onClearAllBrowsingData = viewModel::clearAllBrowsingData,
                            onOpenAdBlockDashboard = { viewModel.openOverlay(BrowserOverlay.AdBlockDashboard) },
                            onOpenVpnSettings = { viewModel.openOverlay(BrowserOverlay.VpnSettings) },
                            onOpenDownloads = { viewModel.openOverlay(BrowserOverlay.Downloads) },
                            onShowMessage = viewModel::emitMessage,
                            onBack = viewModel::closeOverlay
                        )
                    }
                }
            }

            // Chrome-Style Install Site Dialog
            if (showInstallWebAppDialog && !activeTab.isHome) {
                InstallWebAppDialog(
                    initialTitle = activeTab.title,
                    url = activeTab.url,
                    onInstall = { name ->
                        showInstallWebAppDialog = false
                        viewModel.installSiteAsWebApp(name, activeTab.url)
                    },
                    onDismiss = { showInstallWebAppDialog = false }
                )
            }

            // Security & Vault Dialogs
            sslWarning?.let { req ->
                SslWarningDialog(
                    request = req,
                    onCancel = viewModel::cancelSslConnection,
                    onProceedUnsafe = viewModel::proceedUnsafeSslConnection
                )
            }

            sitePermissionPrompt?.let { prompt ->
                SitePermissionDialog(
                    prompt = prompt,
                    onAllow = {
                        val needed = mutableListOf<String>()
                        when (prompt) {
                            is SitePermissionPrompt.WebMedia -> {
                                if (prompt.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                                    if (!settings.allowCameraSites) {
                                        viewModel.resolveSitePermission(false)
                                        viewModel.emitMessage("Camera access for websites is disabled in Settings.")
                                        return@SitePermissionDialog
                                    }
                                    needed.add(Manifest.permission.CAMERA)
                                }
                                if (prompt.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                                    if (!settings.allowMicSites) {
                                        viewModel.resolveSitePermission(false)
                                        viewModel.emitMessage("Microphone access for websites is disabled in Settings.")
                                        return@SitePermissionDialog
                                    }
                                    needed.add(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            is SitePermissionPrompt.Geolocation -> {
                                if (!settings.allowLocationSites) {
                                    viewModel.resolveSitePermission(false)
                                    viewModel.emitMessage("Location access for websites is disabled in Settings.")
                                    return@SitePermissionDialog
                                }
                                needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        }

                        val missing = needed.filter {
                            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                        }
                        if (missing.isEmpty()) {
                            viewModel.resolveSitePermission(true)
                        } else {
                            runtimePermissionsLauncher.launch(missing.toTypedArray())
                        }
                    },
                    onDeny = { viewModel.resolveSitePermission(false) }
                )
            }

            incognitoPinChallenge?.let { challenge ->
                IncognitoPinDialog(
                    challenge = challenge,
                    onSubmit = viewModel::submitIncognitoPin,
                    onDismiss = viewModel::dismissIncognitoPinChallenge
                )
            }

            // Official Android "Install Unknown Apps" Permission Prompt for APKs
            pendingUnknownSourcesInstall?.let { apkItem ->
                UnknownSourcesPermissionDialog(
                    fileName = apkItem.fileName,
                    onOpenSettings = {
                        runCatching {
                            unknownSourcesLauncher.launch(
                                viewModel.downloadManager.createUnknownSourcesSettingsIntent()
                            )
                        }.onFailure {
                            viewModel.dismissUnknownSourcesPrompt()
                            viewModel.emitMessage("Open Android Settings -> Install unknown apps -> Allow Nova Browser")
                        }
                    },
                    onDismiss = viewModel::dismissUnknownSourcesPrompt
                )
            }

            // Live HTTPS & Site Security Inspector Dialog
            if (showSiteSecurityDialog && !activeTab.isHome) {
                val certSummary = remember(activeTab.id, activeTab.url, activeTab.lastUpdatedAt) {
                    viewModel.getActiveSiteCertificateSummary()
                }
                SiteSecurityInspectorDialog(
                    summary = certSummary,
                    adBlockEnabled = settings.adBlockEnabled,
                    isHostAllowlisted = currentHostAllowlisted,
                    httpsOnlyMode = settings.httpsOnlyMode,
                    isIncognito = activeTab.isIncognito,
                    onToggleAllowlist = viewModel::toggleCurrentSiteAllowlist,
                    onToggleHttpsOnly = viewModel::setHttpsOnlyMode,
                    onOpenShieldDashboard = { viewModel.openOverlay(BrowserOverlay.AdBlockDashboard) },
                    onDismiss = { showSiteSecurityDialog = false }
                )
            }

            // Fast Liquid Glass Splash Reveal
            SplashOverlay(
                visible = showSplash,
                onFinished = { showSplash = false }
            )
        }
    }
}
