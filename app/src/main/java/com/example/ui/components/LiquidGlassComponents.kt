package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Prevents taps, long-presses, and drags on overlays from leaking through to
 * composables beneath them in a Z-stacked Box. Attach to a background sibling layer
 * drawn beneath the overlay's own interactive controls.
 */
fun Modifier.consumeAllTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Main).changes.forEach { it.consume() }
        }
    }
}

/**
 * Living Liquid Glass ambient backdrop with subtle drifting aurora light fields
 * so transparent iPhone-style glass surfaces visibly float over living depth.
 */
@Composable
fun NovaWallpaperBackground(
    isIncognito: Boolean,
    modifier: Modifier = Modifier,
    backdropBlur: Dp = 0.dp
) {
    val glass = LocalNovaGlassColors.current
    val animatedBlur by animateDpAsState(
        targetValue = backdropBlur,
        animationSpec = tween(durationMillis = 220),
        label = "wallpaper_backdrop_blur"
    )
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

    val blurModifier = if (animatedBlur > 0.5.dp) {
        Modifier.blur(radius = animatedBlur, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxSize().then(blurModifier)) {
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
            baseFill.copy(alpha = (baseFill.alpha * 1.02f).coerceAtMost(0.98f)),
            baseFill.copy(alpha = (baseFill.alpha * (if (strong) 0.96f else 0.92f)).coerceAtLeast(0.55f))
        )
    )
    val shimmerDrift = 0.42f

    val borderBrush = if (accentBorder) {
        Brush.linearGradient(
            listOf(
                glass.accentStart.copy(alpha = 0.56f),
                Color.White.copy(alpha = if (glass.isDark) 0.24f else 0.78f),
                glass.accentEnd.copy(alpha = 0.46f)
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
                ambientColor = Color.Black.copy(alpha = if (glass.isDark) 0.34f else 0.11f),
                spotColor = glass.glowColor
            )
            .clip(shape)
            .background(fillBrush)
            .drawWithContent {
                drawContent()
                // Subtle real Liquid Glass dome specular & diagonal sheen
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (glass.isDark) 0.08f else 0.32f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = (h * 0.28f).coerceAtLeast(12f)
                    )
                )
                if (strong && w > 0f && h > 0f) {
                    val bandX = shimmerDrift * w
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (glass.isDark) 0.055f else 0.18f),
                                glass.accentStart.copy(alpha = if (glass.isDark) 0.05f else 0.06f),
                                Color.Transparent
                            ),
                            start = Offset(bandX - w * 0.35f, 0f),
                            end = Offset(bandX + w * 0.35f, h * 0.85f)
                        )
                    )
                }
            }
            .border(
                width = 0.95.dp,
                brush = borderBrush,
                shape = shape
            ),
        content = content
    )
}

private const val LIQUID_GLASS_AGSL_SHADER = """
uniform shader content;
uniform float2 uResolution;
uniform float uTime;
uniform float4 uAccent;
uniform float uIsDark;

half4 main(float2 fragCoord) {
    float2 size = max(uResolution, float2(1.0));
    float2 uv = fragCoord / size;
    float radius = min(size.x, size.y) * 0.5;
    float2 halfSize = size * 0.5;
    float2 p = fragCoord - halfSize;
    float2 q = abs(p) - (halfSize - float2(radius));
    float dist = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;

    float edgeMask = smoothstep(-20.0, 0.0, dist) * (1.0 - step(0.5, dist));
    float2 normal = normalize(p + float2(0.0001));
    half4 base = content.eval(fragCoord - normal * (edgeMask * 2.6));

    float topDome = clamp(1.0 - uv.y * 1.75, 0.0, 1.0) * (uIsDark > 0.5 ? 0.08 : 0.16);
    float sheenPos = fract(uTime * 0.14);
    float diag = uv.x * 0.72 + uv.y * 0.28;
    float delta = (diag - sheenPos * 1.4 + 0.2) * 5.0;
    float sheen = exp(-(delta * delta)) * (uIsDark > 0.5 ? 0.09 : 0.20);

    float rimGlow = edgeMask * (uIsDark > 0.5 ? 0.22 : 0.30);
    half3 prism = mix(half3(1.0), uAccent.rgb, 0.42 + 0.34 * sin(uv.x * 3.8 + uTime));

    half3 rgb = base.rgb + prism * (rimGlow + sheen) + half3(topDome);
    return half4(clamp(rgb, half3(0.0), half3(1.0)), base.a);
}
"""

/**
 * Transparent iPhone (iOS 26 style) Liquid Glass floating capsule:
 * Optical AGSL meniscus refraction shader on Android 13+ (applied purely to the glass substrate
 * so foreground text/icons stay razor-sharp), curved top dome caustic highlight, and flowing prism rim.
 */
@Composable
fun IphoneLiquidGlassPill(
    modifier: Modifier = Modifier,
    accentGlow: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val pillShape = RoundedCornerShape(50)

    // Run gentle shimmer loop ONLY on emphasized capsules so idle toolbars stay at 0% GPU redraw
    val rimShift: Float
    val sheenPhase: Float
    if (accentGlow) {
        val infinite = rememberInfiniteTransition(label = "liquid_rim_flow")
        rimShift = infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rim_shift"
        ).value
        sheenPhase = infinite.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pill_sheen_phase"
        ).value
    } else {
        rimShift = 0.35f
        sheenPhase = 0.48f
    }
    val agslShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(LIQUID_GLASS_AGSL_SHADER) }.getOrNull()
        } else {
            null
        }
    }

    val glassBodyBrush = if (glass.isDark) {
        if (glass.isIncognito) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E1338).copy(alpha = if (accentGlow) 0.94f else 0.89f),
                    Color(0xFF120B24).copy(alpha = if (accentGlow) 0.96f else 0.92f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF131D40).copy(alpha = if (accentGlow) 0.93f else 0.88f),
                    Color(0xFF0A1026).copy(alpha = if (accentGlow) 0.96f else 0.92f)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (accentGlow) 0.95f else 0.91f),
                Color(0xFFEAF1FF).copy(alpha = if (accentGlow) 0.91f else 0.87f)
            )
        )
    }

    val refractiveRimBrush = if (accentGlow) {
        Brush.linearGradient(
            colors = listOf(
                glass.accentStart.copy(alpha = 0.44f + 0.14f * rimShift),
                Color.White.copy(alpha = if (glass.isDark) 0.24f else 0.76f),
                glass.accentEnd.copy(alpha = 0.42f - 0.10f * rimShift)
            ),
            start = Offset(0f, 0f),
            end = Offset(600f * (0.4f + rimShift * 0.6f), 140f)
        )
    } else if (glass.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                glass.accentStart.copy(alpha = 0.20f + 0.08f * rimShift),
                glass.accentEnd.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFF93C5FD).copy(alpha = 0.36f),
                Color(0xFF818CF8).copy(alpha = 0.28f)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = if (glass.isDark) 0.36f else 0.14f),
                spotColor = if (accentGlow) glass.glowColor else Color.Black.copy(alpha = 0.18f)
            )
            .clip(pillShape)
            .border(
                width = 1.0.dp,
                brush = refractiveRimBrush,
                shape = pillShape
            )
    ) {
        // Pure glass substrate layer: receives AGSL meniscus refraction on API 33+ without distorting text
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(glassBodyBrush)
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslShader != null) {
                        val applied = runCatching {
                            agslShader.setFloatUniform(
                                "uResolution",
                                size.width.coerceAtLeast(1f),
                                size.height.coerceAtLeast(1f)
                            )
                            agslShader.setFloatUniform("uTime", (sheenPhase + 0.35f) * 3.7f)
                            agslShader.setFloatUniform(
                                "uAccent",
                                glass.accentStart.red,
                                glass.accentStart.green,
                                glass.accentStart.blue,
                                1f
                            )
                            agslShader.setFloatUniform("uIsDark", if (glass.isDark) 1f else 0f)
                            RenderEffect
                                .createRuntimeShaderEffect(agslShader, "content")
                                .asComposeRenderEffect()
                        }.getOrNull()
                        if (applied != null) {
                            renderEffect = applied
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    val w = size.width
                    val inset = 1.5.dp.toPx()
                    val radius = size.height / 2f
                    if (w > 0f) {
                        val cx = sheenPhase * w
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (glass.isDark) 0.06f else 0.18f),
                                    glass.accentStart.copy(alpha = if (glass.isDark) 0.05f else 0.07f),
                                    Color.Transparent
                                ),
                                startX = cx - w * 0.28f,
                                endX = cx + w * 0.28f
                            )
                        )
                    }
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (glass.isDark) 0.16f else 0.52f),
                                Color.Transparent,
                                glass.accentStart.copy(alpha = if (glass.isDark) 0.14f else 0.16f)
                            )
                        ),
                        topLeft = Offset(inset, inset),
                        size = Size(
                            (size.width - inset * 2).coerceAtLeast(0f),
                            (size.height - inset * 2).coerceAtLeast(0f)
                        ),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = 0.85.dp.toPx())
                    )
                }
        )
        content()
    }
}

data class LiquidSegmentTab(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val testTagName: String? = null
)

/**
 * Authentic iPhone iOS 26 gliding Liquid Lens segmented tab bar:
 * A frosted magnifying glass lens stretches softly with horizontal velocity between tabs,
 * settles with an underdamped spring bounce, and triggers crisp Sakuga sparkles on selection.
 */
@Composable
fun LiquidGlassSegmentedTabs(
    tabs: List<LiquidSegmentTab>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentGlow: Boolean = false,
    useThemeSplash: Boolean = false
) {
    val glass = LocalNovaGlassColors.current
    val haptic = LocalHapticFeedback.current
    val sakugaVfx = LocalSakugaVfx.current
    val density = LocalDensity.current
    val slotBounds = remember { mutableStateMapOf<String, Pair<Float, Float>>() }
    val targetBounds = slotBounds[selectedId]

    val lensX by animateFloatAsState(
        targetValue = targetBounds?.first ?: 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "liquid_lens_x"
    )
    val lensW by animateFloatAsState(
        targetValue = targetBounds?.second ?: 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 360f),
        label = "liquid_lens_w"
    )

    val distPx = if (targetBounds != null) abs(targetBounds.first - lensX) else 0f
    val maxStretchPx = with(density) { 18.dp.toPx() }
    val stretchPx = (distPx * 0.24f).coerceIn(0f, maxStretchPx)
    val stretchLeft = if (targetBounds != null && targetBounds.first < lensX) stretchPx * 0.5f else 0f
    val glideLiftScale = 1f + (stretchPx / with(density) { 96.dp.toPx() }).coerceIn(0f, 0.08f)

    IphoneLiquidGlassPill(
        accentGlow = accentGlow,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            if (lensW > 2f) {
                Box(modifier = Modifier.matchParentSize()) {
                    val lensWidthDp = with(density) { (lensW + stretchPx).toDp() }
                    val pillShape = RoundedCornerShape(50)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((lensX - stretchLeft).roundToInt(), 0) }
                            .width(lensWidthDp)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleY = glideLiftScale
                            }
                            .clip(pillShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        glass.accentStart.copy(alpha = if (glass.isDark) 0.34f else 0.24f),
                                        glass.accentEnd.copy(alpha = if (glass.isDark) 0.28f else 0.18f)
                                    )
                                )
                            )
                            .border(
                                width = 0.9.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (glass.isDark) 0.38f else 0.85f),
                                        glass.accentStart.copy(alpha = 0.45f)
                                    )
                                ),
                                shape = pillShape
                            )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { tab ->
                    key(tab.id) {
                        val isSelected = tab.id == selectedId
                        var itemCenterInRoot by remember { mutableStateOf(Offset.Zero) }
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val pressY by animateFloatAsState(
                            targetValue = if (isPressed) 0.90f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "seg_tab_press"
                        )
                        val (sqX, sqY) = sakugaSquashScale(pressY, wideFactor = 0.42f)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .then(if (tab.testTagName != null) Modifier.testTag(tab.testTagName) else Modifier)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInParent()
                                    slotBounds[tab.id] = pos.x to coords.size.width.toFloat()
                                    itemCenterInRoot = coords.boundsInRoot().center
                                }
                                .graphicsLayer {
                                    scaleX = sqX
                                    scaleY = sqY
                                }
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(
                                    role = Role.Tab,
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = true, color = glass.accentStart),
                                    onClick = {
                                        if (useThemeSplash) {
                                            sakugaVfx?.triggerThemeSplash(itemCenterInRoot)
                                        } else {
                                            sakugaVfx?.triggerChipAccent(itemCenterInRoot)
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelect(tab.id)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            if (tab.icon != null) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) glass.accentStart else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
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
    val sakugaVfx = LocalSakugaVfx.current
    var centerInRoot by remember { mutableStateOf(Offset.Zero) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_press_scale"
    )
    val (squashX, squashY) = sakugaSquashScale(pressScale, wideFactor = 0.45f)
    val tagModifier = if (testTagName != null) Modifier.testTag(testTagName) else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(tagModifier)
            .onGloballyPositioned { coords ->
                centerInRoot = coords.boundsInRoot().center
            }
            .graphicsLayer {
                scaleX = squashX
                scaleY = squashY
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
                    sakugaVfx?.triggerMicroSpark(centerInRoot)
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
    val sakugaVfx = LocalSakugaVfx.current
    var centerInRoot by remember { mutableStateOf(Offset.Zero) }
    val chipScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chip_press_scale"
    )
    val (chipSquashX, chipSquashY) = sakugaSquashScale(chipScale, wideFactor = 0.52f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .then(tagModifier)
            .onGloballyPositioned { coords ->
                centerInRoot = coords.boundsInRoot().center
            }
            .graphicsLayer {
                scaleX = chipSquashX
                scaleY = chipSquashY
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
                    sakugaVfx?.triggerChipAccent(centerInRoot)
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

/**
 * Horizontal scrolling LazyRow with soft frosted-blur and alpha-feathered edge dissolve.
 * Items glide softly into frosted glass edges when scrolling sideways instead of clipping flatly.
 */
@Composable
fun LiquidEdgeBlurLazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    edgeFadeWidth: Dp = 38.dp,
    content: LazyListScope.() -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val leftEdgeFraction by animateFloatAsState(
        targetValue = if (state.canScrollBackward) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "edge_blur_left"
    )
    val rightEdgeFraction by animateFloatAsState(
        targetValue = if (state.canScrollForward) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "edge_blur_right"
    )
    val edgeTint = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = state,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fadePx = edgeFadeWidth.toPx().coerceAtMost(size.width * 0.25f)
                    if (fadePx > 1f && leftEdgeFraction > 0.01f) {
                        val w = fadePx * leftEdgeFraction
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.45f to Color.Black.copy(alpha = 0.52f),
                                1f to Color.Black,
                                startX = 0f,
                                endX = w
                            ),
                            topLeft = Offset.Zero,
                            size = Size(w, size.height),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    if (fadePx > 1f && rightEdgeFraction > 0.01f) {
                        val w = fadePx * rightEdgeFraction
                        val startX = (size.width - w).coerceAtLeast(0f)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Black,
                                0.55f to Color.Black.copy(alpha = 0.52f),
                                1f to Color.Transparent,
                                startX = startX,
                                endX = size.width
                            ),
                            topLeft = Offset(startX, 0f),
                            size = Size(w, size.height),
                            blendMode = BlendMode.DstIn
                        )
                    }
                },
            content = content
        )

        Box(modifier = Modifier.matchParentSize()) {
            if (leftEdgeFraction > 0.02f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(edgeFadeWidth * leftEdgeFraction)
                        .fillMaxHeight()
                        .blur(radius = 8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    edgeTint.copy(alpha = 0.72f * leftEdgeFraction),
                                    glass.glassSurface.copy(alpha = 0.28f * leftEdgeFraction),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            if (rightEdgeFraction > 0.02f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(edgeFadeWidth * rightEdgeFraction)
                        .fillMaxHeight()
                        .blur(radius = 8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    glass.glassSurface.copy(alpha = 0.28f * rightEdgeFraction),
                                    edgeTint.copy(alpha = 0.72f * rightEdgeFraction)
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Animated Liquid Glass bottom floating toast for theme/dark-mode toggles,
 * VPN/Shield status alerts, bookmarks, and settings confirmations.
 */
@Composable
fun LiquidGlassFloatingToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var latestMessage by remember { mutableStateOf("") }
    var lastTriggeredToast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            latestMessage = message
        } else {
            lastTriggeredToast = null
        }
    }

    val blurRadius by animateDpAsState(
        targetValue = if (message != null) 0.dp else 10.dp,
        animationSpec = tween(durationMillis = 200),
        label = "toast_blur_transition"
    )

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { it } + fadeIn(tween(220)) + scaleIn(
            initialScale = 0.86f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing)
        ) { it / 2 } + fadeOut(tween(190)) + scaleOut(
            targetScale = 0.90f,
            animationSpec = tween(190)
        ),
        modifier = modifier
    ) {
        val textToShow = message ?: latestMessage
        if (textToShow.isBlank()) return@AnimatedVisibility

        val glass = LocalNovaGlassColors.current
        val lower = textToShow.lowercase()
        val (badgeIcon, badgeColor) = when {
            lower.contains("dark mode") || lower.contains("theme: dark") -> Icons.Rounded.DarkMode to glass.accentStart
            lower.contains("theme: light") -> Icons.Rounded.LightMode to AmberWarning
            lower.contains("theme: system") -> Icons.Rounded.BrightnessAuto to glass.accentEnd
            lower.contains("top floating bar") || lower.contains("bottom floating dock") -> Icons.Rounded.ViewAgenda to glass.accentStart
            lower.contains("vpn") -> Icons.Rounded.VpnLock to glass.accentStart
            lower.contains("shield") || lower.contains("ad") -> Icons.Rounded.Shield to MintSecure
            lower.contains("history") || lower.contains("cleared") || lower.contains("burned") -> Icons.Rounded.DeleteSweep to CoralDanger
            lower.contains("bookmark") -> Icons.Rounded.Bookmark to glass.accentStart
            lower.contains("fingerprint") || lower.contains("biometric") -> Icons.Rounded.Fingerprint to glass.accentEnd
            lower.contains("pin") || lower.contains("vault") || lower.contains("private") -> Icons.Rounded.Lock to glass.accentStart
            lower.contains("notification") -> Icons.Rounded.NotificationsActive to glass.accentStart
            lower.contains("denied") || lower.contains("failed") || lower.contains("error") || lower.contains("invalid") || lower.contains("disabled") || lower.contains("blocked") -> Icons.Rounded.Warning to AmberWarning
            else -> Icons.Rounded.CheckCircle to MintSecure
        }

        val shimmer = rememberInfiniteTransition(label = "toast_liquid_shimmer")
        val sweep by shimmer.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "toast_shimmer_sweep"
        )

        val blurMod = if (blurRadius > 0.5.dp) {
            Modifier.blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier
        }

        val sakugaVfx = LocalSakugaVfx.current
        var pillCenterInRoot by remember { mutableStateOf(Offset.Zero) }
        var pillWidthPx by remember { mutableStateOf(0f) }

        LaunchedEffect(message, pillCenterInRoot, pillWidthPx) {
            if (!message.isNullOrBlank() && pillWidthPx > 0f && lastTriggeredToast != message) {
                lastTriggeredToast = message
                sakugaVfx?.triggerToastLanding(pillCenterInRoot, pillWidthPx)
            }
        }

        IphoneLiquidGlassPill(
            accentGlow = true,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .onGloballyPositioned { coords ->
                    val bounds = coords.boundsInRoot()
                    pillCenterInRoot = bounds.center
                    pillWidthPx = bounds.width
                }
                .then(blurMod)
                .clickable(onClick = onDismiss)
                .testTag("liquid_glass_floating_toast")
        ) {
            Box(
                modifier = Modifier
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (glass.isDark) 0.08f else 0.22f),
                                    glass.accentStart.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startX = (sweep - 0.28f) * w,
                                endX = (sweep + 0.28f) * w
                            )
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                AnimatedContent(
                    targetState = textToShow,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically { it / 3 })
                            .togetherWith(fadeOut(tween(140)) + slideOutVertically { -it / 3 })
                    },
                    label = "toast_text_swap"
                ) { currentMsg ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentMsg,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
