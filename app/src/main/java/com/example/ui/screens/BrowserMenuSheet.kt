package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ChromeReaderMode
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.AddToHomeScreen
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import com.example.viewmodel.BrowserTab

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
    onOpenSettings: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val glass = LocalNovaGlassColors.current
    val backdropInteraction = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (glass.isDark) 0.42f else 0.28f))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss
            )
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            strong = true,
            accentBorder = true,
            elevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
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

                // Quick System / Dark / Light Liquid Glass Theme Switcher Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassPillChip(
                        label = "System",
                        icon = Icons.Rounded.BrightnessAuto,
                        active = themeMode == AppThemeMode.SYSTEM,
                        onClick = { onUpdateTheme(AppThemeMode.SYSTEM) },
                        testTagName = "menu_theme_system",
                        modifier = Modifier.weight(1f)
                    )
                    GlassPillChip(
                        label = "Dark",
                        icon = Icons.Rounded.DarkMode,
                        active = themeMode == AppThemeMode.DARK,
                        onClick = { onUpdateTheme(AppThemeMode.DARK) },
                        testTagName = "menu_theme_dark",
                        modifier = Modifier.weight(1f)
                    )
                    GlassPillChip(
                        label = "Light",
                        icon = Icons.Rounded.LightMode,
                        active = themeMode == AppThemeMode.LIGHT,
                        onClick = { onUpdateTheme(AppThemeMode.LIGHT) },
                        testTagName = "menu_theme_light",
                        modifier = Modifier.weight(1f)
                    )
                }

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
                    MenuTileData("Install Site", Icons.Rounded.AddToHomeScreen, !activeTab.isHome, "menu_install_site") { onDismiss(); onInstallWebApp() },
                    MenuTileData("QR Scanner", Icons.Rounded.QrCodeScanner, false, "menu_qr_tile") { onOpenQrScanner() },
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menu_tile_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .testTag(data.testTag)
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
            }
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (data.highlighted) glass.chipActiveBg
                else glass.glassSurface.copy(alpha = (glass.glassSurface.alpha * 0.85f))
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = glass.accentStart),
                onClick = {
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
