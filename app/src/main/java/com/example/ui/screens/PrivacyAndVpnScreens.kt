package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AdBlockRuleEntity
import com.example.engine.AdBlockStats
import com.example.engine.SecurityUtils
import com.example.engine.UrlResolver
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.LiquidEdgeBlurLazyRow
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.components.sakugaSquashScale
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import com.example.viewmodel.VpnPublicIpState
import com.example.vpn.NovaVpnService
import com.example.vpn.VpnConnectionStatus
import com.example.vpn.VpnRuntimeState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AdBlockDashboardScreen(
    adBlockEnabled: Boolean,
    trackerBlockEnabled: Boolean,
    stats: AdBlockStats,
    rules: List<AdBlockRuleEntity>,
    currentSiteUrl: String,
    isCurrentSiteAllowlisted: Boolean,
    isIncognito: Boolean,
    onSetAdBlockEnabled: (Boolean) -> Unit,
    onSetTrackerBlockEnabled: (Boolean) -> Unit,
    onToggleCurrentSiteAllowlist: () -> Unit,
    onAddRule: (pattern: String, isAllowlist: Boolean) -> Unit,
    onToggleRule: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val currentHost = remember(currentSiteUrl) {
        UrlResolver.extractHost(currentSiteUrl).removePrefix("www.")
    }
    var newRuleInput by remember { mutableStateOf("") }
    var newRuleIsAllowlist by remember { mutableStateOf(false) }

    GlassOverlayScaffold(
        title = "Privacy & Ad Shield",
        isIncognito = isIncognito,
        onBack = onBack
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Shield Counter Card
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    strong = true,
                    accentBorder = adBlockEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = if (adBlockEnabled) MintSecure else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (adBlockEnabled) "Nova Shield Active" else "Nova Shield Paused",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Intercepts known ad & tracker requests",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = adBlockEnabled,
                                onCheckedChange = onSetAdBlockEnabled,
                                modifier = Modifier.testTag("adblock_master_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatPillBox("Total Blocked", stats.totalBlocked.toString(), Modifier.weight(1f))
                            StatPillBox("Ads", stats.adsBlocked.toString(), Modifier.weight(1f))
                            StatPillBox("Trackers", stats.trackersBlocked.toString(), Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tracker Protection",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Block analytics scripts and cross-site pixels",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = trackerBlockEnabled,
                                onCheckedChange = onSetTrackerBlockEnabled,
                                modifier = Modifier.testTag("tracker_block_switch")
                            )
                        }
                    }
                }
            }

            // Current Website Allowlist Quick Toggle
            if (currentHost.isNotBlank()) {
                item {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentHost,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isCurrentSiteAllowlisted) "Shield paused on this site" else "Protected by Nova Shield",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onToggleCurrentSiteAllowlist,
                                modifier = Modifier.testTag("toggle_current_site_allowlist")
                            ) {
                                Text(if (isCurrentSiteAllowlisted) "Enable Shield" else "Allowlist")
                            }
                        }
                    }
                }
            }

            // Add Custom Filter Rule / Allowlist Domain
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Custom Filter & Allowlist Rules",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassPillChip(
                                label = "Block Rule",
                                active = !newRuleIsAllowlist,
                                onClick = { newRuleIsAllowlist = false }
                            )
                            GlassPillChip(
                                label = "Website Allowlist",
                                active = newRuleIsAllowlist,
                                onClick = { newRuleIsAllowlist = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newRuleInput,
                                onValueChange = { newRuleInput = it },
                                placeholder = {
                                    Text(if (newRuleIsAllowlist) "example.com" else "ads.domain.com or /banner/")
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_adblock_rule_input")
                            )
                            GlassIconButton(
                                icon = Icons.Rounded.Add,
                                contentDescription = "Add filter rule",
                                active = true,
                                onClick = {
                                    if (newRuleInput.isNotBlank()) {
                                        onAddRule(newRuleInput, newRuleIsAllowlist)
                                        newRuleInput = ""
                                    }
                                },
                                testTagName = "add_adblock_rule_button"
                            )
                        }
                    }
                }
            }

            // Active Custom Rules List
            if (rules.isNotEmpty()) {
                items(rules, key = { it.id }) { rule ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.pattern,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (rule.isAllowlist) "Allowlist" else "Custom Block",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (rule.isAllowlist) MintSecure else glass.accentStart
                                )
                            }
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { onToggleRule(rule.id, it) }
                            )
                            GlassIconButton(
                                icon = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.cd_delete),
                                onClick = { onDeleteRule(rule.id) },
                                iconSize = 18.dp
                            )
                        }
                    }
                }
            }

            // Recent Blocked Log
            if (stats.recentBlocked.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Blocked Requests",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(stats.recentBlocked.take(15)) { entry ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.host,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "On ${entry.pageHost}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = entry.reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentStart
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPillBox(label: String, value: String, modifier: Modifier = Modifier) {
    LiquidGlassSurface(shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = LocalNovaGlassColors.current.accentStart
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VpnSettingsScreen(
    vpnState: VpnRuntimeState,
    savedDns: String,
    savedGateway: String,
    netShieldEnabled: Boolean = true,
    dohEnabled: Boolean = true,
    publicIpState: VpnPublicIpState = VpnPublicIpState(),
    isIncognito: Boolean,
    onSaveDns: (String) -> Unit,
    onSaveGateway: (String) -> Unit,
    onSetNetShield: (Boolean) -> Unit = {},
    onSetDohEnabled: (Boolean) -> Unit = {},
    onCheckPublicIp: () -> Unit = {},
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onSuppressNextBackgroundLock: () -> Unit = {},
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalNovaGlassColors.current
    var dnsInput by remember(savedDns) { mutableStateOf(savedDns) }
    var gatewayInput by remember(savedGateway) { mutableStateOf(savedGateway) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onStartVpn()
        } else {
            onShowMessage("Android VPN permission was denied.")
        }
    }

    val isConnected = vpnState.status == VpnConnectionStatus.CONNECTED_LOCAL_TUN
    var elapsedSec by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isConnected, vpnState.connectedSinceMs) {
        if (!isConnected || vpnState.connectedSinceMs == null) {
            elapsedSec = 0L
        } else {
            while (true) {
                elapsedSec = ((System.currentTimeMillis() - vpnState.connectedSinceMs) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }

    val protonLaunchIntent = remember(context) {
        context.packageManager.getLaunchIntentForPackage("ch.protonvpn.android")
    }
    val wireguardLaunchIntent = remember(context) {
        context.packageManager.getLaunchIntentForPackage("com.wireguard.android")
    }

    val statusColor = when (vpnState.status) {
        VpnConnectionStatus.CONNECTED_LOCAL_TUN -> MintSecure
        VpnConnectionStatus.CONNECTING -> glass.accentStart
        VpnConnectionStatus.ERROR -> CoralDanger
        VpnConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GlassOverlayScaffold(
        title = "Nova VPN & NetShield",
        isIncognito = isIncognito,
        onBack = onBack
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Proton-style Hero Quick Connect & Telemetry Card
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    strong = true,
                    accentBorder = isConnected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Icon(
                            imageVector = when (vpnState.status) {
                                VpnConnectionStatus.CONNECTED_LOCAL_TUN -> Icons.Rounded.CheckCircle
                                VpnConnectionStatus.ERROR -> Icons.Rounded.Warning
                                else -> Icons.Rounded.VpnKey
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = when (vpnState.status) {
                                VpnConnectionStatus.DISCONNECTED -> "VPN Shield Disconnected"
                                VpnConnectionStatus.CONNECTING -> "Establishing Interface..."
                                VpnConnectionStatus.CONNECTED_LOCAL_TUN -> "VPN Shield Active"
                                VpnConnectionStatus.ERROR -> "VPN Connection Error"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isConnected) {
                            val mins = elapsedSec / 60L
                            val secs = elapsedSec % 60L
                            Text(
                                text = String.format(Locale.US, "Session %02d:%02d", mins, secs),
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.accentStart
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = vpnState.statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isConnected) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                StatPillBox("DNS Queries", vpnState.queriesForwarded.toString(), Modifier.weight(1f))
                                StatPillBox("NetShield Blocked", vpnState.threatsBlocked.toString(), Modifier.weight(1f))
                                StatPillBox("Protocol", if (vpnState.dohEnabled) "DoH TLS" else "UDP 53", Modifier.weight(1f))
                            }
                            if (vpnState.lastBlockedDomain.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Last NetShield Block: ${vpnState.lastBlockedDomain}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintSecure,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val sakugaVfx = LocalSakugaVfx.current
                        var vpnBtnCenter by remember { mutableStateOf(Offset.Zero) }
                        var vpnBtnWidth by remember { mutableStateOf(0f) }
                        val vpnBtnInteraction = remember { MutableInteractionSource() }
                        val isVpnBtnPressed by vpnBtnInteraction.collectIsPressedAsState()
                        val vpnPressY by animateFloatAsState(
                            targetValue = if (isVpnBtnPressed) 0.90f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "vpn_connect_squash"
                        )
                        val (vpnSqX, vpnSqY) = sakugaSquashScale(vpnPressY, wideFactor = 0.35f)

                        Button(
                            interactionSource = vpnBtnInteraction,
                            onClick = {
                                sakugaVfx?.triggerImpactBurst(vpnBtnCenter, vpnBtnWidth)
                                if (isConnected) {
                                    onStopVpn()
                                } else {
                                    val cleanDns = dnsInput.trim().ifBlank { "1.1.1.1" }
                                    if (!SecurityUtils.isValidIpAddress(cleanDns)) {
                                        onShowMessage("Enter a valid IPv4 or IPv6 DNS IP (e.g. 1.1.1.1).")
                                        return@Button
                                    }
                                    dnsInput = cleanDns
                                    onSaveDns(cleanDns)
                                    onSaveGateway(gatewayInput)
                                    val prepareIntent = NovaVpnService.prepareIntent(context)
                                    if (prepareIntent != null) {
                                        onSuppressNextBackgroundLock()
                                        vpnPermissionLauncher.launch(prepareIntent)
                                    } else {
                                        onStartVpn()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) CoralDanger else glass.accentStart
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .onGloballyPositioned {
                                    val b = it.boundsInRoot()
                                    vpnBtnCenter = b.center
                                    vpnBtnWidth = b.width
                                }
                                .graphicsLayer {
                                    scaleX = vpnSqX
                                    scaleY = vpnSqY
                                }
                                .testTag("vpn_connect_button")
                        ) {
                            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isConnected) "Disconnect VPN Shield" else "Quick Connect VPN Shield")
                        }
                    }
                }
            }

            // Proton-Style NetShield & Encrypted DoH Controls
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val sakugaVfxSwitch = LocalSakugaVfx.current
                        Text(
                            text = "Shield & Encryption Features",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = if (netShieldEnabled) MintSecure else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("NetShield Ad & Tracker Blocker", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Blocks known ad networks & telemetry at the VPN DNS layer",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            var netShieldSwitchCenter by remember { mutableStateOf(Offset.Zero) }
                            Switch(
                                checked = netShieldEnabled,
                                onCheckedChange = { enabled ->
                                    sakugaVfxSwitch?.triggerMicroSpark(netShieldSwitchCenter)
                                    onSetNetShield(enabled)
                                },
                                modifier = Modifier.onGloballyPositioned {
                                    netShieldSwitchCenter = it.boundsInRoot().center
                                }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = if (dohEnabled) glass.accentStart else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Encrypted DNS-over-HTTPS (DoH)", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Encrypts DNS lookups over TLS 1.3 via RFC 8484",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            var dohSwitchCenter by remember { mutableStateOf(Offset.Zero) }
                            Switch(
                                checked = dohEnabled,
                                onCheckedChange = { enabled ->
                                    sakugaVfxSwitch?.triggerMicroSpark(dohSwitchCenter)
                                    onSetDohEnabled(enabled)
                                },
                                modifier = Modifier.onGloballyPositioned {
                                    dohSwitchCenter = it.boundsInRoot().center
                                }
                            )
                        }
                    }
                }
            }

            // Real Exit IP & ISP Verification Card
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = glass.accentStart,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Live Exit IP Check", style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(
                                onClick = onCheckPublicIp,
                                enabled = !publicIpState.isChecking,
                                shape = RoundedCornerShape(50)
                            ) {
                                if (publicIpState.isChecking) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (publicIpState.isChecking) "Checking..." else "Check IP")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        when {
                            publicIpState.ipAddress.isNotBlank() -> {
                                val meta = listOf(publicIpState.country, publicIpState.isp)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · ")
                                Text(
                                    text = "Public IP: ${publicIpState.ipAddress}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MintSecure
                                )
                                if (meta.isNotBlank()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            !publicIpState.errorMessage.isNullOrBlank() -> {
                                Text(
                                    text = publicIpState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberWarning
                                )
                            }
                            else -> {
                                Text(
                                    text = "Tap Check IP to verify your real external IP address and ISP/country.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // DNS Resolver Presets
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(
                            text = "TUN DNS Resolver",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val dnsPresets = listOf(
                            "1.1.1.1" to "Cloudflare",
                            "9.9.9.9" to "Quad9",
                            "8.8.8.8" to "Google",
                            "94.140.14.14" to "AdGuard"
                        )
                        LiquidEdgeBlurLazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            edgeFadeWidth = 30.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dnsPresets, key = { it.first }) { (ip, label) ->
                                GlassPillChip(
                                    label = label,
                                    active = dnsInput == ip,
                                    onClick = {
                                        dnsInput = ip
                                        onSaveDns(ip)
                                        if (isConnected) {
                                            onStartVpn()
                                        }
                                        onShowMessage("VPN DNS set to $label ($ip)")
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            OutlinedTextField(
                                value = dnsInput,
                                onValueChange = { dnsInput = it },
                                label = { Text("DNS IP Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = gatewayInput,
                                onValueChange = { gatewayInput = it },
                                label = { Text("Remote VPN Gateway Host (Optional)") },
                                placeholder = { Text("e.g. vpn.yourdomain.com:51820") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Proton VPN Companion + Always-On / Kill Switch + Transparency Notice
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.VpnLock,
                                contentDescription = null,
                                tint = glass.accentStart,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Proton VPN & System Kill Switch",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nova's built-in Shield gives genuine Encrypted DoH DNS & NetShield Ad/Tracker filtering without needing an account. For full encrypted remote country IP routing, connect Proton VPN or WireGuard / OpenVPN and configure Android Always-On + Block connections without VPN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onSuppressNextBackgroundLock()
                                val intent = protonLaunchIntent ?: wireguardLaunchIntent ?: Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://protonvpn.com/download-android")
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }
                                    .onFailure { onShowMessage("Unable to open Proton VPN launcher.") }
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.VpnLock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when {
                                    protonLaunchIntent != null -> "Open Proton VPN App"
                                    wireguardLaunchIntent != null -> "Open WireGuard App"
                                    else -> "Get Proton VPN (Multi-Country Tunnel)"
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    onSuppressNextBackgroundLock()
                                    context.startActivity(
                                        Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.onFailure {
                                    onShowMessage("System VPN settings unavailable on this device.")
                                }
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_system_vpn_settings")
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Always-On & System VPN Settings")
                        }
                    }
                }
            }
        }
    }
}
