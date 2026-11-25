package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

@Immutable
data class NovaGlassColors(
    val glassSurface: Color,
    val glassSurfaceStrong: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val glowColor: Color,
    val chipActiveBg: Color,
    val isDark: Boolean,
    val isIncognito: Boolean
)

val LocalNovaGlassColors = compositionLocalOf {
    NovaGlassColors(
        glassSurface = Color(0x52121836),
        glassSurfaceStrong = Color(0x88101630),
        glassBorderTop = Color(0x3693C5FD),
        glassBorderBottom = Color(0x168B5CF6),
        accentStart = ElectricCyan,
        accentEnd = LuminousViolet,
        glowColor = Color(0x2638BDF8),
        chipActiveBg = Color(0x2B38BDF8),
        isDark = true,
        isIncognito = false
    )
}

private val NovaDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF041424),
    primaryContainer = Color(0xFF1A2552),
    onPrimaryContainer = Color(0xFFD9EEFF),
    secondary = LuminousViolet,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF281D52),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = AuroraMagenta,
    onTertiary = Color(0xFF1F0A33),
    background = DeepCosmicBg,
    onBackground = TextPrimaryDark,
    surface = DeepCosmicSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepCosmicElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF33416E),
    error = CoralDanger,
    onError = Color(0xFF2A0610)
)

private val NovaIncognitoColorScheme = darkColorScheme(
    primary = IncognitoHighlight,
    onPrimary = Color(0xFF1E0B36),
    primaryContainer = Color(0xFF331C54),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = IncognitoAccent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF291842),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = Color(0xFFC084FC),
    onTertiary = Color(0xFF1E0938),
    background = IncognitoBg,
    onBackground = TextPrimaryDark,
    surface = IncognitoSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = IncognitoElevated,
    onSurfaceVariant = Color(0xFFC4B5D9),
    outline = Color(0xFF4C3575),
    error = CoralDanger,
    onError = Color(0xFF2A0610)
)

private val NovaLightColorScheme = lightColorScheme(
    primary = FrostPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = FrostSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = FrostTertiary,
    onTertiary = Color.White,
    background = FrostLightBg,
    onBackground = FrostOnBg,
    surface = FrostLightSurface,
    onSurface = FrostOnBg,
    surfaceVariant = Color(0xFFE2E8F8),
    onSurfaceVariant = FrostOnSurfaceVariant,
    outline = Color(0xFFA5B4FC),
    error = Color(0xFFE11D48),
    onError = Color.White
)

@Composable
fun resolveEffectiveDarkTheme(
    themeMode: AppThemeMode,
    isIncognito: Boolean
): Boolean {
    val systemDark = isSystemInDarkTheme()
    return when {
        isIncognito -> true
        themeMode == AppThemeMode.DARK -> true
        themeMode == AppThemeMode.LIGHT -> false
        else -> systemDark
    }
}

@Composable
fun NovaBrowserTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    isIncognito: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = resolveEffectiveDarkTheme(themeMode = themeMode, isIncognito = isIncognito)

    // Keep Android status bar and navigation bar icon contrast synced cleanly on theme change only
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(useDark) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDark
            insetsController.isAppearanceLightNavigationBars = !useDark
        }
    }

    val colorScheme = when {
        isIncognito -> NovaIncognitoColorScheme
        useDark -> NovaDarkColorScheme
        else -> NovaLightColorScheme
    }

    val targetGlass = when {
        isIncognito -> NovaGlassColors(
            glassSurface = Color(0xC41E1338),
            glassSurfaceStrong = Color(0xF7160E29),
            glassBorderTop = Color(0x5CC084FC),
            glassBorderBottom = Color(0x2E7E22CE),
            accentStart = IncognitoAccent,
            accentEnd = Color(0xFF9333EA),
            glowColor = Color(0x3CA855F7),
            chipActiveBg = Color(0x45A855F7),
            isDark = true,
            isIncognito = true
        )
        useDark -> NovaGlassColors(
            glassSurface = Color(0xBF131D42),
            glassSurfaceStrong = Color(0xF70D1430),
            glassBorderTop = Color(0x5493C5FD),
            glassBorderBottom = Color(0x288B5CF6),
            accentStart = ElectricCyan,
            accentEnd = LuminousViolet,
            glowColor = Color(0x3838BDF8),
            chipActiveBg = Color(0x3B38BDF8),
            isDark = true,
            isIncognito = false
        )
        else -> NovaGlassColors(
            glassSurface = Color(0xDBF7FAFF),
            glassSurfaceStrong = Color(0xF8F4F8FF),
            glassBorderTop = Color(0xD8FFFFFF),
            glassBorderBottom = Color(0x52818CF8),
            accentStart = FrostSecondary,
            accentEnd = FrostTertiary,
            glowColor = Color(0x2E4338CA),
            chipActiveBg = Color(0x304338CA),
            isDark = false,
            isIncognito = false
        )
    }

    val animSpec = tween<Color>(durationMillis = 150)
    val animatedSurface by animateColorAsState(targetGlass.glassSurface, animSpec, label = "glass_surface")
    val animatedSurfaceStrong by animateColorAsState(targetGlass.glassSurfaceStrong, animSpec, label = "glass_strong")
    val animatedBorderTop by animateColorAsState(targetGlass.glassBorderTop, animSpec, label = "glass_border_top")
    val animatedBorderBottom by animateColorAsState(targetGlass.glassBorderBottom, animSpec, label = "glass_border_bottom")
    val animatedAccentStart by animateColorAsState(targetGlass.accentStart, animSpec, label = "glass_accent_start")
    val animatedAccentEnd by animateColorAsState(targetGlass.accentEnd, animSpec, label = "glass_accent_end")

    val glassColors = targetGlass.copy(
        glassSurface = animatedSurface,
        glassSurfaceStrong = animatedSurfaceStrong,
        glassBorderTop = animatedBorderTop,
        glassBorderBottom = animatedBorderBottom,
        accentStart = animatedAccentStart,
        accentEnd = animatedAccentEnd
    )

    CompositionLocalProvider(LocalNovaGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovaTypography,
            content = content
        )
    }
}
