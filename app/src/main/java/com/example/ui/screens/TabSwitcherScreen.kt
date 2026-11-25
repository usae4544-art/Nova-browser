package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.engine.PageSecurityState
import com.example.engine.UrlResolver
import com.example.ui.components.GlassIconButton
import com.example.ui.components.IphoneLiquidGlassPill
import com.example.ui.components.LiquidGlassSegmentedTabs
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidSegmentTab
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.components.NovaWallpaperBackground
import com.example.ui.components.consumeAllTouches
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.IncognitoAccent
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import com.example.viewmodel.BrowserTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TabSwitcherScreen(
    tabs: List<BrowserTab>,
    activeTabId: String,
    incognitoLockEnabled: Boolean,
    incognitoUnlocked: Boolean,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: (isIncognito: Boolean) -> Unit,
    onCloseAll: (incognitoOnly: Boolean) -> Unit,
    onRequestUnlockIncognito: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    var showIncognitoSection by remember(activeTab?.isIncognito) {
        mutableStateOf(activeTab?.isIncognito == true)
    }
    var pendingPrivateSwitch by remember { mutableStateOf(false) }

    LaunchedEffect(incognitoUnlocked) {
        if (incognitoUnlocked && pendingPrivateSwitch) {
            showIncognitoSection = true
            pendingPrivateSwitch = false
        }
    }

    val normalTabs = remember(tabs) { tabs.filter { !it.isIncognito } }
    val incognitoTabs = remember(tabs) { tabs.filter { it.isIncognito } }
    val displayedTabs = if (showIncognitoSection) incognitoTabs else normalTabs
    val glass = LocalNovaGlassColors.current
    val sakugaVfx = LocalSakugaVfx.current
    var closeAllBtnCenter by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        NovaWallpaperBackground(isIncognito = showIncognitoSection)
        Box(modifier = Modifier.matchParentSize().consumeAllTouches())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar with iOS 26 Gliding Liquid Lens Segmented Control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                LiquidGlassSegmentedTabs(
                    tabs = listOf(
                        LiquidSegmentTab("NORMAL", "Tabs (${normalTabs.size})", Icons.Rounded.Language, "tab_filter_normal"),
                        LiquidSegmentTab("PRIVATE", "Private (${incognitoTabs.size})", Icons.Rounded.PrivacyTip, "tab_filter_incognito")
                    ),
                    selectedId = if (showIncognitoSection) "PRIVATE" else "NORMAL",
                    onSelect = { selected ->
                        if (selected == "NORMAL") {
                            showIncognitoSection = false
                        } else {
                            if (incognitoLockEnabled && !incognitoUnlocked) {
                                pendingPrivateSwitch = true
                                onRequestUnlockIncognito()
                            } else {
                                showIncognitoSection = true
                            }
                        }
                    },
                    accentGlow = showIncognitoSection,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (displayedTabs.isNotEmpty()) {
                        GlassIconButton(
                            icon = if (showIncognitoSection) Icons.Rounded.LocalFireDepartment else Icons.Rounded.DeleteSweep,
                            contentDescription = if (showIncognitoSection) "Burn all private tabs" else "Close all tabs in group",
                            tint = if (showIncognitoSection) CoralDanger else null,
                            onClick = {
                                if (showIncognitoSection) {
                                    sakugaVfx?.triggerImpactBurst(closeAllBtnCenter)
                                }
                                onCloseAll(showIncognitoSection)
                            },
                            modifier = Modifier.onGloballyPositioned { closeAllBtnCenter = it.boundsInRoot().center },
                            testTagName = "close_all_tabs_button"
                        )
                    }
                    GlassIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        onClick = onDismiss,
                        testTagName = "tab_switcher_close"
                    )
                }
            }

            if (displayedTabs.isNotEmpty()) {
                Text(
                    text = "Swipe any tab left or right to close",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            // Tab Grid with iOS-style directional horizontal spring slide + scale transition
            AnimatedContent(
                targetState = showIncognitoSection,
                transitionSpec = {
                    val dir = if (targetState) 1 else -1
                    (slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                    ) { dir * it / 3 } +
                        fadeIn(tween(210)) +
                        scaleIn(initialScale = 0.96f, animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(tween(190)) { -dir * it / 3 } +
                                fadeOut(tween(160)) +
                                scaleOut(targetScale = 0.96f, animationSpec = tween(180))
                        )
                },
                label = "tab_section_switch",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { isPrivateTabView ->
                val sectionTabs = if (isPrivateTabView) incognitoTabs else normalTabs
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = if (maxWidth >= 600.dp) 3 else 2

                    if (isPrivateTabView && incognitoLockEnabled && !incognitoUnlocked) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LiquidGlassSurface(
                                strong = true,
                                accentBorder = true,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = IncognitoAccent,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Private Vault Locked",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Enter your NOVA PIN to view private tabs.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = onRequestUnlockIncognito) {
                                        Text("Unlock Vault")
                                    }
                                }
                            }
                        }
                    } else if (sectionTabs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isPrivateTabView) Icons.Rounded.PrivacyTip else Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = if (isPrivateTabView) IncognitoAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isPrivateTabView) "No Stealth Tabs Open" else "No Tabs Open",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onNewTab(isPrivateTabView) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPrivateTabView) IncognitoAccent else glass.accentStart
                                    ),
                                    modifier = Modifier.testTag("empty_new_tab_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isPrivateTabView) "Open Stealth Tab" else "Open New Tab")
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sectionTabs, key = { it.id }) { tab ->
                                AnimatedTabPreviewCard(
                                    tab = tab,
                                    isActive = tab.id == activeTabId,
                                    onSelect = { onSelectTab(tab.id) },
                                    onClose = { onCloseTab(tab.id) },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(220),
                                        fadeOutSpec = tween(180),
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Floating iPhone Liquid Glass Action Bar
            IphoneLiquidGlassPill(
                accentGlow = showIncognitoSection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = { onNewTab(false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = glass.accentStart,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("new_tab_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Tab", maxLines = 1)
                    }

                    Button(
                        onClick = { onNewTab(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IncognitoAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("new_incognito_tab_button")
                    ) {
                        Icon(Icons.Rounded.PrivacyTip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stealth", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedTabPreviewCard(
    tab: BrowserTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    val scope = rememberCoroutineScope()

    var isClosing by remember { mutableStateOf(false) }
    val dragOffsetX = remember { Animatable(0f) }

    val closeScale by animateFloatAsState(
        targetValue = if (isClosing) 0.72f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "tab_close_scale"
    )
    val closeAlpha by animateFloatAsState(
        targetValue = if (isClosing) 0f else (1f - (abs(dragOffsetX.value) / 360f)).coerceIn(0.25f, 1f),
        animationSpec = tween(210),
        label = "tab_close_alpha"
    )
    val closeRotation by animateFloatAsState(
        targetValue = if (isClosing) -7f else (dragOffsetX.value / 28f).coerceIn(-12f, 12f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tab_close_rotation"
    )

    fun triggerAnimatedClose(flingDirection: Float = 0f) {
        if (isClosing) return
        isClosing = true
        scope.launch {
            if (flingDirection != 0f) {
                dragOffsetX.animateTo(
                    targetValue = flingDirection * 420f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
            } else {
                delay(210L)
            }
            onClose()
        }
    }

    val dragBlurPx = if (isClosing) 14f else ((abs(dragOffsetX.value) - 22f).coerceAtLeast(0f) / 14f).coerceAtMost(12f)

    LiquidGlassSurface(
        shape = RoundedCornerShape(22.dp),
        strong = isActive,
        accentBorder = isActive,
        elevation = if (isActive) 12.dp else 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .zIndex(if (abs(dragOffsetX.value) > 2f || isClosing) 2f else 0f)
            .graphicsLayer {
                translationX = dragOffsetX.value
                scaleX = closeScale
                scaleY = closeScale
                rotationZ = closeRotation
                alpha = closeAlpha
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dragBlurPx > 0.5f) {
                    renderEffect = BlurEffect(dragBlurPx, dragBlurPx, TileMode.Decal)
                }
            }
            .pointerInput(tab.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(dragOffsetX.value) > 120f) {
                            triggerAnimatedClose(if (dragOffsetX.value > 0f) 1f else -1f)
                        } else {
                            scope.launch {
                                dragOffsetX.animateTo(
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
                        scope.launch {
                            dragOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                        }
                    }
                )
            }
            .testTag("tab_card_${tab.id}")
            .clickable(enabled = !isClosing, onClick = onSelect)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Card Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                if (tab.isIncognito) IncognitoAccent.copy(alpha = 0.30f) else glass.accentStart.copy(alpha = 0.18f),
                                if (tab.isIncognito) Color(0xFFEC4899).copy(alpha = 0.12f) else glass.accentEnd.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Icon(
                    imageVector = when {
                        tab.isIncognito -> Icons.Rounded.PrivacyTip
                        tab.securityState == PageSecurityState.SECURE_HTTPS -> Icons.Rounded.Lock
                        else -> Icons.Rounded.Language
                    },
                    contentDescription = null,
                    tint = when {
                        tab.isIncognito -> IncognitoAccent
                        tab.securityState == PageSecurityState.SECURE_HTTPS -> MintSecure
                        else -> glass.accentStart
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tab.title.ifBlank { if (tab.isIncognito) "Stealth Home" else "NOVA Home" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_close_tab),
                    onClick = { triggerAnimatedClose() },
                    iconSize = 16.dp,
                    testTagName = "close_tab_${tab.id}"
                )
            }

            // Card Body Preview
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (tab.isIncognito) IncognitoAccent.copy(alpha = 0.24f)
                            else glass.accentStart.copy(alpha = 0.18f)
                        )
                ) {
                    Text(
                        text = tab.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "N",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (tab.isIncognito) IncognitoAccent else glass.accentStart
                    )
                }

                Column {
                    Text(
                        text = UrlResolver.displayHostOrTitle(tab.url, tab.title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (tab.isIncognito) "Active Stealth Tab" else "Active Tab",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tab.isIncognito) IncognitoAccent else glass.accentStart
                        )
                    }
                }
            }
        }
    }
}
