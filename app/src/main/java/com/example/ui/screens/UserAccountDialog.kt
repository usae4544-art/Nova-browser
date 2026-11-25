package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserAccountEntity
import com.example.ui.components.GlassIconButton
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AVATAR_PALETTE = listOf(
    "#8B5CF6", // Purple
    "#38BDF8", // Cyan
    "#EC4899", // Pink
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#6366F1"  // Indigo
)

@Composable
fun UserAccountDialog(
    currentAccount: UserAccountEntity?,
    onDismiss: () -> Unit,
    onLogin: (email: String, username: String, displayName: String, avatarHex: String) -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (displayName: String, bio: String, avatarHex: String) -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val sakugaVfx = LocalSakugaVfx.current

    var emailInput by remember { mutableStateOf(currentAccount?.email.orEmpty()) }
    var usernameInput by remember { mutableStateOf(currentAccount?.username.orEmpty()) }
    var displayNameInput by remember { mutableStateOf(currentAccount?.displayName.orEmpty()) }
    var bioInput by remember { mutableStateOf(currentAccount?.bio.orEmpty()) }
    var selectedAvatarHex by remember { mutableStateOf(currentAccount?.avatarColorHex ?: AVATAR_PALETTE[0]) }
    var isEditingProfile by remember { mutableStateOf(false) }

    var actionBtnCenter by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(28.dp),
                strong = true,
                accentBorder = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_account_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = glass.accentStart,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentAccount != null) "Nova Account" else "Sign In or Register",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Close",
                            onClick = onDismiss
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentAccount != null && !isEditingProfile) {
                        // LOGGED IN VIEW
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(parseHex(currentAccount.avatarColorHex))
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.8f), glass.accentStart)
                                        ),
                                        shape = CircleShape
                                    )
                            ) {
                                Text(
                                    text = currentAccount.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentAccount.displayName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint = MintSecure,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (currentAccount.username.isNotBlank()) {
                                Text(
                                    text = "@${currentAccount.username}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.accentStart
                                )
                            }

                            if (currentAccount.email.isNotBlank()) {
                                Text(
                                    text = currentAccount.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (currentAccount.bio.isNotBlank()) {
                                Text(
                                    text = "\"${currentAccount.bio}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Member info chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(glass.glassSurfaceStrong)
                                    .border(0.8.dp, glass.glassBorder, RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Active Account · Logged in across Nova Browser",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintSecure
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        displayNameInput = currentAccount.displayName
                                        bioInput = currentAccount.bio
                                        selectedAvatarHex = currentAccount.avatarColorHex
                                        isEditingProfile = true
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Edit Profile", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = {
                                        onLogout()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ExitToApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log Out", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    } else {
                        // SIGN IN / REGISTER / EDIT PROFILE FORM
                        Text(
                            text = if (isEditingProfile) "Update Profile" else "Log In or Create Profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Avatar Picker Row
                        Text(
                            text = "Choose Avatar Accent",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AVATAR_PALETTE.forEach { hex ->
                                val isSelected = hex.equals(selectedAvatarHex, ignoreCase = true)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(parseHex(hex))
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatarHex = hex }
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Display Name
                        GlassInputField(
                            label = "Display Name",
                            placeholder = "e.g. Alex",
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!isEditingProfile) {
                            // Username / Email
                            GlassInputField(
                                label = "Email or Username",
                                placeholder = "e.g. alex@nova.app",
                                value = emailInput,
                                onValueChange = { emailInput = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Bio
                        GlassInputField(
                            label = "Bio / Tagline",
                            placeholder = "e.g. Privacy advocate & daily surfer",
                            value = bioInput,
                            onValueChange = { bioInput = it }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isEditingProfile) {
                                TextButton(onClick = { isEditingProfile = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Button(
                                onClick = {
                                    sakugaVfx?.triggerImpactBurst(actionBtnCenter)
                                    if (isEditingProfile) {
                                        onUpdateProfile(displayNameInput, bioInput, selectedAvatarHex)
                                        isEditingProfile = false
                                    } else {
                                        val resolvedName = displayNameInput.ifBlank {
                                            emailInput.substringBefore("@").ifBlank { "Nova User" }
                                        }
                                        onLogin(
                                            emailInput,
                                            emailInput.substringBefore("@"),
                                            resolvedName,
                                            selectedAvatarHex
                                        )
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = glass.accentStart,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .onGloballyPositioned { actionBtnCenter = it.boundsInRoot().center }
                                    .testTag("account_action_button")
                            ) {
                                Icon(
                                    imageVector = if (isEditingProfile) Icons.Rounded.Save else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEditingProfile) "Save Changes" else "Sign In & Keep Logged In",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val glass = LocalNovaGlassColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(glass.glassSurfaceStrong)
                .border(1.dp, glass.glassBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(glass.accentStart),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun parseHex(hex: String): Color {
    return runCatching {
        val clean = hex.removePrefix("#")
        val colorLong = clean.toLong(16)
        if (clean.length == 6) Color(colorLong or 0x00000000FF000000)
        else Color(colorLong)
    }.getOrDefault(Color(0xFF8B5CF6))
}
