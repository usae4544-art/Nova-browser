package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserAccountEntity
import com.example.ui.components.GlassIconButton
import com.example.ui.components.LiquidGlassSegmentedTabs
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidSegmentTab
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure

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
    savedAccounts: List<UserAccountEntity> = emptyList(),
    googleWebSignedIn: Boolean = false,
    googleDetectedEmail: String? = null,
    authError: String? = null,
    onClearError: () -> Unit = {},
    onDismiss: () -> Unit,
    onRegister: (
        email: String,
        username: String,
        displayName: String,
        password: String,
        avatarHex: String,
        bio: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onLoginWithPassword: (identifier: String, password: String, onSuccess: () -> Unit) -> Unit,
    onSwitchSavedAccount: (userId: String, onSuccess: () -> Unit) -> Unit = { _, _ -> },
    onLaunchGoogleSignIn: () -> Unit = {},
    onLaunchCreateGoogleAccount: () -> Unit = {},
    onOpenGoogleAccountHub: () -> Unit = {},
    onSignOutGoogleWeb: () -> Unit = {},
    onLogout: (alsoSignOutGoogle: Boolean) -> Unit,
    onUpdateProfile: (displayName: String, bio: String, avatarHex: String) -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val sakugaVfx = LocalSakugaVfx.current

    var authMode by remember { mutableStateOf("SIGNIN") } // SIGNIN | CREATE
    var emailInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf(currentAccount?.displayName.orEmpty()) }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf(currentAccount?.bio.orEmpty()) }
    var selectedAvatarHex by remember { mutableStateOf(currentAccount?.avatarColorHex ?: AVATAR_PALETTE[0]) }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var alsoLogoutGoogle by remember { mutableStateOf(false) }

    var actionBtnCenter by remember { mutableStateOf(Offset.Zero) }
    var googleBtnCenter by remember { mutableStateOf(Offset.Zero) }

    val displayedError = localError ?: authError

    Dialog(
        onDismissRequest = {
            onClearError()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(28.dp),
                strong = true,
                accentBorder = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .testTag("user_account_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
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
                                text = when {
                                    isEditingProfile -> "Edit Profile"
                                    currentAccount != null -> "Account & Google Sync"
                                    authMode == "CREATE" -> "Create Full Account"
                                    else -> "Sign In to Nova"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Close",
                            onClick = {
                                onClearError()
                                onDismiss()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentAccount != null && !isEditingProfile) {
                        // LOGGED IN ACCOUNT + LIVE GOOGLE WEB SESSION VIEW
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(parseHex(currentAccount.avatarColorHex))
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.72f), glass.accentStart)
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

                            Spacer(modifier = Modifier.height(10.dp))

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
                                    text = "@${currentAccount.username} · ${currentAccount.authProvider}",
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
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Google Browser Sign-In Status Card (for logging in everywhere across websites)
                            val isGoogleActive = googleWebSignedIn || currentAccount.googleConnected
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(glass.glassSurface)
                                    .border(1.dp, glass.glassBorder, RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        GoogleBadgeCircle()
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isGoogleActive) "Google Connected (All Sites)" else "Connect Google Account",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (isGoogleActive) MintSecure else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = googleDetectedEmail
                                                    ?: if (isGoogleActive) "Active on YouTube, Gmail & Continue with Google"
                                                    else "Sign in once via Google to stay logged in everywhere",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (isGoogleActive) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                onDismiss()
                                                onOpenGoogleAccountHub()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("Google Hub", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = onSignOutGoogleWeb,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Sign Out Google", style = MaterialTheme.typography.labelSmall, color = CoralDanger)
                                        }
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onLaunchGoogleSignIn()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = glass.accentStart,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Sign in Google", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onDismiss()
                                                onLaunchCreateGoogleAccount()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Create Google", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (googleWebSignedIn) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { alsoLogoutGoogle = !alsoLogoutGoogle }
                                ) {
                                    Checkbox(
                                        checked = alsoLogoutGoogle,
                                        onCheckedChange = { alsoLogoutGoogle = it }
                                    )
                                    Text(
                                        text = "Also clear Google login cookies when logging out",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

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
                                        onLogout(alsoLogoutGoogle)
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
                    } else if (isEditingProfile && currentAccount != null) {
                        // EDIT PROFILE VIEW
                        AvatarColorPickerRow(
                            selectedAvatarHex = selectedAvatarHex,
                            onSelect = { selectedAvatarHex = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassInputField(
                            label = "Display Name",
                            placeholder = "Your Name",
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GlassInputField(
                            label = "Bio / Tagline",
                            placeholder = "Privacy advocate & daily surfer",
                            value = bioInput,
                            onValueChange = { bioInput = it }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { isEditingProfile = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    sakugaVfx?.triggerImpactBurst(actionBtnCenter)
                                    onUpdateProfile(displayNameInput, bioInput, selectedAvatarHex)
                                    isEditingProfile = false
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
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Changes", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // LOGGED OUT: GOOGLE SIGN IN + FULL SIGN IN / CREATE ACCOUNT TABS

                        // Primary Google Account Card (Sign in via Chromium WebView so all sites stay logged in)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            glass.accentStart.copy(alpha = if (glass.isDark) 0.16f else 0.12f),
                                            glass.accentEnd.copy(alpha = if (glass.isDark) 0.14f else 0.10f)
                                        )
                                    )
                                )
                                .border(1.dp, glass.glassBorder, RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(glass.glassSurfaceStrong)
                                    .border(0.9.dp, glass.glassBorderStrong, RoundedCornerShape(14.dp))
                                    .onGloballyPositioned { googleBtnCenter = it.boundsInRoot().center }
                                    .clickable {
                                        sakugaVfx?.triggerChipAccent(googleBtnCenter)
                                        onDismiss()
                                        onLaunchGoogleSignIn()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                                    .testTag("google_signin_button")
                            ) {
                                GoogleBadgeCircle()
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Continue with Google",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Login once for YouTube, Gmail & all websites",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = glass.accentStart,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Don't have a Google account?",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Create Google Account →",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = glass.accentStart,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onDismiss()
                                            onLaunchCreateGoogleAccount()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = glass.glassBorder)
                            Text(
                                text = " OR NOVA ACCOUNT ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = glass.glassBorder)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LiquidGlassSegmentedTabs(
                            tabs = listOf(
                                LiquidSegmentTab("SIGNIN", "Sign In", Icons.Rounded.Lock, "tab_signin"),
                                LiquidSegmentTab("CREATE", "Create Account", Icons.Rounded.PersonAdd, "tab_create_account")
                            ),
                            selectedId = authMode,
                            onSelect = {
                                authMode = it
                                localError = null
                                onClearError()
                            },
                            accentGlow = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (authMode == "SIGNIN" && savedAccounts.isNotEmpty()) {
                            Text(
                                text = "Saved Accounts on Device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(savedAccounts, key = { it.userId }) { acc ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(glass.glassSurface)
                                            .border(0.8.dp, glass.glassBorder, RoundedCornerShape(50))
                                            .clickable {
                                                onSwitchSavedAccount(acc.userId) {
                                                    onDismiss()
                                                }
                                            }
                                            .padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(parseHex(acc.avatarColorHex))
                                        ) {
                                            Text(
                                                text = acc.displayName.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = acc.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (authMode == "CREATE") {
                            AvatarColorPickerRow(
                                selectedAvatarHex = selectedAvatarHex,
                                onSelect = { selectedAvatarHex = it }
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            GlassInputField(
                                label = "Full / Display Name",
                                placeholder = "e.g. Alex Verma",
                                value = displayNameInput,
                                onValueChange = { displayNameInput = it; localError = null; onClearError() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GlassInputField(
                                label = "Username",
                                placeholder = "e.g. alex_nova",
                                value = usernameInput,
                                onValueChange = { usernameInput = it; localError = null; onClearError() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GlassInputField(
                                label = "Email Address",
                                placeholder = "e.g. you@gmail.com",
                                value = emailInput,
                                keyboardType = KeyboardType.Email,
                                onValueChange = { emailInput = it; localError = null; onClearError() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GlassInputField(
                                label = "Password (min 6 characters)",
                                placeholder = "Create a strong password",
                                value = passwordInput,
                                isPassword = true,
                                showPassword = showPassword,
                                onTogglePassword = { showPassword = !showPassword },
                                onValueChange = { passwordInput = it; localError = null; onClearError() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GlassInputField(
                                label = "Confirm Password",
                                placeholder = "Re-enter password",
                                value = confirmPasswordInput,
                                isPassword = true,
                                showPassword = showPassword,
                                onTogglePassword = { showPassword = !showPassword },
                                onValueChange = { confirmPasswordInput = it; localError = null; onClearError() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            GlassInputField(
                                label = "Bio (optional)",
                                placeholder = "Nova Browser Explorer",
                                value = bioInput,
                                onValueChange = { bioInput = it }
                            )
                        } else {
                            GlassInputField(
                                label = "Email or Username",
                                placeholder = "e.g. you@gmail.com or @username",
                                value = emailInput,
                                keyboardType = KeyboardType.Email,
                                onValueChange = { emailInput = it; localError = null; onClearError() }
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            GlassInputField(
                                label = "Password",
                                placeholder = "Enter your password",
                                value = passwordInput,
                                isPassword = true,
                                showPassword = showPassword,
                                onTogglePassword = { showPassword = !showPassword },
                                onValueChange = { passwordInput = it; localError = null; onClearError() }
                            )
                        }

                        AnimatedVisibility(visible = !displayedError.isNullOrBlank()) {
                            Text(
                                text = displayedError.orEmpty(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = CoralDanger,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                localError = null
                                onClearError()
                                sakugaVfx?.triggerImpactBurst(actionBtnCenter)
                                if (authMode == "CREATE") {
                                    if (displayNameInput.isBlank()) {
                                        localError = "Please enter your display name."
                                        return@Button
                                    }
                                    if (passwordInput != confirmPasswordInput) {
                                        localError = "Passwords do not match."
                                        return@Button
                                    }
                                    onRegister(
                                        emailInput,
                                        usernameInput,
                                        displayNameInput,
                                        passwordInput,
                                        selectedAvatarHex,
                                        bioInput
                                    ) { onDismiss() }
                                } else {
                                    onLoginWithPassword(emailInput, passwordInput) { onDismiss() }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = glass.accentStart,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .onGloballyPositioned { actionBtnCenter = it.boundsInRoot().center }
                                .testTag("account_action_button")
                        ) {
                            Icon(
                                imageVector = if (authMode == "CREATE") Icons.Rounded.PersonAdd else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (authMode == "CREATE") "Create Full Account & Sign In" else "Sign In & Stay Logged In",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleBadgeCircle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0xFFDADCE0), CircleShape)
    ) {
        Text(
            text = "G",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF4285F4)
        )
    }
}

@Composable
private fun AvatarColorPickerRow(
    selectedAvatarHex: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Avatar Accent",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AVATAR_PALETTE.forEach { hex ->
                val isSelected = hex.equals(selectedAvatarHex, ignoreCase = true)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(parseHex(hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) }
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
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
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(glass.glassSurfaceStrong)
                .border(1.dp, glass.glassBorder, RoundedCornerShape(14.dp))
                .padding(start = 14.dp, end = if (isPassword) 4.dp else 14.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType
                    ),
                    visualTransformation = if (isPassword && !showPassword) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(glass.accentStart),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword && onTogglePassword != null) {
                IconButton(
                    onClick = onTogglePassword,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
