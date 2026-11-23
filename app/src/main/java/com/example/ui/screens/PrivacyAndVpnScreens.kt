package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AdBlockRuleEntity
import com.example.engine.AdBlockStats
import com.example.engine.UrlResolver
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import com.example.vpn.NovaVpnService
import com.example.vpn.VpnConnectionStatus
import com.example.vpn.VpnRuntimeState

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
    isIncognito: Boolean,
    onSaveDns: (String) -> Unit,
    onSaveGateway: (String) -> Unit,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
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
    val statusColor = when (vpnState.status) {
        VpnConnectionStatus.CONNECTED_LOCAL_TUN -> MintSecure
        VpnConnectionStatus.CONNECTING -> glass.accentStart
        VpnConnectionStatus.ERROR -> CoralDanger
        VpnConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GlassOverlayScaffold(
        title = "VPN Settings",
        isIncognito = isIncognito,
        onBack = onBack
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Connection Status Card
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
                                VpnConnectionStatus.DISCONNECTED -> "VPN Disconnected"
                                VpnConnectionStatus.CONNECTING -> "Establishing Interface..."
                                VpnConnectionStatus.CONNECTED_LOCAL_TUN -> "Local VpnService TUN Active"
                                VpnConnectionStatus.ERROR -> "VPN Connection Error"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = vpnState.statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isConnected) {
                                    onStopVpn()
                                } else {
                                    onSaveDns(dnsInput)
                                    onSaveGateway(gatewayInput)
                                    val prepareIntent = NovaVpnService.prepareIntent(context)
                                    if (prepareIntent != null) {
                                        vpnPermissionLauncher.launch(prepareIntent)
                                    } else {
                                        onStartVpn()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) CoralDanger else glass.accentStart
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("vpn_connect_button")
                        ) {
                            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isConnected) "Disconnect VPN Interface" else "Start Android VpnService")
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TUN DNS Resolver",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("1.1.1.1" to "Cloudflare", "9.9.9.9" to "Quad9", "8.8.8.8" to "Google").forEach { (ip, label) ->
                                GlassPillChip(
                                    label = label,
                                    active = dnsInput == ip,
                                    onClick = {
                                        dnsInput = ip
                                        onSaveDns(ip)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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

            // Honest Security Notice + System VPN Settings
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connection Transparency",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nova Browser never simulates VPN protection. The toggle above uses Android's genuine VpnService API to create a local TUN/DNS interface. To encrypt and tunnel all internet traffic through a remote exit IP, connect an external WireGuard/OpenVPN/IKEv2 profile in Android System VPN Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.onFailure {
                                    onShowMessage("System VPN settings unavailable on this device.")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_system_vpn_settings")
                        ) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Android System VPN Settings")
                        }
                    }
                }
            }
        }
    }
}
