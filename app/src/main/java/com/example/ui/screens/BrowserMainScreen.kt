package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.data.local.ShortcutEntity
import com.example.data.preferences.BrowserSettings
import com.example.engine.AdBlockStats
import com.example.engine.ChromiumEngineInfo
import com.example.engine.PageSecurityState
import com.example.engine.TabWebViewManager
import com.example.engine.UrlResolver
import com.example.ui.components.GlassIconButton
import com.example.ui.components.IphoneLiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidProgressBar
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.components.NovaWallpaperBackground
import com.example.ui.components.sakugaSquashScale
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import com.example.viewmodel.BrowserTab
import com.example.viewmodel.FindInPageState
import com.example.vpn.VpnRuntimeState

@Composable
fun BrowserMainScreen(
    activeTab: BrowserTab,
    tabCount: Int,
    hasPreviousTabInHistory: Boolean,
    settings: BrowserSettings,
    shortcuts: List<ShortcutEntity>,
    adBlockStats: AdBlockStats,
    vpnState: VpnRuntimeState,
    chromiumInfo: ChromiumEngineInfo,
    findInPage: FindInPageState,
    isFullscreenMode: Boolean,
    barsHiddenByScroll: Boolean,
    fullscreenVideoView: View?,
    webViewManager: TabWebViewManager,
    hasOverlayOpen: Boolean = false,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onReloadOrStop: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenAdBlock: () -> Unit,
    onInspectSiteSecurity: () -> Unit,
    onOpenVpn: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenIncognitoTab: () -> Unit,
    onBurnIncognitoSession: () -> Unit,
    onAddShortcut: (String, String) -> Unit,
    onDeleteShortcut: (Long) -> Unit,
    onUpdateFindQuery: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onCloseFindInPage: () -> Unit,
    onExitFullscreen: () -> Unit,
    onExitFullscreenVideo: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    var isOmniboxFocused by remember { mutableStateOf(false) }
    var omniboxValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (activeTab.isHome) "" else activeTab.url
            )
        )
    }

    LaunchedEffect(activeTab.id, activeTab.url, isOmniboxFocused) {
        if (!isOmniboxFocused) {
            val display = if (activeTab.isHome) "" else activeTab.url
            omniboxValue = TextFieldValue(display, TextRange(display.length))
        }
    }

    // Unified deterministic system back handler:
    // Priority: Fullscreen video -> Find-in-page -> Fullscreen UI -> Current Website Back -> Parent Tab / NOVA Home -> Previous Tab
    val shouldInterceptBack = fullscreenVideoView != null ||
        findInPage.isActive ||
        isFullscreenMode ||
        !activeTab.isHome ||
        hasPreviousTabInHistory

    BackHandler(enabled = shouldInterceptBack) {
        when {
            fullscreenVideoView != null -> onExitFullscreenVideo()
            findInPage.isActive -> onCloseFindInPage()
            isFullscreenMode -> onExitFullscreen()
            else -> onBack()
        }
    }

    fun submitOmnibox() {
        val query = omniboxValue.text.trim()
        keyboardController?.hide()
        focusManager.clearFocus()
        isOmniboxFocused = false
        if (query.isNotEmpty()) {
            onNavigate(query)
        }
    }

    // Smooth immersive system-bars control during fullscreen HTML5 video playback
    val view = LocalView.current
    DisposableEffect(fullscreenVideoView != null) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (fullscreenVideoView != null && controller != null) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (fullscreenVideoView != null && controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val overlayBlurDp by animateDpAsState(
        targetValue = if (hasOverlayOpen) 28.dp else 0.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "overlay_content_blur"
    )
    val overlayDepthScale by animateFloatAsState(
        targetValue = if (hasOverlayOpen) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "overlay_depth_scale"
    )
    val overlayBlurModifier = if (overlayBlurDp > 0.5.dp) {
        Modifier.blur(radius = overlayBlurDp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        Modifier
    }
    val glassTheme = LocalNovaGlassColors.current
    var webBackdropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    DisposableEffect(hasOverlayOpen, activeTab.id, activeTab.isHome, glassTheme.isDark) {
        var active = true
        if (hasOverlayOpen && !activeTab.isHome) {
            val window = (view.context as? Activity)?.window
            val fallback = if (glassTheme.isDark) {
                android.graphics.Color.parseColor("#0A0E1B")
            } else {
                android.graphics.Color.WHITE
            }
            webViewManager.captureLowResBlurBitmap(
                tabId = activeTab.id,
                window = window,
                fallbackColor = fallback
            ) { bmp ->
                if (active) {
                    webBackdropBitmap = bmp
                }
            }
        } else {
            webBackdropBitmap = null
        }
        onDispose {
            active = false
            webBackdropBitmap = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NovaWallpaperBackground(
            isIncognito = activeTab.isIncognito,
            backdropBlur = if (hasOverlayOpen) 28.dp else 0.dp
        )

        // Smooth GPU-safe Liquid Crossfade between NOVA Home and Full-Bleed Live Webpage
        // (avoids offscreen scale RenderNode layers over AndroidView(WebView) & video surfaces)
        AnimatedContent(
            targetState = activeTab.isHome,
            transitionSpec = {
                fadeIn(tween(240, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(tween(180, easing = FastOutSlowInEasing)))
            },
            label = "site_open_liquid_transition",
            modifier = Modifier.fillMaxSize()
        ) { showingHome ->
            if (showingHome) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = overlayDepthScale
                            scaleY = overlayDepthScale
                        }
                        .then(overlayBlurModifier)
                ) {
                    NovaHomeScreen(
                        isIncognito = activeTab.isIncognito,
                        searchEngine = settings.searchEngine,
                        shortcuts = shortcuts,
                        adBlockEnabled = settings.adBlockEnabled,
                        adBlockStats = adBlockStats,
                        vpnState = vpnState,
                        chromiumInfo = chromiumInfo,
                        topContentPadding = if (isFullscreenMode || !settings.showTopBar) 16.dp else 72.dp,
                        bottomContentPadding = if (isFullscreenMode || isImeVisible || !settings.showBottomBar) 16.dp else 92.dp,
                        onNavigate = onNavigate,
                        onAddShortcut = onAddShortcut,
                        onDeleteShortcut = onDeleteShortcut,
                        onOpenIncognitoTab = onOpenIncognitoTab,
                        onBurnIncognitoSession = onBurnIncognitoSession,
                        onOpenQrScanner = onOpenQrScanner,
                        onOpenAdBlock = onOpenAdBlock,
                        onOpenVpn = onOpenVpn,
                        onOpenDownloads = onOpenDownloads,
                        onOpenBookmarks = onOpenBookmarks,
                        onOpenHistory = onOpenHistory,
                        onOpenNotes = onOpenNotes
                    )
                }
            } else {
                // Full-bleed Chromium WebView flows seamlessly behind BOTH top & bottom transparent Liquid Glass pills
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { container ->
                            val wv = webViewManager.getOrCreateWebView(
                                context = container.context,
                                tabId = activeTab.id,
                                isIncognito = activeTab.isIncognito,
                                desktopMode = activeTab.desktopMode
                            )
                            if (wv.parent !== container) {
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                container.removeAllViews()
                                container.addView(
                                    wv,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                            if (wv.url.isNullOrBlank() && activeTab.url.startsWith("http")) {
                                webViewManager.loadUrl(activeTab.id, activeTab.url)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chromium_webview_container")
                    )

                    // Subtle soft feathered fade only under the Android status bar icons
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(14.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.36f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Graceful Network / Renderer Error Card Overlay
                    if (!activeTab.networkErrorMessage.isNullOrBlank()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                                .padding(24.dp)
                        ) {
                            LiquidGlassSurface(
                                shape = RoundedCornerShape(24.dp),
                                strong = true,
                                modifier = Modifier.widthIn(max = 420.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WifiOff,
                                        contentDescription = null,
                                        tint = CoralDanger,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Page Couldn't Load",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = activeTab.networkErrorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(onClick = onReloadOrStop) {
                                            Text("Reload")
                                        }
                                        Button(onClick = onHome) {
                                            Text("Home")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Smooth Frosted Blur Veil over active webpage when 3-dot Menu / Overlays open
                    AnimatedVisibility(
                        visible = hasOverlayOpen,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(180)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val glass = LocalNovaGlassColors.current
                        Box(modifier = Modifier.fillMaxSize()) {
                            webBackdropBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    filterQuality = FilterQuality.High,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(overlayBlurModifier)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.background.copy(
                                            alpha = if (glass.isDark) 0.44f else 0.38f
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        var expandCompactScrolledBars by remember(activeTab.id, barsHiddenByScroll) { mutableStateOf(false) }
        val barsVisible = !isFullscreenMode &&
            fullscreenVideoView == null &&
            (activeTab.isHome || !barsHiddenByScroll || expandCompactScrolledBars || isOmniboxFocused || findInPage.isActive)

        val showTopCapsule = barsVisible && (settings.showTopBar || isOmniboxFocused || findInPage.isActive)
        val showBottomCapsule = barsVisible && !isImeVisible && settings.showBottomBar
        val showCompactScrollBadge = !barsVisible &&
            !isFullscreenMode &&
            fullscreenVideoView == null &&
            !activeTab.isHome &&
            settings.showTopBar &&
            !isImeVisible

        // Top Floating iPhone-Style Transparent Liquid Glass Search & Omnibox Capsule
        AnimatedVisibility(
            visible = showTopCapsule,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .graphicsLayer {
                    scaleX = overlayDepthScale
                    scaleY = overlayDepthScale
                }
                .then(overlayBlurModifier)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 720.dp)
            ) {
                if (settings.showTopBar || isOmniboxFocused) {
                    TopLiquidOmnibox(
                        activeTab = activeTab,
                        omniboxValue = omniboxValue,
                        isFocused = isOmniboxFocused,
                        adBlockEnabled = settings.adBlockEnabled,
                        searchEngineName = settings.searchEngine.displayName,
                        onValueChange = { omniboxValue = it },
                        onFocusChanged = { focused ->
                            isOmniboxFocused = focused
                            if (focused) {
                                val currentText = if (activeTab.isHome) "" else activeTab.url
                                omniboxValue = TextFieldValue(
                                    text = currentText,
                                    selection = TextRange(0, currentText.length)
                                )
                            }
                        },
                        onSubmit = { submitOmnibox() },
                        onClear = { omniboxValue = TextFieldValue("") },
                        onShieldClick = {
                            if (activeTab.isHome) onOpenAdBlock() else onInspectSiteSecurity()
                        },
                        onReloadOrStop = onReloadOrStop,
                        onQrClick = onOpenQrScanner
                    )
                }

                AnimatedVisibility(visible = findInPage.isActive) {
                    FindInPageFloatingBar(
                        state = findInPage,
                        onQueryChange = onUpdateFindQuery,
                        onPrev = { onFindNext(false) },
                        onNext = { onFindNext(true) },
                        onClose = onCloseFindInPage,
                        modifier = Modifier.padding(top = if (settings.showTopBar || isOmniboxFocused) 8.dp else 0.dp)
                    )
                }
            }
        }

        // iOS 26 Safari Compact Address Pill while scrolling down full-screen pages (tap to restore bars)
        AnimatedVisibility(
            visible = showCompactScrollBadge,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .then(overlayBlurModifier)
        ) {
            IphoneLiquidGlassPill(
                accentGlow = activeTab.isIncognito,
                modifier = Modifier
                    .widthIn(min = 140.dp, max = 230.dp)
                    .clickable { expandCompactScrolledBars = true }
                    .testTag("compact_scrolled_address_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (activeTab.isIncognito) Icons.Rounded.PrivacyTip else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = if (activeTab.securityState == PageSecurityState.SECURE_HTTPS) MintSecure else glassTheme.accentStart,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = UrlResolver.displayHostOrTitle(activeTab.url, activeTab.title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom Floating iPhone (iOS 26 style) Transparent Liquid Glass Capsule Dock
        AnimatedVisibility(
            visible = showBottomCapsule,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .graphicsLayer {
                    scaleX = overlayDepthScale
                    scaleY = overlayDepthScale
                }
                .then(overlayBlurModifier)
        ) {
            IphoneFloatingLiquidDock(
                isHome = activeTab.isHome,
                tabCount = tabCount,
                isIncognito = activeTab.isIncognito,
                onHome = onHome,
                onNewTab = onNewTab,
                onTabsClick = onOpenTabSwitcher,
                onMenuClick = onOpenMenu
            )
        }

        // Minimal Translucent Corner Menu Handle when Bottom Dock is hidden OR compacted while scrolling down
        AnimatedVisibility(
            visible = !showBottomCapsule && !isFullscreenMode && fullscreenVideoView == null && !isImeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
                .then(overlayBlurModifier)
        ) {
            IphoneLiquidGlassPill(
                accentGlow = activeTab.isIncognito
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.MoreHoriz,
                    contentDescription = stringResource(R.string.cd_menu),
                    onClick = onOpenMenu,
                    testTagName = "floating_quick_menu_handle"
                )
            }
        }

        // Exit Fullscreen Floating Pill
        if (isFullscreenMode && fullscreenVideoView == null) {
            IphoneLiquidGlassPill(
                accentGlow = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(18.dp)
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.FullscreenExit,
                    contentDescription = stringResource(R.string.cd_fullscreen),
                    onClick = onExitFullscreen,
                    testTagName = "exit_fullscreen_button"
                )
            }
        }

        // Fullscreen HTML5 / YouTube Video Host
        if (fullscreenVideoView != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { container ->
                        if (fullscreenVideoView.parent !== container) {
                            (fullscreenVideoView.parent as? ViewGroup)?.removeView(fullscreenVideoView)
                            container.removeAllViews()
                            container.addView(
                                fullscreenVideoView,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IphoneLiquidGlassPill(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(14.dp)
                ) {
                    GlassIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Exit fullscreen video",
                        onClick = onExitFullscreenVideo
                    )
                }
            }
        }
    }
}

@Composable
private fun TopLiquidOmnibox(
    activeTab: BrowserTab,
    omniboxValue: TextFieldValue,
    isFocused: Boolean,
    adBlockEnabled: Boolean,
    searchEngineName: String,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onShieldClick: () -> Unit,
    onReloadOrStop: () -> Unit,
    onQrClick: () -> Unit
) {
    val glass = LocalNovaGlassColors.current
    IphoneLiquidGlassPill(
        accentGlow = isFocused || activeTab.isIncognito || activeTab.isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                val (secIcon, secTint) = when {
                    activeTab.isIncognito -> Icons.Rounded.PrivacyTip to glass.accentStart
                    activeTab.securityState == PageSecurityState.CERT_ERROR -> Icons.Rounded.Warning to CoralDanger
                    activeTab.securityState == PageSecurityState.INSECURE_HTTP -> Icons.Rounded.Warning to AmberWarning
                    activeTab.securityState == PageSecurityState.SECURE_HTTPS -> Icons.Rounded.Lock to MintSecure
                    else -> Icons.Rounded.Shield to if (adBlockEnabled) MintSecure else MaterialTheme.colorScheme.onSurfaceVariant
                }

                GlassIconButton(
                    icon = secIcon,
                    contentDescription = stringResource(R.string.cd_shield),
                    onClick = onShieldClick,
                    tint = secTint,
                    iconSize = 20.dp,
                    testTagName = "omnibox_shield_button"
                )

                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    if (!isFocused && activeTab.isHome) {
                        Text(
                            text = if (activeTab.isIncognito) "Private search or URL" else "Search $searchEngineName or type URL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (!isFocused && !activeTab.isHome) {
                        Text(
                            text = UrlResolver.displayHostOrTitle(activeTab.url, activeTab.title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = if (isFocused) omniboxValue else TextFieldValue(""),
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(glass.accentStart),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { onSubmit() },
                            onSearch = { onSubmit() },
                            onDone = { onSubmit() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                            .onFocusChanged { onFocusChanged(it.isFocused) }
                            .testTag("omnibox_input")
                    )
                }

                if (isFocused) {
                    if (omniboxValue.text.isNotEmpty()) {
                        GlassIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            onClick = onClear,
                            iconSize = 18.dp,
                            testTagName = "omnibox_clear_button"
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        onClick = onSubmit,
                        active = true,
                        iconSize = 20.dp,
                        testTagName = "omnibox_search_button"
                    )
                } else if (activeTab.isHome) {
                    GlassIconButton(
                        icon = Icons.Rounded.QrCodeScanner,
                        contentDescription = stringResource(R.string.cd_qr_scan),
                        onClick = onQrClick,
                        tint = glass.accentStart,
                        iconSize = 20.dp,
                        testTagName = "omnibox_qr_button"
                    )
                } else {
                    GlassIconButton(
                        icon = if (activeTab.isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                        contentDescription = if (activeTab.isLoading) stringResource(R.string.cd_stop) else stringResource(R.string.cd_refresh),
                        onClick = onReloadOrStop,
                        iconSize = 20.dp,
                        testTagName = "omnibox_reload_button"
                    )
                }
            }

            if (activeTab.isLoading && activeTab.progress in 1..99) {
                LiquidProgressBar(
                    progress = activeTab.progress / 100f,
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .testTag("omnibox_progress")
                )
            }
        }
    }
}

@Composable
private fun FindInPageFloatingBar(
    state: FindInPageState,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    LiquidGlassSurface(
        shape = RoundedCornerShape(22.dp),
        strong = true,
        accentBorder = true,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (state.query.isEmpty()) {
                    Text(
                        text = "Find in page...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(glass.accentStart),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("find_in_page_input")
                )
            }
            if (state.query.isNotEmpty()) {
                Text(
                    text = "${state.activeMatchIndex}/${state.totalMatches}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            GlassIconButton(icon = Icons.Rounded.KeyboardArrowUp, contentDescription = "Previous match", onClick = onPrev, iconSize = 20.dp)
            GlassIconButton(icon = Icons.Rounded.KeyboardArrowDown, contentDescription = "Next match", onClick = onNext, iconSize = 20.dp)
            GlassIconButton(icon = Icons.Rounded.Close, contentDescription = "Close find in page", onClick = onClose, iconSize = 18.dp)
        }
    }
}

/**
 * Compact floating iPhone Liquid Glass bottom dock:
 * Removes the bottom back button completely (system back gesture handles site-first back),
 * leaving a crystal-clear floating glass capsule with Home, New Tab, Tabs badge, and Menu.
 */
@Composable
private fun IphoneFloatingLiquidDock(
    isHome: Boolean,
    tabCount: Int,
    isIncognito: Boolean,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val density = LocalDensity.current
    var homeSlotBounds by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var tabsSlotBounds by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    val targetBounds = if (isHome) homeSlotBounds else tabsSlotBounds

    val lensX by animateFloatAsState(
        targetValue = targetBounds?.first ?: 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "dock_liquid_lens_x"
    )
    val lensW by animateFloatAsState(
        targetValue = targetBounds?.second ?: 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 340f),
        label = "dock_liquid_lens_w"
    )
    val travelDist = if (targetBounds != null) abs(targetBounds.first - lensX) else 0f
    val stretchPx = (travelDist * 0.22f).coerceIn(0f, with(density) { 18.dp.toPx() })
    val stretchLeft = if (targetBounds != null && targetBounds.first < lensX) stretchPx * 0.5f else 0f
    val lensHeightLift = 1f + (stretchPx / with(density) { 80.dp.toPx() }).coerceIn(0f, 0.08f)

    IphoneLiquidGlassPill(
        accentGlow = isIncognito,
        modifier = Modifier
            .widthIn(min = 248.dp, max = 320.dp)
            .testTag("iphone_liquid_glass_dock")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            if (lensW > 2f) {
                val lensWidthDp = with(density) { (lensW + stretchPx).toDp() }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset { IntOffset((lensX - stretchLeft).roundToInt(), 0) }
                            .size(width = lensWidthDp, height = 44.dp)
                            .graphicsLayer { scaleY = lensHeightLift }
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        glass.accentStart.copy(alpha = if (glass.isDark) 0.28f else 0.18f),
                                        glass.accentEnd.copy(alpha = if (glass.isDark) 0.20f else 0.14f)
                                    )
                                )
                            )
                            .border(
                                width = 0.85.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (glass.isDark) 0.34f else 0.82f),
                                        glass.accentStart.copy(alpha = 0.42f)
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.Home,
                    contentDescription = stringResource(R.string.cd_home),
                    onClick = onHome,
                    active = isHome,
                    testTagName = "nav_home_button",
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val p = coords.positionInParent()
                        homeSlotBounds = p.x to coords.size.width.toFloat()
                    }
                )

                GlassIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_new_tab),
                    onClick = onNewTab,
                    testTagName = "nav_new_tab_button"
                )

                // Liquid Glass Tab Counter Pill Button with bounce + Sakuga spark on tap
                val sakugaVfx = LocalSakugaVfx.current
                var tabsBtnCenter by remember { mutableStateOf(Offset.Zero) }
                val badgeScale = remember { Animatable(1f) }
                LaunchedEffect(tabCount) {
                    badgeScale.snapTo(1.26f)
                    badgeScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .testTag("nav_tabs_button")
                        .onGloballyPositioned { coords ->
                            tabsBtnCenter = coords.boundsInRoot().center
                            val p = coords.positionInParent()
                            tabsSlotBounds = p.x to coords.size.width.toFloat()
                        }
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = {
                            sakugaVfx?.triggerMicroSpark(tabsBtnCenter)
                            onTabsClick()
                        })
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size((28f * badgeScale.value).dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        glass.accentStart.copy(alpha = 0.14f),
                                        glass.accentEnd.copy(alpha = 0.14f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        glass.accentStart.copy(alpha = 0.55f),
                                        glass.accentEnd.copy(alpha = 0.45f)
                                    )
                                ),
                                shape = RoundedCornerShape(9.dp)
                            )
                    ) {
                        Text(
                            text = if (tabCount > 99) "99" else tabCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                GlassIconButton(
                    icon = Icons.Rounded.MoreHoriz,
                    contentDescription = stringResource(R.string.cd_menu),
                    onClick = onMenuClick,
                    testTagName = "nav_menu_button"
                )
            }
        }
    }
}
