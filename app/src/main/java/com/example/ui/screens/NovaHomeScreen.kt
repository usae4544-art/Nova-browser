package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.components.sakugaSquashScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ShortcutEntity
import com.example.data.preferences.SearchEngine
import com.example.engine.AdBlockStats
import com.example.engine.ChromiumEngineInfo
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.IphoneLiquidGlassPill
import com.example.ui.components.LiquidEdgeBlurLazyRow
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.IncognitoAccent
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import com.example.vpn.VpnConnectionStatus
import com.example.vpn.VpnRuntimeState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovaHomeScreen(
    isIncognito: Boolean,
    searchEngine: SearchEngine,
    shortcuts: List<ShortcutEntity>,
    adBlockEnabled: Boolean,
    adBlockStats: AdBlockStats,
    vpnState: VpnRuntimeState,
    chromiumInfo: ChromiumEngineInfo,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onNavigate: (String) -> Unit,
    onAddShortcut: (title: String, url: String) -> Unit,
    onDeleteShortcut: (Long) -> Unit,
    onOpenIncognitoTab: () -> Unit,
    onBurnIncognitoSession: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenAdBlock: () -> Unit,
    onOpenVpn: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenReviews: () -> Unit = {},
    onOpenAccount: () -> Unit = {}
) {
    val glass = LocalNovaGlassColors.current
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var homeQuery by remember { mutableStateOf("") }
    var showAddShortcutDialog by remember { mutableStateOf(false) }
    var shortcutToDelete by remember { mutableStateOf<ShortcutEntity?>(null) }

    // Breathing luminous pulse for Stealth / NOVA emblem
    val infiniteTransition = rememberInfiniteTransition(label = "emblem_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem_pulse_scale"
    )

    fun submitHomeSearch() {
        val q = homeQuery.trim()
        if (q.isNotEmpty()) {
            keyboardController?.hide()
            focusManager.clearFocus()
            onNavigate(q)
            homeQuery = ""
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(scrollState)
                .padding(
                    top = topContentPadding + 18.dp,
                    bottom = bottomContentPadding + 24.dp
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
            // Soft Breathing Emblem
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    (if (isIncognito) IncognitoAccent else glass.accentStart).copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    glass.glassSurface,
                                    glass.glassSurfaceStrong
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    glass.accentStart.copy(alpha = 0.35f),
                                    glass.accentEnd.copy(alpha = 0.30f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    if (isIncognito) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = stringResource(R.string.cd_nova_logo),
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_nova_icon_fg),
                            contentDescription = stringResource(R.string.cd_nova_logo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(66.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isIncognito) "STEALTH VAULT" else "NOVA",
                style = MaterialTheme.typography.displayMedium.copy(
                    brush = Brush.horizontalGradient(
                        listOf(glass.accentStart, MaterialTheme.colorScheme.onBackground, glass.accentEnd)
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Live Shield / Incognito Status Pill & Community Reviews Pill
            val sakugaVfx = LocalSakugaVfx.current
            var shieldPillCenter by remember { mutableStateOf(Offset.Zero) }
            var reviewsPillCenter by remember { mutableStateOf(Offset.Zero) }
            var homeSearchCenter by remember { mutableStateOf(Offset.Zero) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                IphoneLiquidGlassPill(
                    accentGlow = adBlockEnabled || isIncognito,
                    modifier = Modifier
                        .testTag("home_shield_pill")
                        .onGloballyPositioned { shieldPillCenter = it.boundsInRoot().center }
                        .clickable(onClick = {
                            sakugaVfx?.triggerChipAccent(shieldPillCenter)
                            onOpenAdBlock()
                        })
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isIncognito) Icons.Rounded.PrivacyTip else Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = if (isIncognito) IncognitoAccent else if (adBlockEnabled) MintSecure else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isIncognito) {
                                "Stealth · ${adBlockStats.totalBlocked}"
                            } else if (adBlockEnabled) {
                                "Shield · ${adBlockStats.totalBlocked}"
                            } else {
                                "Ad Shield Off"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IphoneLiquidGlassPill(
                    accentGlow = true,
                    modifier = Modifier
                        .testTag("home_reviews_pill")
                        .onGloballyPositioned { reviewsPillCenter = it.boundsInRoot().center }
                        .clickable(onClick = {
                            sakugaVfx?.triggerChipAccent(reviewsPillCenter)
                            onOpenReviews()
                        })
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "4.9 ★ Reviews",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary iPhone Liquid Glass Search Capsule on Home
            IphoneLiquidGlassPill(
                accentGlow = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .padding(start = 16.dp, end = 6.dp, top = 3.dp, bottom = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = glass.accentStart,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (homeQuery.isEmpty()) {
                            Text(
                                text = if (isIncognito) {
                                    "Search privately via ${searchEngine.displayName}"
                                } else {
                                    "Search ${searchEngine.displayName} or type URL"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = homeQuery,
                            onValueChange = { next ->
                                homeQuery = next
                                sakugaVfx?.triggerTypewriterKeyAccent(homeSearchCenter)
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(glass.accentStart),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { submitHomeSearch() },
                                onGo = { submitHomeSearch() },
                                onDone = { submitHomeSearch() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { homeSearchCenter = it.boundsInRoot().center }
                                .testTag("home_search_input")
                        )
                    }

                    if (homeQuery.isNotEmpty()) {
                        GlassIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            onClick = { homeQuery = "" },
                            iconSize = 18.dp,
                            testTagName = "home_search_clear"
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            onClick = { submitHomeSearch() },
                            active = true,
                            testTagName = "home_search_submit"
                        )
                    } else {
                        GlassIconButton(
                            icon = Icons.Rounded.QrCodeScanner,
                            contentDescription = stringResource(R.string.cd_qr_scan),
                            onClick = onOpenQrScanner,
                            tint = glass.accentStart,
                            testTagName = "home_qr_button"
                        )
                    }
                }
            }

            // Enhanced Incognito Stealth Protection Deck
            if (isIncognito) {
                Spacer(modifier = Modifier.height(18.dp))
                LiquidGlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    strong = true,
                    accentBorder = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Private Stealth Protections",
                                style = MaterialTheme.typography.titleSmall,
                                color = IncognitoAccent
                            )
                            Text(
                                text = "ZERO TRACE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintSecure
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StealthBadgeCell(Icons.Rounded.History, "No History", Modifier.weight(1f))
                            StealthBadgeCell(Icons.Rounded.Cookie, "Cookie Wipe", Modifier.weight(1f))
                            StealthBadgeCell(Icons.Rounded.Shield, "Ad Shield", Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var burnBtnCenter by remember { mutableStateOf(Offset.Zero) }
                        var burnBtnWidth by remember { mutableStateOf(0f) }
                        Button(
                            onClick = {
                                sakugaVfx?.triggerImpactBurst(burnBtnCenter, burnBtnWidth)
                                onBurnIncognitoSession()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralDanger.copy(alpha = 0.88f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .onGloballyPositioned {
                                    val b = it.boundsInRoot()
                                    burnBtnCenter = b.center
                                    burnBtnWidth = b.width
                                }
                                .testTag("burn_incognito_button")
                        ) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Burn Private Session & Wipe Data")
                        }
                    }
                }
            }

            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Action Chips Row with soft frosted blur edge dissolve on sideways scroll
            LiquidEdgeBlurLazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                edgeFadeWidth = 40.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    GlassPillChip(
                        label = "Incognito",
                        icon = Icons.Rounded.PrivacyTip,
                        active = isIncognito,
                        onClick = onOpenIncognitoTab,
                        testTagName = "chip_incognito"
                    )
                }
                item {
                    GlassPillChip(
                        label = if (vpnState.status == VpnConnectionStatus.CONNECTED_LOCAL_TUN) "VPN TUN On" else "VPN",
                        icon = Icons.Rounded.VpnLock,
                        active = vpnState.status == VpnConnectionStatus.CONNECTED_LOCAL_TUN,
                        onClick = onOpenVpn,
                        testTagName = "chip_vpn"
                    )
                }
                item {
                    GlassPillChip(
                        label = "Ad Shield",
                        icon = Icons.Rounded.Shield,
                        active = adBlockEnabled,
                        onClick = onOpenAdBlock,
                        testTagName = "chip_adblock"
                    )
                }
                item {
                    GlassPillChip(
                        label = "Downloads",
                        icon = Icons.Rounded.Download,
                        onClick = onOpenDownloads,
                        testTagName = "chip_downloads"
                    )
                }
                item {
                    GlassPillChip(
                        label = "Bookmarks",
                        icon = Icons.Rounded.Bookmarks,
                        onClick = onOpenBookmarks,
                        testTagName = "chip_bookmarks"
                    )
                }
                item {
                    GlassPillChip(
                        label = "History",
                        icon = Icons.Rounded.History,
                        onClick = onOpenHistory,
                        testTagName = "chip_history"
                    )
                }
                item {
                    GlassPillChip(
                        label = "Notes",
                        icon = Icons.Rounded.EditNote,
                        onClick = onOpenNotes,
                        testTagName = "chip_notes"
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quick Launch",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Responsive Adaptive Shortcuts Grid
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = when {
                    maxWidth >= 560.dp -> 5
                    maxWidth >= 340.dp -> 4
                    else -> 3
                }
                val totalItems = shortcuts.size + 1
                val rows = (totalItems + columns - 1) / columns

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (r in 0 until rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (c in 0 until columns) {
                                val index = r * columns + c
                                if (index < shortcuts.size) {
                                    val item = shortcuts[index]
                                    ShortcutGlassTile(
                                        shortcut = item,
                                        onClick = { onNavigate(item.url) },
                                        onLongClick = { shortcutToDelete = item },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else if (index == shortcuts.size) {
                                    AddShortcutGlassTile(
                                        onClick = { showAddShortcutDialog = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (showAddShortcutDialog) {
        var titleInput by remember { mutableStateOf("") }
        var urlInput by remember { mutableStateOf("") }
        LiquidGlassModalDialog(onDismissRequest = { showAddShortcutDialog = false }) {
            Text("Add Website Shortcut", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Website URL (e.g. wikipedia.org)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showAddShortcutDialog = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            onAddShortcut(titleInput, urlInput)
                            showAddShortcutDialog = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add")
                }
            }
        }
    }

    shortcutToDelete?.let { item ->
        LiquidGlassModalDialog(onDismissRequest = { shortcutToDelete = null }) {
            Text("Remove Shortcut", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Remove ${item.title} from your NOVA homepage?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { shortcutToDelete = null },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onDeleteShortcut(item.id)
                        shortcutToDelete = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun StealthBadgeCell(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IncognitoAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutGlassTile(
    shortcut: ShortcutEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = remember(shortcut.accentHex) {
        runCatching { Color(android.graphics.Color.parseColor(shortcut.accentHex)) }
            .getOrDefault(Color(0xFF38BDF8))
    }
    val initial = shortcut.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
    val haptic = LocalHapticFeedback.current
    val sakugaVfx = LocalSakugaVfx.current
    var tileCenter by remember { mutableStateOf(Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.87f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shortcut_scale"
    )
    val (tileSqX, tileSqY) = sakugaSquashScale(scale, wideFactor = 0.45f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("shortcut_${shortcut.title.lowercase()}")
            .onGloballyPositioned { tileCenter = it.boundsInRoot().center }
            .graphicsLayer {
                scaleX = tileSqX
                scaleY = tileSqY
            }
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    sakugaVfx?.triggerMicroSpark(tileCenter)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(vertical = 4.dp)
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(18.dp),
            elevation = 4.dp,
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = accent
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = shortcut.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddShortcutGlassTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "add_shortcut_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("shortcut_add_button")
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(vertical = 4.dp)
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(18.dp),
            elevation = 2.dp,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add shortcut",
                    tint = glass.accentStart,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
