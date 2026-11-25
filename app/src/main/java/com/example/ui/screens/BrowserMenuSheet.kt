package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ChromeReaderMode
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.UserAccountEntity
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.LiquidGlassSegmentedTabs
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidSegmentTab
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.components.consumeAllTouches
import com.example.ui.components.sakugaSquashScale
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import com.example.viewmodel.BrowserTab
import kotlinx.coroutines.launch

@Composable
fun BrowserMenuOverlay(
    activeTab: BrowserTab,
    tabCount: Int,
    themeMode: AppThemeMode,
    autoDarkWebsites: Boolean,
    showTopBar: Boolean,
    showBottomBar: Boolean,
    isBookmarked: Boolean,
    adBlockEnabled: Boolean,
    blockedCount: Int,
    isFullscreen: Boolean,
    onDismiss: () -> Unit,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onToggleAutoDarkWebsites: () -> Unit,
    onToggleShowTopBar: () -> Unit,
    onToggleShowBottomBar: () -> Unit,
    onGoHome: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onForward: () -> Unit,
    onReloadOrStop: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAdBlock: () -> Unit,
    onOpenVpn: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenFindInPage: () -> Unit,
    onOpenReadingMode: () -> Unit,
    onTranslatePage: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onInstallWebApp: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenSettings: () -> Unit,
    userAccount: UserAccountEntity? = null,
    googleWebSignedIn: Boolean = false,
    onOpenReviews: () -> Unit = {},
    onOpenAccount: () -> Unit = {}
) {
    val glass = LocalNovaGlassColors.current
    val haptic = LocalHapticFeedback.current
    val sakugaVfx = LocalSakugaVfx.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val backdropInteraction = remember { MutableInteractionSource() }
    val enterAnim = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(900f) }
    var isDraggingHandle by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    var handleCenter by remember { mutableStateOf(Offset.Zero) }

    fun dismissAnimated() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            sakugaVfx?.triggerBottomTapWave(handleCenter)
            val exitTarget = (sheetHeightPx + 120f).coerceAtLeast(600f)
            dragOffsetY.animateTo(
                targetValue = exitTarget,
                animationSpec = tween(durationMillis = 190)
            )
            onDismiss()
        }
    }

    BackHandler { dismissAnimated() }

    LaunchedEffect(Unit) {
        enterAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.80f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    val settleProgress = enterAnim.value.coerceIn(0f, 1f)
    val sheetRevealBlur = ((1f - settleProgress) * 6f).dp
    val dragProgress = (dragOffsetY.value / sheetHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val contentScale = (0.96f + 0.04f * settleProgress) * (1f - dragProgress * 0.03f)
    val entrySlidePx = (1f - settleProgress) * with(density) { 48.dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        // Reserve clear clickable top space below status bar/notch so tapping above the sheet cleanly goes back
        val maxSheetHeight = (maxHeight * 0.76f).coerceAtMost(maxHeight - 104.dp).coerceAtLeast(280.dp)

        // Full-screen touch consumer & tap-to-close backdrop behind the sheet
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeAllTouches()
                .clickable(
                    interactionSource = backdropInteraction,
                    indication = null,
                    onClick = { dismissAnimated() }
                )
        )

        LiquidGlassSurface(
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            strong = true,
            accentBorder = true,
            elevation = 20.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .statusBarsPadding()
                .padding(top = 84.dp)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .heightIn(max = maxSheetHeight)
                .onGloballyPositioned { coords ->
                    sheetHeightPx = coords.size.height.toFloat().coerceAtLeast(300f)
                }
                .graphicsLayer {
                    translationY = entrySlidePx + dragOffsetY.value
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = (1f - dragProgress * 0.28f).coerceIn(0.45f, 1f)
                }
                .then(
                    if (sheetRevealBlur > 0.4.dp) {
                        Modifier.blur(radius = sheetRevealBlur, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                // Interactive pull-down notch/handle strip with live drag-follow & spring dismiss animation
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .onGloballyPositioned { handleCenter = it.boundsInRoot().center }
                        .pointerInput(Unit) {
                            val dismissThresholdPx = with(density) { 105.dp.toPx() }
                            var totalDrag = 0f
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDraggingHandle = true
                                    totalDrag = dragOffsetY.value
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag = (totalDrag + dragAmount).coerceAtLeast(0f)
                                    scope.launch { dragOffsetY.snapTo(totalDrag) }
                                },
                                onDragEnd = {
                                    isDraggingHandle = false
                                    if (totalDrag > dismissThresholdPx) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        dismissAnimated()
                                    } else {
                                        scope.launch {
                                            dragOffsetY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isDraggingHandle = false
                                    scope.launch {
                                        dragOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                        )
                                    }
                                }
                            )
                        }
                        .testTag("menu_pull_handle")
                ) {
                    val handleWidth by animateFloatAsState(
                        targetValue = if (isDraggingHandle) 62f else 46f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "handle_width"
                    )
                    Box(
                        modifier = Modifier
                            .width(handleWidth.dp)
                            .height(if (isDraggingHandle) 5.5.dp else 4.5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isDraggingHandle) {
                                    glass.accentStart.copy(alpha = 0.78f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                                }
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {

                // User Account Banner at top of menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(glass.glassSurfaceStrong)
                        .border(0.8.dp, glass.glassBorder, RoundedCornerShape(16.dp))
                        .clickable { onDismiss(); onOpenAccount() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("menu_account_banner")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (userAccount != null) {
                                        runCatching {
                                            val c = userAccount.avatarColorHex.removePrefix("#").toLong(16)
                                            Color(c or 0x00000000FF000000)
                                        }.getOrDefault(glass.accentStart)
                                    } else glass.glassBorderStrong
                                )
                        ) {
                            if (userAccount != null) {
                                Text(
                                    text = userAccount.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = userAccount?.displayName ?: "Nova Account",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = when {
                                    userAccount != null && (googleWebSignedIn || userAccount.googleConnected) ->
                                        "Google active across sites · Tap to manage"
                                    userAccount != null ->
                                        "Logged in & Synced · Tap to view"
                                    googleWebSignedIn ->
                                        "Google Web active · Tap to link profile"
                                    else ->
                                        "Sign in with Google or create account"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (userAccount != null) "Manage" else "Sign In",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = glass.accentStart
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Top quick strip: Forward, Refresh, Bookmark, Share, Settings
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = stringResource(R.string.cd_forward),
                        onClick = { onDismiss(); onForward() },
                        enabled = activeTab.canGoForward,
                        testTagName = "menu_forward"
                    )
                    GlassIconButton(
                        icon = if (activeTab.isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh),
                        onClick = { onDismiss(); onReloadOrStop() },
                        enabled = !activeTab.isHome,
                        testTagName = "menu_refresh"
                    )
                    GlassIconButton(
                        icon = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = stringResource(R.string.cd_bookmark),
                        onClick = { onToggleBookmark(); onDismiss() },
                        active = isBookmarked,
                        enabled = !activeTab.isHome,
                        testTagName = "menu_bookmark_toggle"
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.cd_share),
                        onClick = { onDismiss(); onShare() },
                        enabled = !activeTab.isHome,
                        testTagName = "menu_share"
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        onClick = onOpenSettings,
                        tint = glass.accentStart,
                        testTagName = "menu_settings_quick"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick System / Dark / Light iOS 26 Gliding Liquid Lens Theme Switcher
                LiquidGlassSegmentedTabs(
                    tabs = listOf(
                        LiquidSegmentTab("SYSTEM", "System", Icons.Rounded.BrightnessAuto, "menu_theme_system"),
                        LiquidSegmentTab("DARK", "Dark", Icons.Rounded.DarkMode, "menu_theme_dark"),
                        LiquidSegmentTab("LIGHT", "Light", Icons.Rounded.LightMode, "menu_theme_light")
                    ),
                    selectedId = themeMode.name,
                    onSelect = { selected ->
                        onUpdateTheme(AppThemeMode.valueOf(selected))
                    },
                    accentGlow = true,
                    useThemeSplash = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Independent Top Bar & Bottom Bar Show/Hide Switcher
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassPillChip(
                        label = if (showTopBar) "Top Bar: On" else "Top Bar: Off",
                        icon = Icons.Rounded.VerticalAlignTop,
                        active = showTopBar,
                        onClick = onToggleShowTopBar,
                        testTagName = "menu_toggle_top_bar",
                        modifier = Modifier.weight(1f)
                    )
                    GlassPillChip(
                        label = if (showBottomBar) "Bottom Bar: On" else "Bottom Bar: Off",
                        icon = Icons.Rounded.VerticalAlignBottom,
                        active = showBottomBar,
                        onClick = onToggleShowBottomBar,
                        testTagName = "menu_toggle_bottom_bar",
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Grid of Command Tiles (3 per row)
                val items = listOf(
                    MenuTileData("Home", Icons.Rounded.Home, activeTab.isHome, "menu_home") { onDismiss(); onGoHome() },
                    MenuTileData("Tabs ($tabCount)", Icons.Rounded.Layers, false, "menu_tabs") { onOpenTabSwitcher() },
                    MenuTileData("New Tab", Icons.Rounded.AddBox, false, "menu_new_tab") { onNewTab() },
                    MenuTileData("Incognito", Icons.Rounded.PrivacyTip, activeTab.isIncognito, "menu_incognito") { onNewIncognitoTab() },
                    MenuTileData("Dark Sites", Icons.Rounded.DarkMode, autoDarkWebsites, "menu_dark_sites") { onToggleAutoDarkWebsites() },
                    MenuTileData("Bookmarks", Icons.Rounded.Bookmarks, false, "menu_bookmarks") { onOpenBookmarks() },
                    MenuTileData("History", Icons.Rounded.History, false, "menu_history") { onOpenHistory() },
                    MenuTileData("Downloads", Icons.Rounded.Download, false, "menu_downloads") { onOpenDownloads() },
                    MenuTileData("Shield ($blockedCount)", Icons.Rounded.Shield, adBlockEnabled, "menu_adblock") { onOpenAdBlock() },
                    MenuTileData("VPN", Icons.Rounded.VpnLock, false, "menu_vpn") { onOpenVpn() },
                    MenuTileData("Quick Notes", Icons.Rounded.EditNote, false, "menu_notes") { onOpenNotes() },
                    MenuTileData("Find in Page", Icons.Rounded.FindInPage, false, "menu_find") { onOpenFindInPage() },
                    MenuTileData("Reader Mode", Icons.AutoMirrored.Rounded.ChromeReaderMode, false, "menu_reader") { onOpenReadingMode() },
                    MenuTileData("Translate", Icons.Rounded.Translate, false, "menu_translate") { onTranslatePage() },
                    MenuTileData("Desktop Site", Icons.Rounded.DesktopWindows, activeTab.desktopMode, "menu_desktop") { onDismiss(); onToggleDesktopMode() },
                    MenuTileData(if (isFullscreen) "Exit Full" else "Fullscreen", if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, isFullscreen, "menu_fullscreen") { onDismiss(); onToggleFullscreen() },
                    MenuTileData("Install Site", Icons.AutoMirrored.Rounded.AddToHomeScreen, !activeTab.isHome, "menu_install_site") { onDismiss(); onInstallWebApp() },
                    MenuTileData("QR Scanner", Icons.Rounded.QrCodeScanner, false, "menu_qr_tile") { onOpenQrScanner() },
                    MenuTileData("Reviews", Icons.Rounded.Star, false, "menu_reviews") { onDismiss(); onOpenReviews() },
                    MenuTileData("Account", Icons.Rounded.AccountCircle, userAccount != null, "menu_account") { onDismiss(); onOpenAccount() },
                    MenuTileData("Settings", Icons.Rounded.Settings, false, "menu_settings") { onOpenSettings() }
                )

                items.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        rowItems.forEach { tile ->
                            MenuActionTile(
                                data = tile,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        }
    }
}

private data class MenuTileData(
    val label: String,
    val icon: ImageVector,
    val highlighted: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun MenuActionTile(
    data: MenuTileData,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    val haptic = LocalHapticFeedback.current
    val sakugaVfx = LocalSakugaVfx.current
    var tileCenter by remember { mutableStateOf(Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed) 0.89f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menu_tile_scale"
    )
    val (tileSquashX, tileSquashY) = sakugaSquashScale(tileScale, wideFactor = 0.48f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .testTag(data.testTag)
            .onGloballyPositioned { tileCenter = it.boundsInRoot().center }
            .graphicsLayer {
                scaleX = tileSquashX
                scaleY = tileSquashY
            }
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (data.highlighted) glass.chipActiveBg
                else glass.glassSurface.copy(alpha = (glass.glassSurface.alpha * 0.90f))
            )
            .border(
                width = 0.7.dp,
                color = if (data.highlighted) {
                    glass.accentStart.copy(alpha = 0.48f)
                } else {
                    glass.glassBorder.copy(alpha = if (glass.isDark) 0.42f else 0.72f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = glass.accentStart),
                onClick = {
                    if (data.testTag == "menu_dark_sites") {
                        sakugaVfx?.triggerThemeSplash(tileCenter)
                    } else {
                        sakugaVfx?.triggerChipAccent(tileCenter)
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    data.onClick()
                }
            )
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = data.icon,
            contentDescription = data.label,
            tint = if (data.highlighted) {
                if (data.testTag == "menu_adblock") MintSecure else glass.accentStart
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = data.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
