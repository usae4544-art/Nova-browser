package com.example.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.SiteNotificationRuleEntity
import com.example.data.preferences.BrowserSettings
import com.example.data.preferences.SearchEngine
import com.example.engine.ChromiumEngineInfo
import com.example.notifications.NovaNotificationSound
import com.example.ui.components.BrowserBarLayoutPreview
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.LocalNovaGlassColors
import com.example.vpn.VpnConnectionStatus
import com.example.vpn.VpnRuntimeState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: BrowserSettings,
    vpnState: VpnRuntimeState,
    chromiumInfo: ChromiumEngineInfo,
    isIncognito: Boolean,
    biometricAvailable: Boolean,
    siteNotificationRules: List<SiteNotificationRuleEntity>,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateSearchEngine: (SearchEngine) -> Unit,
    onUpdateCustomHomepage: (String) -> Unit,
    onSetAutoDarkWebsites: (Boolean) -> Unit,
    onSetShowTopBar: (Boolean) -> Unit,
    onSetShowBottomBar: (Boolean) -> Unit,
    onSetTextZoom: (Int) -> Unit,
    onSetAdBlock: (Boolean) -> Unit,
    onSetHttpsOnly: (Boolean) -> Unit,
    onSetCookies: (Boolean) -> Unit,
    onSetThirdPartyCookies: (Boolean) -> Unit,
    onSetJavaScript: (Boolean) -> Unit,
    onSetPopupBlocking: (Boolean) -> Unit,
    onSetAllowCamera: (Boolean) -> Unit,
    onSetAllowMic: (Boolean) -> Unit,
    onSetAllowLocation: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetSiteNotificationSound: (String) -> Unit,
    onToggleSiteNotificationRule: (host: String, allowed: Boolean) -> Unit,
    onDeleteSiteNotificationRule: (host: String) -> Unit,
    onTestNotificationSound: (String) -> Unit,
    onSetIncognitoBiometric: (Boolean) -> Unit,
    onSetupIncognitoPin: (String) -> Unit,
    onRequestDisableIncognitoPin: () -> Unit,
    onClearAllBrowsingData: () -> Unit,
    onOpenAdBlockDashboard: () -> Unit,
    onOpenVpnSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSuppressNextBackgroundLock: () -> Unit = {},
    isCheckingForUpdates: Boolean = false,
    onCheckForUpdates: () -> Unit = {},
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var homepageInput by remember(settings.customHomepageUrl) { mutableStateOf(settings.customHomepageUrl) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var canInstallUnknownApps by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            } else true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canInstallUnknownApps = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetNotifications(granted)
        if (!granted) onShowMessage("Notification permission denied")
    }

    val defaultBrowserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onShowMessage("Default browser settings updated")
    }

    GlassOverlayScaffold(
        title = "Settings",
        isIncognito = isIncognito,
        onBack = onBack
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Appearance
            item {
                SettingsSectionCard(title = "Appearance", icon = Icons.Rounded.Palette) {
                    Text(
                        text = "Liquid Glass Theme",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            GlassPillChip(
                                label = mode.label,
                                active = settings.themeMode == mode,
                                onClick = { onUpdateTheme(mode) },
                                testTagName = "theme_${mode.name.lowercase()}"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    BrowserBarLayoutPreview(
                        showTopBar = settings.showTopBar,
                        showBottomBar = settings.showBottomBar,
                        onSetShowTopBar = onSetShowTopBar,
                        onSetShowBottomBar = onSetShowBottomBar
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleRow(
                        title = "Top Floating Address Bar",
                        subtitle = "Show or hide the top Liquid Glass search and URL bar independently",
                        checked = settings.showTopBar,
                        onCheckedChange = onSetShowTopBar,
                        switchTestTag = "settings_show_top_bar_switch"
                    )
                    SettingsToggleRow(
                        title = "Bottom Floating Dock",
                        subtitle = "Show or hide the bottom iPhone Liquid Glass bar independently (corner menu stays accessible)",
                        checked = settings.showBottomBar,
                        onCheckedChange = onSetShowBottomBar,
                        switchTestTag = "settings_show_bottom_bar_switch"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleRow(
                        title = "Dark Mode on All Websites",
                        subtitle = "Automatically darkens every website whenever System Dark Mode or Dark Theme is active",
                        checked = settings.autoDarkWebsites,
                        onCheckedChange = onSetAutoDarkWebsites
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Website Text Size", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${settings.textZoomPercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = LocalNovaGlassColors.current.accentStart
                        )
                    }
                    Slider(
                        value = settings.textZoomPercent.toFloat(),
                        onValueChange = { onSetTextZoom((it / 5f).roundToInt() * 5) },
                        valueRange = 75f..200f,
                        steps = 24
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = homepageInput,
                            onValueChange = { homepageInput = it },
                            label = { Text("Custom Homepage URL (blank = NOVA)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { onUpdateCustomHomepage(homepageInput) }) {
                            Text("Save")
                        }
                    }
                }
            }

            // 2. Search Engine
            item {
                SettingsSectionCard(title = "Search Engine", icon = Icons.Rounded.Search) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchEngine.entries.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { engine ->
                                    GlassPillChip(
                                        label = engine.displayName,
                                        active = settings.searchEngine == engine,
                                        onClick = { onUpdateSearchEngine(engine) },
                                        testTagName = "search_engine_${engine.name.lowercase()}"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Privacy & Security
            item {
                SettingsSectionCard(title = "Privacy & Security", icon = Icons.Rounded.Security) {
                    SettingsToggleRow(
                        title = "HTTPS-Only Upgrade",
                        subtitle = "Automatically upgrade HTTP links to HTTPS",
                        checked = settings.httpsOnlyMode,
                        onCheckedChange = onSetHttpsOnly
                    )
                    SettingsToggleRow(
                        title = "Cookies",
                        subtitle = "Allow websites to store login sessions",
                        checked = settings.cookiesEnabled,
                        onCheckedChange = onSetCookies
                    )
                    SettingsToggleRow(
                        title = "Third-Party Cookies",
                        subtitle = "Disabled by default for stronger tracking protection",
                        checked = settings.thirdPartyCookiesEnabled,
                        onCheckedChange = onSetThirdPartyCookies
                    )
                    SettingsToggleRow(
                        title = "JavaScript",
                        subtitle = "Required by modern web apps like YouTube and Google",
                        checked = settings.javascriptEnabled,
                        onCheckedChange = onSetJavaScript
                    )
                    SettingsToggleRow(
                        title = "Pop-up Blocker",
                        subtitle = "Prevent unsolicited automatic pop-up windows",
                        checked = settings.popupBlockingEnabled,
                        onCheckedChange = onSetPopupBlocking
                    )
                    SettingsToggleRow(
                        title = "Fingerprint Unlock (Private Vault)",
                        subtitle = if (biometricAvailable) {
                            "Auto-lock Incognito on background & unlock with Fingerprint or PIN"
                        } else {
                            "Biometric hardware not enrolled on this device (uses PIN fallback)"
                        },
                        checked = settings.incognitoBiometricEnabled,
                        onCheckedChange = onSetIncognitoBiometric
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (settings.incognitoLockEnabled) onRequestDisableIncognitoPin()
                                else showPinSetupDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("incognito_pin_button")
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (settings.incognitoLockEnabled) "Remove PIN" else "Lock Private Tabs")
                        }
                        OutlinedButton(
                            onClick = { showClearDataDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_browsing_data_button")
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = CoralDanger, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Data")
                        }
                    }
                }
            }

            // 4. Ad Blocker, VPN & Downloads Links
            item {
                SettingsSectionCard(title = "Shield, VPN & Downloads", icon = Icons.Rounded.Shield) {
                    SettingsToggleRow(
                        title = "Nova Ad & Tracker Blocker",
                        subtitle = "Filter known ad networks and trackers",
                        checked = settings.adBlockEnabled,
                        onCheckedChange = onSetAdBlock
                    )
                    SettingsNavRow(
                        icon = Icons.Rounded.Shield,
                        title = "Ad Blocker & Custom Rules",
                        subtitle = "Manage allowlist, custom rules, and blocked logs",
                        onClick = onOpenAdBlockDashboard
                    )
                    SettingsNavRow(
                        icon = Icons.Rounded.VpnLock,
                        title = "VPN Settings",
                        subtitle = when (vpnState.status) {
                            VpnConnectionStatus.CONNECTED_LOCAL_TUN -> "Local TUN Active (${vpnState.dnsServer})"
                            VpnConnectionStatus.ERROR -> "Error — tap to review"
                            else -> "Disconnected · Configure Android VpnService"
                        },
                        onClick = onOpenVpnSettings
                    )
                    SettingsNavRow(
                        icon = Icons.Rounded.Download,
                        title = "Download Manager",
                        subtitle = "Manage active, paused, and completed files",
                        onClick = onOpenDownloads
                    )
                    SettingsNavRow(
                        icon = Icons.Rounded.InstallMobile,
                        title = "Install Unknown Apps (APK Sources)",
                        subtitle = if (canInstallUnknownApps) {
                            "Allowed · Direct APK installation enabled"
                        } else {
                            "Not allowed yet · Tap to enable in Android Settings"
                        },
                        onClick = {
                            runCatching {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                } else {
                                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                onSuppressNextBackgroundLock()
                                context.startActivity(intent)
                            }.onFailure {
                                onShowMessage("Unable to open Install Unknown Apps settings.")
                            }
                        }
                    )
                }
            }

            // 5. Website Permissions & Notifications
            item {
                SettingsSectionCard(title = "Website Permissions & Notifications", icon = Icons.Rounded.Notifications) {
                    SettingsToggleRow(
                        title = "Camera Access Prompt",
                        subtitle = "Allow websites to request camera permission",
                        checked = settings.allowCameraSites,
                        onCheckedChange = onSetAllowCamera
                    )
                    SettingsToggleRow(
                        title = "Microphone Access Prompt",
                        subtitle = "Allow websites to request microphone permission",
                        checked = settings.allowMicSites,
                        onCheckedChange = onSetAllowMic
                    )
                    SettingsToggleRow(
                        title = "Location Access Prompt",
                        subtitle = "Allow websites to request approximate location",
                        checked = settings.allowLocationSites,
                        onCheckedChange = onSetAllowLocation
                    )
                    SettingsToggleRow(
                        title = "Browser & Site Notifications",
                        subtitle = "Allow enabled websites and download alerts with distinct chime sounds",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                onSuppressNextBackgroundLock()
                                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSetNotifications(enable)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Site Notification Chime Sound",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NovaNotificationSound.entries.forEach { sound ->
                            GlassPillChip(
                                label = sound.label,
                                active = settings.siteNotificationSoundId == sound.id,
                                onClick = { onSetSiteNotificationSound(sound.id) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onTestNotificationSound(settings.siteNotificationSoundId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview & Test Notification Sound")
                    }

                    if (siteNotificationRules.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Enabled / Configured Websites",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        siteNotificationRules.forEach { rule ->
                            val soundLabel = NovaNotificationSound.fromId(rule.soundId).label
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = rule.host, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = if (rule.allowed) "Allowed · $soundLabel" else "Blocked",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = rule.allowed,
                                    onCheckedChange = { onToggleSiteNotificationRule(rule.host, it) }
                                )
                                GlassIconButton(
                                    icon = Icons.Rounded.Delete,
                                    contentDescription = "Remove rule",
                                    onClick = { onDeleteSiteNotificationRule(rule.host) },
                                    iconSize = 18.dp
                                )
                            }
                        }
                    }
                }
            }

            // 6. Default Browser & About NOVA
            item {
                SettingsSectionCard(title = "Default Browser & About", icon = Icons.Rounded.Info) {
                    SettingsNavRow(
                        icon = Icons.Rounded.Language,
                        title = "Set as Default Browser",
                        subtitle = "Open Android Default Browser selector",
                        onClick = {
                            runCatching {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                                        !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
                                    ) {
                                        onSuppressNextBackgroundLock()
                                        defaultBrowserLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                                        return@SettingsNavRow
                                    }
                                }
                                onSuppressNextBackgroundLock()
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure {
                                onShowMessage("Unable to open default browser settings on this device.")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    val installedVersion = remember(context) {
                        runCatching {
                            val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                context.packageManager.getPackageInfo(
                                    context.packageName,
                                    android.content.pm.PackageManager.PackageInfoFlags.of(0L)
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                context.packageManager.getPackageInfo(context.packageName, 0)
                            }
                            pkg.versionName ?: "1.0.5"
                        }.getOrDefault("1.0.5")
                    }
                    Text(
                        text = "Nova Browser v$installedVersion",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Engine: ${chromiumInfo.engineName}\nPackage: ${chromiumInfo.packageName}\nChromium WebView Version: ${chromiumInfo.versionName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalNovaGlassColors.current.accentStart,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = chromiumInfo.limitationsNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsNavRow(
                        icon = Icons.Rounded.SystemUpdate,
                        title = if (isCheckingForUpdates) "Checking GitHub Releases..." else "Check for App Updates",
                        subtitle = "Checks usae4544-art/Nova-browser releases for new builds",
                        onClick = onCheckForUpdates
                    )
                }
            }
        }
    }

    if (showPinSetupDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        LiquidGlassModalDialog(onDismissRequest = { showPinSetupDialog = false }) {
            Text("Set Private Tab PIN", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your PIN is stored using salted iterative SHA-256 hashing and never saved in plaintext.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
                label = { Text("Enter PIN (4–8 digits)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 8) confirmPin = it.filter(Char::isDigit) },
                label = { Text("Confirm PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error!!, color = CoralDanger, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showPinSetupDialog = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        when {
                            pin.length < 4 -> error = "PIN must be at least 4 digits."
                            pin != confirmPin -> error = "PINs do not match."
                            else -> {
                                onSetupIncognitoPin(pin)
                                showPinSetupDialog = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enable Lock")
                }
            }
        }
    }

    if (showClearDataDialog) {
        LiquidGlassModalDialog(onDismissRequest = { showClearDataDialog = false }) {
            Text("Clear All Browsing Data?", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Removes browsing history, WebView cache, site cookies, and WebStorage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showClearDataDialog = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onClearAllBrowsingData()
                        showClearDataDialog = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Now")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    LiquidGlassSurface(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LocalNovaGlassColors.current.accentStart,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalNovaGlassColors.current.accentStart,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
