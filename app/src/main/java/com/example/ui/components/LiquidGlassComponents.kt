package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalNovaGlassColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Living Liquid Glass ambient backdrop with subtle drifting aurora light fields
 * so transparent iPhone-style glass surfaces visibly float over living depth.
 */
@Composable
fun NovaWallpaperBackground(
    isIncognito: Boolean,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    val infinite = rememberInfiniteTransition(label = "liquid_aurora_bg")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_phase"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (glass.isDark) {
            val drawableId = if (isIncognito) {
                R.drawable.img_incognito_bg_1795429851061
            } else {
                R.drawable.img_liquid_glass_bg_1795429831472
            }
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.44f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.24f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.58f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE6EEFF),
                                Color(0xFFF2EAFF),
                                Color(0xFFE2F5FE),
                                Color(0xFFEDF2FF)
                            )
                        )
                    )
            )
        }

        // Soft drifting translucent aurora light orbs for subtle fluid depth
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val orb1 = Offset(
                x = w * (0.25f + 0.14f * cos(phase)),
                y = h * (0.22f + 0.08f * sin(phase))
            )
            val orb2 = Offset(
                x = w * (0.78f + 0.12f * sin(phase * 0.85f)),
                y = h * (0.62f + 0.10f * cos(phase * 0.85f))
            )
            val orbAlpha = if (glass.isDark) 0.16f else 0.22f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.accentStart.copy(alpha = orbAlpha), Color.Transparent),
                    center = orb1,
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = orb1
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.accentEnd.copy(alpha = orbAlpha * 0.9f), Color.Transparent),
                    center = orb2,
                    radius = w * 0.60f
                ),
                radius = w * 0.60f,
                center = orb2
            )
        }
    }
}

/**
 * Crystal-clear translucent Liquid Glass card / container with whisper-thin refractive rim.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false,
    accentBorder: Boolean = false,
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val baseFill = if (strong) glass.glassSurfaceStrong else glass.glassSurface
    val fillBrush = Brush.verticalGradient(
        colors = listOf(
            baseFill.copy(alpha = (baseFill.alpha * 1.05f).coerceAtMost(0.92f)),
            baseFill.copy(alpha = (baseFill.alpha * 0.84f).coerceAtLeast(0.18f))
        )
    )
    val borderBrush = if (accentBorder) {
        Brush.linearGradient(
            listOf(
                glass.accentStart.copy(alpha = 0.44f),
                Color.White.copy(alpha = if (glass.isDark) 0.14f else 0.65f),
                glass.accentEnd.copy(alpha = 0.36f)
            )
        )
    } else {
        Brush.verticalGradient(listOf(glass.glassBorderTop, glass.glassBorderBottom))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (glass.isDark) 0.26f else 0.09f),
                spotColor = glass.glowColor
            )
            .clip(shape)
            .background(fillBrush)
            .border(
                width = 0.9.dp,
                brush = borderBrush,
                shape = shape
            ),
        content = content
    )
}

/**
 * Transparent iPhone (iOS 26 style) Liquid Glass floating capsule:
 * True crystal translucency so live websites and aurora backdrops shine right through,
 * paired with a living, ultra-subtle prismatic glass refraction rim (zero harsh glare).
 */
@Composable
fun IphoneLiquidGlassPill(
    modifier: Modifier = Modifier,
    accentGlow: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val pillShape = RoundedCornerShape(50)

    // Subtle slow-flowing light drift around the glass edge
    val infinite = rememberInfiniteTransition(label = "liquid_rim_flow")
    val rimShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rim_shift"
    )

    val glassBodyBrush = if (glass.isDark) {
        if (glass.isIncognito) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1C1232).copy(alpha = 0.38f),
                    Color(0xFF100A20).copy(alpha = 0.50f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF141D3D).copy(alpha = 0.34f),
                    Color(0xFF0B1126).copy(alpha = 0.46f)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                Color(0xFFEFF4FF).copy(alpha = 0.32f)
            )
        )
    }

    val refractiveRimBrush = if (accentGlow) {
        Brush.linearGradient(
            colors = listOf(
                glass.accentStart.copy(alpha = 0.34f + 0.10f * rimShift),
                Color.White.copy(alpha = if (glass.isDark) 0.18f else 0.62f),
                glass.accentEnd.copy(alpha = 0.34f - 0.08f * rimShift)
            ),
            start = Offset(0f, 0f),
            end = Offset(600f * (0.4f + rimShift * 0.6f), 140f)
        )
    } else if (glass.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                glass.accentStart.copy(alpha = 0.14f + 0.06f * rimShift),
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.82f),
                Color(0xFF93C5FD).copy(alpha = 0.28f),
                Color(0xFF818CF8).copy(alpha = 0.22f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = if (glass.isDark) 0.30f else 0.12f),
                spotColor = if (accentGlow) glass.glowColor else Color.Black.copy(alpha = 0.16f)
            )
            .clip(pillShape)
            .background(glassBodyBrush)
            .drawWithContent {
                drawContent()
                // Whisper-thin inner glass refraction rim (no milky overlay or glare)
                val inset = 1.6.dp.toPx()
                val radius = size.height / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (glass.isDark) 0.09f else 0.35f),
                            Color.Transparent,
                            glass.accentStart.copy(alpha = if (glass.isDark) 0.08f else 0.10f)
                        )
                    ),
                    topLeft = Offset(inset, inset),
                    size = Size(
                        (size.width - inset * 2).coerceAtLeast(0f),
                        (size.height - inset * 2).coerceAtLeast(0f)
                    ),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 0.75.dp.toPx())
                )
            }
            .border(
                width = 0.9.dp,
                brush = refractiveRimBrush,
                shape = pillShape
            ),
        content = content
    )
}

/**
 * Smooth animated liquid-glow progress bar for live page loads.
 */
@Composable
fun LiquidProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "liquid_progress"
    )
    val shimmerTransition = rememberInfiniteTransition(label = "progress_shimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(50))
    ) {
        val barWidth = size.width * animatedProgress
        if (barWidth <= 0f) return@Canvas
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    glass.accentStart,
                    glass.accentEnd,
                    glass.accentStart
                ),
                startX = (shimmerX - 0.5f) * size.width,
                endX = (shimmerX + 0.5f) * size.width
            ),
            size = Size(barWidth, size.height),
            cornerRadius = CornerRadius(size.height, size.height)
        )
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    tint: Color? = null,
    testTagName: String? = null,
    iconSize: Dp = 22.dp
) {
    val glass = LocalNovaGlassColors.current
    val haptic = LocalHapticFeedback.current
    val resolvedTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
        tint != null -> tint
        active -> glass.accentStart
        else -> MaterialTheme.colorScheme.onSurface
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_press_scale"
    )
    val tagModifier = if (testTagName != null) Modifier.testTag(testTagName) else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(tagModifier)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .then(
                if (active) {
                    Modifier.background(glass.chipActiveBg)
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = glass.accentStart),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .semantics { this.contentDescription = contentDescription }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = resolvedTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun GlassPillChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    testTagName: String? = null
) {
    val glass = LocalNovaGlassColors.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(50)
    val tagModifier = if (testTagName != null) Modifier.testTag(testTagName) else Modifier
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val chipScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chip_press_scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .then(tagModifier)
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(
                if (active) {
                    Brush.horizontalGradient(
                        listOf(
                            glass.accentStart.copy(alpha = 0.24f),
                            glass.accentEnd.copy(alpha = 0.22f)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(glass.glassSurface, glass.glassSurface.copy(alpha = glass.glassSurface.alpha * 0.85f)))
                }
            )
            .border(
                width = 0.9.dp,
                brush = if (active) {
                    Brush.horizontalGradient(
                        listOf(
                            glass.accentStart.copy(alpha = 0.52f),
                            glass.accentEnd.copy(alpha = 0.45f)
                        )
                    )
                } else {
                    Brush.verticalGradient(listOf(glass.glassBorderTop, glass.glassBorderBottom))
                },
                shape = shape
            )
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = glass.accentStart),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) glass.accentStart else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
