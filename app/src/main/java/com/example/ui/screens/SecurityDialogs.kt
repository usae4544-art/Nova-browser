package com.example.ui.screens

import android.net.http.SslError
import android.webkit.PermissionRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.AppUpdateInfo
import com.example.engine.SiteCertificateSummary
import com.example.notifications.NovaNotificationSound
import com.example.ui.components.GlassPillChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.components.NovaWallpaperBackground
import com.example.ui.components.consumeAllTouches
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.IncognitoAccent
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import com.example.viewmodel.IncognitoPinChallenge
import com.example.viewmodel.IncognitoVaultPurpose
import com.example.viewmodel.SitePermissionPrompt
import com.example.viewmodel.SslWarningRequest

@Composable
fun LiquidGlassModalDialog(
    onDismissRequest: () -> Unit,
    accentBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    val progress = enterAnim.value.coerceIn(0f, 1f)
    val modalScale = 0.90f + 0.10f * progress
    val modalBlur = ((1f - progress) * 7f).dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (glass.isDark) 0.46f else 0.24f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(28.dp),
                strong = true,
                accentBorder = accentBorder,
                elevation = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .graphicsLayer {
                        scaleX = modalScale
                        scaleY = modalScale
                        alpha = progress
                    }
                    .then(
                        if (modalBlur > 0.4.dp) {
                            Modifier.blur(radius = modalBlur, edgeTreatment = BlurredEdgeTreatment.Rectangle)
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
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun SslWarningDialog(
    request: SslWarningRequest,
    onCancel: () -> Unit,
    onProceedUnsafe: () -> Unit
) {
    val reason = when (request.primaryError) {
        SslError.SSL_EXPIRED -> "The security certificate has expired."
        SslError.SSL_IDMISMATCH -> "The certificate hostname does not match this website."
        SslError.SSL_UNTRUSTED -> "The certificate authority is not trusted."
        SslError.SSL_NOTYETVALID -> "The certificate is not valid yet."
        else -> "The HTTPS security certificate could not be verified."
    }

    LiquidGlassModalDialog(onDismissRequest = onCancel) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = CoralDanger,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Connection Not Private",
                style = MaterialTheme.typography.titleLarge,
                color = CoralDanger
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Nova Browser blocked an untrusted certificate for ${request.host}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = onProceedUnsafe,
                colors = ButtonDefaults.textButtonColors(contentColor = CoralDanger),
                modifier = Modifier.weight(1f)
            ) {
                Text("Proceed Anyway")
            }
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back to Safety")
            }
        }
    }
}

@Composable
fun SitePermissionDialog(
    prompt: SitePermissionPrompt,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val (title, message, icon) = when (prompt) {
        is SitePermissionPrompt.WebMedia -> {
            val names = prompt.resources.map { res ->
                when (res) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "Camera"
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "Microphone"
                    else -> "Media device"
                }
            }.distinct().joinToString(" & ")
            Triple("Website Permission Request", "${prompt.origin} wants to access your $names.", Icons.Rounded.Shield)
        }
        is SitePermissionPrompt.Geolocation -> {
            Triple("Location Permission Request", "${prompt.origin} wants to access your Approximate Location.", Icons.Rounded.Shield)
        }
        is SitePermissionPrompt.WebNotification -> {
            Triple(
                "Allow Site Notifications?",
                "${prompt.host} wants to send notifications with your custom Nova alert chime.",
                Icons.Rounded.NotificationsActive
            )
        }
    }

    LiquidGlassModalDialog(onDismissRequest = onDeny) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalNovaGlassColors.current.accentStart,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onDeny,
                modifier = Modifier.weight(1f)
            ) {
                Text("Block")
            }
            Button(
                onClick = onAllow,
                modifier = Modifier.weight(1f)
            ) {
                Text("Allow")
            }
        }
    }
}

@Composable
fun IncognitoPinDialog(
    challenge: IncognitoPinChallenge,
    biometricAvailable: Boolean,
    onRequestBiometric: () -> Unit,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val glass = LocalNovaGlassColors.current
    val isDisable = challenge.purpose == IncognitoVaultPurpose.DISABLE_INCOGNITO_PIN

    LiquidGlassModalDialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(IncognitoAccent.copy(alpha = 0.18f))
                    .border(1.2.dp, IncognitoAccent.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = IncognitoAccent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isDisable) "Verify PIN to Remove Lock" else "Unlock Private Vault",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isDisable) {
                    "Verify with Fingerprint or your Nova PIN to disable the lock."
                } else {
                    "Enter your Nova Private Tab PIN to continue."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Glowing Liquid Glass PIN dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dotCount = maxOf(4, pin.length.coerceAtMost(8))
                repeat(dotCount) { idx ->
                    val filled = idx < pin.length
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) IncognitoAccent else glass.glassSurface
                            )
                            .border(
                                width = 1.dp,
                                color = if (filled) IncognitoAccent else glass.glassBorderTop,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
                label = { Text("Enter 4–8 digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incognito_pin_input")
            )

            if (challenge.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = challenge.errorMessage,
                    color = CoralDanger,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            if (biometricAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRequestBiometric,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        tint = IncognitoAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Fingerprint")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (pin.isNotEmpty()) {
                            onSubmit(pin)
                            pin = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncognitoAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isDisable) "Verify & Remove" else "Unlock")
                }
            }
        }
    }
}

@Composable
fun IncognitoVaultLockOverlay(
    visible: Boolean,
    hasPinSet: Boolean,
    biometricAvailable: Boolean,
    onUnlockWithFingerprint: () -> Unit,
    onUnlockWithPin: () -> Unit,
    onExitIncognito: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.97f, animationSpec = tween(180))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "vault_lock_pulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "vault_lock_scale"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            NovaWallpaperBackground(isIncognito = true)
            Box(modifier = Modifier.matchParentSize().consumeAllTouches())

            LiquidGlassSurface(
                shape = RoundedCornerShape(30.dp),
                strong = true,
                accentBorder = true,
                elevation = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 400.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .scale(pulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        IncognitoAccent.copy(alpha = 0.34f),
                                        IncognitoAccent.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(1.4.dp, IncognitoAccent.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (biometricAvailable) Icons.Rounded.Fingerprint else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = IncognitoAccent,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Private Vault Locked",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your Incognito session was locked when Nova went to the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    if (biometricAvailable) {
                        Button(
                            onClick = onUnlockWithFingerprint,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IncognitoAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock with Fingerprint")
                        }
                    }

                    if (hasPinSet) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onUnlockWithPin,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enter Nova PIN")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onExitIncognito,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Normal Tab", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun InstallWebAppDialog(
    initialTitle: String,
    url: String,
    onInstall: (title: String) -> Unit,
    onDismiss: () -> Unit
) {
    var appName by remember(initialTitle, url) {
        mutableStateOf(initialTitle.ifBlank { url.substringAfter("://").substringBefore("/") }.take(28))
    }

    LiquidGlassModalDialog(onDismissRequest = onDismiss) {
        Text("Install Web App", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add this website to your Android Home Screen as an installable app shortcut, just like Chrome.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = appName,
            onValueChange = { if (it.length <= 28) appName = it },
            label = { Text("App Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (appName.isNotBlank()) {
                        onInstall(appName.trim())
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Install")
            }
        }
    }
}

@Composable
fun UnknownSourcesPermissionDialog(
    fileName: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    LiquidGlassModalDialog(onDismissRequest = onDismiss) {
        Text("Allow Install Unknown Apps", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "To install $fileName directly from Nova Browser, Android requires enabling 'Allow from this source' in System Settings.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "After enabling it and pressing Back, Nova Browser will automatically open the Android Package Installer for this APK.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text("Open Settings")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SiteSecurityInspectorDialog(
    summary: SiteCertificateSummary,
    adBlockEnabled: Boolean,
    isHostAllowlisted: Boolean,
    httpsOnlyMode: Boolean,
    isIncognito: Boolean,
    siteNotificationsAllowed: Boolean,
    siteNotificationSoundId: String,
    onToggleAllowlist: () -> Unit,
    onToggleHttpsOnly: (Boolean) -> Unit,
    onToggleSiteNotifications: (Boolean) -> Unit,
    onSelectSiteSound: (String) -> Unit,
    onTestSiteNotification: () -> Unit,
    onOpenShieldDashboard: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusColor = when {
        summary.threatWarning != null -> CoralDanger
        summary.isHttps -> MintSecure
        else -> AmberWarning
    }

    LiquidGlassModalDialog(onDismissRequest = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (summary.isHttps && summary.threatWarning == null) Icons.Rounded.Lock else Icons.Rounded.Warning,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (summary.isHttps && summary.threatWarning == null) "Connection Secure" else if (summary.threatWarning != null) "Certificate Warning" else "Connection Not Encrypted",
                color = statusColor,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = summary.host, style = MaterialTheme.typography.titleSmall)
        if (summary.threatWarning != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary.threatWarning,
                style = MaterialTheme.typography.bodySmall,
                color = CoralDanger
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Issued To: ${summary.issuedTo}\nIssued By: ${summary.issuedBy}\nValid Until: ${summary.validUntil}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildString {
                append("Safe Browsing: Active · DNT & GPC Headers: On")
                if (isIncognito) append("\nScreenshot Protection (FLAG_SECURE): Active")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MintSecure
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ad & Tracker Shield", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (adBlockEnabled && !isHostAllowlisted) "Blocking ads & trackers on this site" else "Paused for this site",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = adBlockEnabled && !isHostAllowlisted,
                onCheckedChange = { onToggleAllowlist() }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("HTTPS-Only Upgrade", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Force encrypted HTTPS connections",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = httpsOnlyMode,
                onCheckedChange = onToggleHttpsOnly
            )
        }

        if (!isIncognito && summary.host.isNotBlank() && summary.host != "Local / Home") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Site Notifications", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (siteNotificationsAllowed) "Alerts ON with custom sound" else "Blocked for ${summary.host}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = siteNotificationsAllowed,
                    onCheckedChange = onToggleSiteNotifications
                )
            }

            if (siteNotificationsAllowed) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NovaNotificationSound.entries.forEach { sound ->
                        GlassPillChip(
                            label = sound.label,
                            active = siteNotificationSoundId == sound.id,
                            onClick = { onSelectSiteSound(sound.id) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = onTestSiteNotification) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Site Alert Sound")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                onDismiss()
                onOpenShieldDashboard()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Full Security & Shield Dashboard")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

/**
 * Animated Liquid Glass GitHub release update popup with pulsing cosmic halo,
 * version delta pill, release highlights, Install Now CTA, and Remind Later dismissal.
 */
@Composable
fun AppUpdateLiquidGlassDialog(
    updateInfo: AppUpdateInfo,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
    onOpenReleasePage: () -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val sakugaVfx = LocalSakugaVfx.current
    var badgeCenter by remember { mutableStateOf(Offset.Zero) }
    var updateBtnCenter by remember { mutableStateOf(Offset.Zero) }
    var updateBtnWidth by remember { mutableStateOf(0f) }

    val pulse = rememberInfiniteTransition(label = "update_badge_pulse")
    val haloScale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "update_halo_scale"
    )

    LaunchedEffect(updateInfo.releaseTag, badgeCenter) {
        if (badgeCenter != Offset.Zero) {
            sakugaVfx?.triggerThemeSplash(badgeCenter)
        }
    }

    LiquidGlassModalDialog(onDismissRequest = onLater, accentBorder = true) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.onGloballyPositioned { badgeCenter = it.boundsInRoot().center }
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(haloScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    glass.accentStart.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    glass.accentStart.copy(alpha = 0.28f),
                                    glass.accentEnd.copy(alpha = 0.24f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.45f), glass.accentStart.copy(alpha = 0.55f))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = glass.accentStart,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MintSecure,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "New Version Available",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LiquidGlassSurface(
                shape = RoundedCornerShape(50),
                accentBorder = true
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "v${updateInfo.currentVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = glass.accentStart,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${updateInfo.releaseTag}${if (updateInfo.formattedApkSize.isNotBlank()) " · ${updateInfo.formattedApkSize}" else ""}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MintSecure
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LiquidGlassSurface(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = updateInfo.releaseTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onLater,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("app_update_later_button")
                ) {
                    Text("Later")
                }

                Button(
                    onClick = {
                        sakugaVfx?.triggerImpactBurst(updateBtnCenter, updateBtnWidth)
                        onUpdateNow()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = glass.accentStart),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .onGloballyPositioned {
                            val b = it.boundsInRoot()
                            updateBtnCenter = b.center
                            updateBtnWidth = b.width
                        }
                        .testTag("app_update_install_button")
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update Now")
                }
            }

            TextButton(onClick = onOpenReleasePage) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("View on GitHub", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Signature Conflict Dialog — shown when downloaded APK cert ≠ installed cert
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SignatureConflictDialog(
    fileName: String,
    onUninstallAndInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "sigConflictHalo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    LiquidGlassModalDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsing warning icon halo
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(haloScale)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    AmberWarning.copy(alpha = haloAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                LiquidGlassSurface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Text(
                text = "Update Signing Changed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Nova Browser v1.0.4+ uses a new permanent signing key.\n\n" +
                        "Android requires uninstalling the old version before installing \"$fileName\".\n\n" +
                        "Your bookmarks and settings are stored separately and will be restored after reinstall.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Warning chip
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "This is a one-time action. Future updates will install smoothly.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarning
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onUninstallAndInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Uninstall & Install", color = Color.Black)
                }
            }
        }
    }
}
