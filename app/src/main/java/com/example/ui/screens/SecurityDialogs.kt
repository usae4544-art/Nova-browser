package com.example.ui.screens

import android.net.http.SslError
import android.webkit.PermissionRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.engine.SiteCertificateSummary
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.MintSecure
import com.example.viewmodel.IncognitoPinChallenge
import com.example.viewmodel.SitePermissionPrompt
import com.example.viewmodel.SslWarningRequest

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

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Connection Not Private", color = CoralDanger) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nova Browser blocked an untrusted certificate for ${request.host}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onCancel) {
                Text("Back to Safety")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onProceedUnsafe,
                colors = ButtonDefaults.textButtonColors(contentColor = CoralDanger)
            ) {
                Text("Proceed Anyway")
            }
        }
    )
}

@Composable
fun SitePermissionDialog(
    prompt: SitePermissionPrompt,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val (origin, label) = when (prompt) {
        is SitePermissionPrompt.WebMedia -> {
            val names = prompt.resources.map { res ->
                when (res) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "Camera"
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "Microphone"
                    else -> "Media device"
                }
            }.distinct().joinToString(" & ")
            prompt.origin to names
        }
        is SitePermissionPrompt.Geolocation -> prompt.origin to "Approximate Location"
    }

    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text("Website Permission Request") },
        text = {
            Text(
                text = "$origin wants to access your $label.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onAllow) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Block")
            }
        }
    )
}

@Composable
fun IncognitoPinDialog(
    challenge: IncognitoPinChallenge,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock Private Vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter your Nova Private Tab PIN to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (challenge.errorMessage != null) {
                    Text(
                        text = challenge.errorMessage,
                        color = CoralDanger,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.isNotEmpty()) {
                        onSubmit(pin)
                        pin = ""
                    }
                }
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install Web App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add this website to your Android Home Screen as an installable app shortcut, just like Chrome.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = appName,
                    onValueChange = { if (it.length <= 28) appName = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (appName.isNotBlank()) {
                        onInstall(appName.trim())
                    }
                }
            ) {
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UnknownSourcesPermissionDialog(
    fileName: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow Install Unknown Apps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "To install $fileName directly from Nova Browser, Android requires enabling 'Allow from this source' in System Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "After enabling it and pressing Back, Nova Browser will automatically open the Android Package Installer for this APK.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SiteSecurityInspectorDialog(
    summary: SiteCertificateSummary,
    adBlockEnabled: Boolean,
    isHostAllowlisted: Boolean,
    httpsOnlyMode: Boolean,
    isIncognito: Boolean,
    onToggleAllowlist: () -> Unit,
    onToggleHttpsOnly: (Boolean) -> Unit,
    onOpenShieldDashboard: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusColor = when {
        summary.threatWarning != null -> CoralDanger
        summary.isHttps -> MintSecure
        else -> AmberWarning
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (summary.isHttps && summary.threatWarning == null) Icons.Rounded.Lock else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (summary.isHttps) "Connection Secure" else "Connection Not Encrypted",
                    color = statusColor,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = summary.host,
                    style = MaterialTheme.typography.titleSmall
                )
                if (summary.threatWarning != null) {
                    Text(
                        text = summary.threatWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralDanger
                    )
                }
                Text(
                    text = "Issued To: ${summary.issuedTo}\nIssued By: ${summary.issuedBy}\nValid Until: ${summary.validUntil}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildString {
                        append("Safe Browsing: Active · DNT & GPC Headers: On")
                        if (isIncognito) append("\nScreenshot Protection (FLAG_SECURE): Active")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MintSecure
                )

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ad & Tracker Shield", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (adBlockEnabled && !isHostAllowlisted) "Blocking ads & trackers on this site"
                            else "Paused for this site",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = adBlockEnabled && !isHostAllowlisted,
                        onCheckedChange = { onToggleAllowlist() }
                    )
                }

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

                Spacer(modifier = Modifier.height(2.dp))
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
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}


