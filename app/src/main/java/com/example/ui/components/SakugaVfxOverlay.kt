package com.example.ui.components

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IncognitoAccent
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.NovaGlassColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private const val TAU = (PI * 2.0).toFloat()

/**
 * Theme-adaptive two-tone + outline + accent palette inspired by Sakuga VFX:
 * Light Mode preserves daylight paper-white & indigo anime styling;
 * Dark Mode & Incognito shift puffs/shards into dark cosmic / obsidian tones
 * rimmed with glowing cyan/violet outlines and crisp accent sparks.
 */
data class SakugaPalette(
    val fill: Color,
    val shade: Color,
    val line: Color,
    val accent: Color,
    val sparkleFill: Color,
    val isDark: Boolean
) {
    companion object {
        fun forTheme(isDark: Boolean, isIncognito: Boolean): SakugaPalette = when {
            isIncognito -> SakugaPalette(
                fill = Color(0xFF21143D),
                shade = Color(0xFF421C78),
                line = Color(0xFFC084FC),
                accent = IncognitoAccent,
                sparkleFill = Color(0xFFF3E8FF),
                isDark = true
            )
            isDark -> SakugaPalette(
                fill = Color(0xFF172247),
                shade = Color(0xFF0C1229),
                line = ElectricCyan,
                accent = Color(0xFFFF5FA8),
                sparkleFill = Color(0xFFE0F2FE),
                isDark = true
            )
            else -> SakugaPalette(
                fill = Color(0xFFFFFFFF),
                shade = Color(0xFFB8B3FF),
                line = Color(0xFF5A4FD8),
                accent = Color(0xFFFF5FA8),
                sparkleFill = Color(0xFFFFFFFF),
                isDark = false
            )
        }

        fun fromGlassColors(glass: NovaGlassColors): SakugaPalette =
            forTheme(isDark = glass.isDark, isIncognito = glass.isIncognito)
    }
}

private fun clamp01(v: Float): Float = v.coerceIn(0f, 1f)
private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t
private fun rndF(a: Float, b: Float): Float = a + Random.nextFloat() * (b - a)
private fun outCubic(t: Float): Float = 1f - (1f - t).pow(3)
private fun inCubic(t: Float): Float = t * t * t
private fun outBack(t: Float): Float {
    val u = t - 1f
    return 1f + 2.70158f * u * u * u + 1.70158f * u * u
}

/**
 * Computes volume-preserving Sakuga squash & stretch scales from a pressed Y-scale fraction.
 */
fun sakugaSquashScale(pressY: Float, wideFactor: Float = 0.55f): Pair<Float, Float> {
    val y = pressY.coerceIn(0.65f, 1.35f)
    val x = (1f + (1f - y) * wideFactor).coerceIn(0.75f, 1.35f)
    return x to y
}

private sealed class SakugaParticle(
    val life: Float,
    var age: Float = 0f
) {
    abstract fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette)
}

private data class PuffLobe(
    val angle: Float,
    val distDp: Float,
    val radiusDp: Float,
    val phase: Float,
    val speed: Float
)

private class PuffParticle(
    val x: Float,
    val y: Float,
    val vxDp: Float,
    val vyDp: Float,
    val sizeDp: Float,
    val lobes: List<PuffLobe>,
    val drawArcs: Boolean,
    life: Float
) : SakugaParticle(life) {
    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val grow = outBack(clamp01(t / 0.22f)).coerceAtLeast(0f)
        val die = 1f - inCubic(clamp01((k - 0.55f) / 0.45f))
        if (grow * die <= 0.02f) return

        val px = x + vxDp * density * outCubic(k) * life
        val py = y + vyDp * density * outCubic(k) * life
        val unionPath = Path()
        val circles = ArrayList<Triple<Float, Float, Float>>(lobes.size)

        for (l in lobes) {
            val spreadDist = l.distDp * density * (0.5f + outCubic(k) * 0.9f)
            val cx = px + cos(l.angle + t * 0.9f * l.speed) * spreadDist
            val cy = py + sin(l.angle + t * 0.9f * l.speed) * spreadDist - outCubic(k) * 5f * density * l.speed
            val r = l.radiusDp * density * grow * die * (1f + sin(t * 9f + l.phase) * 0.05f)
            if (r > 0.8f) {
                circles.add(Triple(cx, cy, r))
                unionPath.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
            }
        }
        if (circles.isEmpty()) return

        val baseRadius = circles[0].third
        val strokeW = 1.5f * density

        drawPath(
            path = unionPath,
            color = p.line,
            style = Stroke(width = strokeW * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        clipPath(unionPath) {
            drawRect(color = p.shade, topLeft = Offset(px - 220f * density, py - 220f * density), size = Size(440f * density, 440f * density))
            translate(left = baseRadius * 0.14f, top = -baseRadius * 0.36f) {
                drawPath(path = unionPath, color = p.fill)
            }
            if (drawArcs) {
                val arcPath = Path()
                for ((cx, cy, rr) in circles) {
                    if (rr > 3f * density) {
                        arcPath.addArc(
                            oval = Rect(cx - rr, cy - rr, cx + rr, cy + rr),
                            startAngleDegrees = -155f,
                            sweepAngleDegrees = 126f
                        )
                    }
                }
                drawPath(
                    path = arcPath,
                    color = p.line,
                    style = Stroke(width = strokeW * 0.75f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

private data class BurstPin(val angle: Float, val lengthMult: Float, val halfWidthRad: Float)

private class BurstParticle(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val spikes: List<Float>,
    val rotation: Float,
    val pins: List<BurstPin>,
    life: Float
) : SakugaParticle(life) {
    private fun starPath(cx: Float, cy: Float, rPx: Float, turn: Float, innerMult: Float): Path {
        val path = Path()
        for (i in spikes.indices) {
            val a = rotation + turn + (i.toFloat() / spikes.size) * TAU
            val m = spikes[i]
            val rr = rPx * (if (i % 2 == 1) m * innerMult else m)
            val px = cx + cos(a) * rr
            val py = cy + sin(a) * rr
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }

    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val rPx = radiusDp * density * outCubic(clamp01(k / 0.30f))
        if (rPx <= 1f) return
        val hollow = 1f + inCubic(clamp01((k - 0.35f) / 0.65f)) * 1.3f
        val r0 = rPx * 0.6f * min(1.9f, hollow)

        val pinsPath = Path()
        for (n in pins) {
            val l = rPx * n.lengthMult
            if (l <= r0) continue
            pinsPath.moveTo(x + cos(n.angle - n.halfWidthRad) * r0, y + sin(n.angle - n.halfWidthRad) * r0)
            pinsPath.lineTo(x + cos(n.angle) * l, y + sin(n.angle) * l)
            pinsPath.lineTo(x + cos(n.angle + n.halfWidthRad) * r0, y + sin(n.angle + n.halfWidthRad) * r0)
            pinsPath.close()
        }
        drawPath(path = pinsPath, color = p.accent)

        val bigStar = starPath(x, y, rPx * 1.14f, 0.19f, hollow)
        drawPath(path = bigStar, color = p.shade)
        drawPath(path = bigStar, color = p.line, style = Stroke(width = 1.2f * density, join = StrokeJoin.Round))

        val frontStar = starPath(x, y, rPx * 0.82f, 0f, hollow)
        val frontFill = if (p.isDark) LuminousViolet.copy(alpha = 0.86f) else p.fill
        drawPath(path = frontStar, color = p.line, style = Stroke(width = 2.6f * density, join = StrokeJoin.Round))
        clipPath(frontStar) {
            drawPath(path = frontStar, color = p.shade)
            translate(left = -2.2f * density, top = -2.8f * density) {
                drawPath(path = frontStar, color = frontFill)
            }
        }
    }
}

private class ShardParticle(
    val x: Float,
    val y: Float,
    val angle: Float,
    val speedDp: Float,
    val sizeDp: Float,
    val spin: Float,
    val gravityDp: Float,
    val pts: List<Pair<Float, Float>>,
    life: Float
) : SakugaParticle(life) {
    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val sc = sizeDp * density * (1f - inCubic(clamp01((k - 0.60f) / 0.40f)))
        if (sc <= 0.7f) return

        val speedPx = speedDp * density
        val px = x + cos(angle) * speedPx * t
        val py = y + sin(angle) * speedPx * t + gravityDp * density * t * t
        val path = Path()
        for (i in pts.indices) {
            val u = pts[i].first * sc
            val v = pts[i].second * sc
            if (i == 0) path.moveTo(u, v) else path.lineTo(u, v)
        }
        path.close()

        translate(left = px, top = py) {
            rotateRad(radians = spin * t + angle, pivot = Offset.Zero) {
                val strokeW = 1.1f * density
                drawPath(path = path, color = p.line, style = Stroke(width = strokeW * 2f, join = StrokeJoin.Round))
                clipPath(path) {
                    drawRect(color = p.shade, topLeft = Offset(-sc * 3f, -sc * 3f), size = Size(sc * 6f, sc * 6f))
                    translate(left = -sc * 0.32f, top = -sc * 0.45f) {
                        drawPath(path = path, color = p.fill)
                    }
                }
                if (sc > 2.4f * density) {
                    drawLine(
                        color = p.line,
                        start = Offset(pts[1].first * sc, pts[1].second * sc),
                        end = Offset(pts[4].first * sc, pts[4].second * sc),
                        strokeWidth = 0.9f * density
                    )
                }
            }
        }
    }
}

private class RingParticle(
    val x: Float,
    val y: Float,
    val rxDp: Float,
    val ryDp: Float,
    val widthDp: Float,
    life: Float
) : SakugaParticle(life) {
    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val s = outCubic(k)
        val lw = widthDp * density * (1f - k)
        if (lw <= 0.35f) return

        val rx = rxDp * density * (0.32f + s)
        val ry = ryDp * density * (0.32f + s)
        val rect = Rect(x - rx, y - ry, x + rx, y + ry)
        val oval = Path().apply { addOval(rect) }
        drawPath(path = oval, color = p.line, style = Stroke(width = lw + 2.2f * density))
        drawPath(path = oval, color = if (p.isDark) p.sparkleFill else p.fill, style = Stroke(width = lw))
    }
}

private class TicksParticle(
    val x: Float,
    val y: Float,
    val count: Int,
    val radiusDp: Float,
    val lengthDp: Float,
    val dir: Float,
    val fan: Float,
    life: Float
) : SakugaParticle(life) {
    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val a = outCubic(clamp01(k / 0.50f))
        val b = inCubic(clamp01((k - 0.40f) / 0.60f))
        val rPx = radiusDp * density
        val lenPx = lengthDp * density
        val path = Path()
        for (i in 0 until count) {
            val an = dir + (i - (count - 1) / 2f) * fan
            val r0 = rPx + lenPx * b * 1.25f
            val r1 = rPx + lenPx * a * 1.25f
            if (r1 - r0 < 0.7f * density) continue
            val hw = 0.07f + (2.2f * density) / (r1 + 8f * density)
            path.moveTo(x + cos(an) * r0, y + sin(an) * r0)
            path.lineTo(x + cos(an - hw) * r1, y + sin(an - hw) * r1)
            path.lineTo(x + cos(an + hw) * r1, y + sin(an + hw) * r1)
            path.close()
        }
        drawPath(path = path, color = p.accent)
    }
}

private class SpeedLinesParticle(
    val x: Float,
    val y: Float,
    val dir: Float,
    val lenDp: Float,
    val offsetDp: Float,
    val delayFrac: Float,
    val widthDp: Float,
    life: Float
) : SakugaParticle(life) {
    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val kk = clamp01((k - delayFrac) / (1f - delayFrac))
        if (kk <= 0f) return
        val a = outCubic(kk)
        val b = inCubic(kk)
        val lw = widthDp * density * (1f - b)
        if (lw <= 0.2f) return

        val lPx = lenDp * density
        val oPx = offsetDp * density
        val px = x - sin(dir) * oPx
        val py = y + cos(dir) * oPx
        val sDist = lPx * b * 1.4f
        val eDist = lPx * (0.2f + a * 1.2f)
        drawLine(
            color = p.line,
            start = Offset(px + cos(dir) * sDist, py + sin(dir) * sDist),
            end = Offset(px + cos(dir) * eDist, py + sin(dir) * eDist),
            strokeWidth = lw,
            cap = StrokeCap.Round
        )
    }
}

private class WobParticle(
    val x: Float,
    val y: Float,
    val dirSign: Float,
    val radiusDp: Float,
    life: Float
) : SakugaParticle(life) {
    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val lw = 2.0f * density * (1f - k)
        if (lw <= 0.25f) return
        val rBase = radiusDp * density
        val cx = x - dirSign * rBase
        val cy = y
        val startDeg = if (dirSign > 0f) -26f else 154f
        val path = Path()
        for (dDp in floatArrayOf(0f, 7f)) {
            val rr = rBase + (dDp + outCubic(k) * 8f) * density
            path.addArc(
                oval = Rect(cx - rr, cy - rr, cx + rr, cy + rr),
                startAngleDegrees = startDeg,
                sweepAngleDegrees = 52f
            )
        }
        drawPath(path = path, color = p.line, style = Stroke(width = lw, cap = StrokeCap.Round))
    }
}

private class SlashParticle(
    val x: Float,
    val y: Float,
    val lengthDp: Float,
    val angle: Float,
    val thickDp: Float,
    life: Float
) : SakugaParticle(life) {
    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val head = outCubic(clamp01(k / 0.42f))
        val tail = inCubic(clamp01((k - 0.22f) / 0.78f))
        if (head - tail < 0.02f) return

        val r = lengthDp * density * 0.9f
        val baseAngle = angle - (PI / 2.0).toFloat()
        val cx = x - cos(baseAngle) * r * 0.86f
        val cy = y - sin(baseAngle) * r * 0.86f
        val a0 = baseAngle - 0.62f
        val sweep = 1.24f

        fun crescentPath(thicknessPx: Float): Path {
            val path = Path()
            val segments = 22
            for (i in 0..segments) {
                val u = lerpF(tail, head, i.toFloat() / segments)
                val a = a0 + sweep * u
                val px = cx + cos(a) * r
                val py = cy + sin(a) * r
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            for (i in segments downTo 0) {
                val q = i.toFloat() / segments
                val u = lerpF(tail, head, q)
                val a = a0 + sweep * u
                val rr = r - thicknessPx * sin(q * PI.toFloat()).coerceAtLeast(0f).pow(0.7f)
                path.lineTo(cx + cos(a) * rr, cy + sin(a) * rr)
            }
            path.close()
            return path
        }

        val outer = crescentPath(thickDp * density)
        drawPath(path = outer, color = p.accent)
        drawPath(path = crescentPath(thickDp * density * 0.42f), color = p.sparkleFill)
        drawPath(path = outer, color = p.line, style = Stroke(width = 1.3f * density, join = StrokeJoin.Round))
    }
}

private class KiraParticle(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    life: Float
) : SakugaParticle(life) {
    override fun DrawScope.draw(k: Float, t: Float, density: Float, p: SakugaPalette) {
        val s = radiusDp * density * sin(k * PI.toFloat()).coerceAtLeast(0f).pow(0.6f)
        if (s <= 0.6f) return
        val q = s * 0.16f
        val path = Path().apply {
            moveTo(0f, -s)
            quadraticTo(q, -q, s * 0.72f, 0f)
            quadraticTo(q, q, 0f, s)
            quadraticTo(-q, q, -s * 0.72f, 0f)
            quadraticTo(-q, -q, 0f, -s)
            close()
        }
        translate(left = x, top = y) {
            rotateRad(radians = sin(t * 3f) * 0.22f, pivot = Offset.Zero) {
                drawPath(path = path, color = p.sparkleFill)
                drawPath(path = path, color = p.accent, style = Stroke(width = 1.8f * density, join = StrokeJoin.Round))
            }
        }
    }
}

@Stable
class SakugaVfxController(
    private val reducedMotion: Boolean
) {
    private val particles = mutableListOf<SakugaParticle>()
    internal var overlayOriginInRoot: Offset = Offset.Zero
    private var tickCount by mutableIntStateOf(0)
    internal var hasActiveEffects by mutableStateOf(false)
        private set

    private fun toLocal(rootPos: Offset): Offset = rootPos - overlayOriginInRoot

    private fun addParticle(particle: SakugaParticle) {
        if (reducedMotion) return
        while (particles.size >= 64) {
            particles.removeAt(0)
        }
        particles.add(particle)
        hasActiveEffects = true
    }

    internal fun step(dt: Float) {
        if (particles.isEmpty()) {
            hasActiveEffects = false
            return
        }
        var i = particles.size - 1
        while (i >= 0) {
            val item = particles[i]
            item.age += dt
            if (item.age >= item.life) {
                particles.removeAt(i)
            }
            i--
        }
        tickCount++
        if (particles.isEmpty()) {
            hasActiveEffects = false
        }
    }

    internal fun drawAll(scope: DrawScope, density: Float, palette: SakugaPalette) {
        @Suppress("UNUSED_VARIABLE")
        val frameRead = tickCount
        for (i in 0 until particles.size) {
            val item = particles[i]
            val k = clamp01(item.age / item.life)
            with(item) {
                scope.draw(k = k, t = item.age, density = density, p = palette)
            }
        }
    }

    /** Crisp wedges + sparkle glint + trailing puff for quick chips and menu tiles. */
    fun triggerChipAccent(rootCenter: Offset) {
        if (reducedMotion) return
        val c = toLocal(rootCenter)
        addParticle(
            TicksParticle(
                x = c.x,
                y = c.y,
                count = 4,
                radiusDp = 18f,
                lengthDp = 11f,
                dir = -0.82f,
                fan = 0.42f,
                life = 0.40f
            )
        )
        addParticle(
            KiraParticle(
                x = c.x + rndF(14f, 28f),
                y = c.y - rndF(12f, 22f),
                radiusDp = 10.5f,
                life = 0.48f
            )
        )
        spawnPuff(
            x = c.x - 16f,
            y = c.y + 8f,
            n = 2,
            sizeDp = 6.5f,
            vxDp = -18f,
            vyDp = -8f,
            life = 0.46f
        )
    }

    /** Lightweight ring + glint + radial ticks for toolbar icon buttons, shortcuts, and switch flips. */
    fun triggerMicroSpark(rootCenter: Offset) {
        if (reducedMotion) return
        val c = toLocal(rootCenter)
        addParticle(
            RingParticle(
                x = c.x,
                y = c.y,
                rxDp = 21f,
                ryDp = 21f,
                widthDp = 3.0f,
                life = 0.36f
            )
        )
        addParticle(
            TicksParticle(
                x = c.x,
                y = c.y,
                count = 3,
                radiusDp = 16f,
                lengthDp = 9f,
                dir = -1.1f,
                fan = 0.55f,
                life = 0.34f
            )
        )
        addParticle(
            KiraParticle(
                x = c.x + rndF(-14f, 18f),
                y = c.y - rndF(10f, 18f),
                radiusDp = 9.5f,
                life = 0.46f
            )
        )
    }

    /** Theme & Shield splash: layered rings, crystal shards, ticks crown, and cosmic glints. */
    fun triggerThemeSplash(rootCenter: Offset) {
        if (reducedMotion) return
        val c = toLocal(rootCenter)
        addParticle(
            RingParticle(
                x = c.x,
                y = c.y,
                rxDp = 28f,
                ryDp = 28f,
                widthDp = 3.8f,
                life = 0.44f
            )
        )
        addParticle(
            RingParticle(
                x = c.x,
                y = c.y,
                rxDp = 40f,
                ryDp = 40f,
                widthDp = 2.4f,
                life = 0.54f
            )
        )
        addParticle(
            TicksParticle(
                x = c.x,
                y = c.y,
                count = 5,
                radiusDp = 20f,
                lengthDp = 13f,
                dir = (-PI / 2f).toFloat(),
                fan = 0.50f,
                life = 0.44f
            )
        )
        spawnShards(c.x, c.y, count = 7, speedDp = 155f, sizeDp = 4.2f, life = 0.62f)
        addParticle(KiraParticle(x = c.x - 24f, y = c.y - 18f, radiusDp = 10.5f, life = 0.55f))
        addParticle(KiraParticle(x = c.x + 26f, y = c.y - 12f, radiusDp = 9f, life = 0.52f))
        addParticle(KiraParticle(x = c.x + 4f, y = c.y + 18f, radiusDp = 7.5f, life = 0.48f))
    }

    /**
     * Signature Sakuga impact burst: racing slash crescent, spiky two-tone star,
     * accent needles, crystal shards, elliptical shock ring, wobble arcs, and corner puffs.
     */
    fun triggerImpactBurst(rootCenter: Offset, widthPx: Float = 0f) {
        if (reducedMotion) return
        val c = toLocal(rootCenter)
        val spikes = List(22) { idx ->
            if (idx % 2 == 1) rndF(0.35f, 0.50f) else rndF(0.76f, 1.14f)
        }
        val pins = List(7) { idx ->
            BurstPin(
                angle = idx / 7f * TAU + rndF(0f, 0.48f),
                lengthMult = rndF(1.22f, 1.65f),
                halfWidthRad = rndF(0.03f, 0.055f)
            )
        }
        addParticle(
            SlashParticle(
                x = c.x + 12f,
                y = c.y - 6f,
                lengthDp = 72f,
                angle = -0.45f,
                thickDp = 11f,
                life = 0.36f
            )
        )
        addParticle(
            BurstParticle(
                x = c.x,
                y = c.y,
                radiusDp = 42f,
                spikes = spikes,
                rotation = rndF(0f, TAU),
                pins = pins,
                life = 0.38f
            )
        )
        addParticle(
            RingParticle(
                x = c.x,
                y = c.y + 8f,
                rxDp = 44f,
                ryDp = 14f,
                widthDp = 4.4f,
                life = 0.46f
            )
        )
        val halfSpan = if (widthPx > 0f) min(widthPx * 0.36f, 120f) else 46f
        for (sign in floatArrayOf(-1f, 1f)) {
            spawnPuff(
                x = c.x + sign * halfSpan,
                y = c.y + 6f,
                n = 4,
                sizeDp = 10f,
                vxDp = sign * 28f,
                vyDp = -16f,
                life = 0.64f
            )
            addParticle(
                WobParticle(
                    x = c.x + sign * (halfSpan * 0.7f),
                    y = c.y,
                    dirSign = sign,
                    radiusDp = 15f,
                    life = 0.42f
                )
            )
        }
        spawnShards(c.x, c.y - 6f, count = 7, speedDp = 185f, sizeDp = 4.6f, life = 0.68f)
    }

    /**
     * Liquid Glass Toast landing VFX inspired by sakuga-vfx toast.js:
     * symmetrical trailing speed lines + top crown ticks + side dust puffs + corner glints.
     */
    fun triggerToastLanding(rootCenter: Offset, widthPx: Float) {
        if (reducedMotion) return
        val c = toLocal(rootCenter)
        val halfW = if (widthPx > 0f) min(widthPx * 0.44f, 190f) else 110f

        addParticle(
            TicksParticle(
                x = c.x - halfW * 0.25f,
                y = c.y - 20f,
                count = 3,
                radiusDp = 8f,
                lengthDp = 13f,
                dir = (-PI / 2f).toFloat(),
                fan = 0.50f,
                life = 0.42f
            )
        )
        addParticle(
            KiraParticle(
                x = c.x + halfW * 0.56f,
                y = c.y - 18f,
                radiusDp = 9.5f,
                life = 0.52f
            )
        )
        addParticle(
            KiraParticle(
                x = c.x - halfW * 0.54f,
                y = c.y - 14f,
                radiusDp = 7.5f,
                life = 0.46f
            )
        )
        for (i in 0 until 3) {
            addParticle(
                SpeedLinesParticle(
                    x = c.x + halfW * 0.68f,
                    y = c.y + rndF(-10f, 10f),
                    dir = 0f,
                    lenDp = rndF(28f, 48f),
                    offsetDp = rndF(-12f, 12f),
                    delayFrac = rndF(0f, 0.16f),
                    widthDp = rndF(1.3f, 2.3f),
                    life = 0.34f
                )
            )
            addParticle(
                SpeedLinesParticle(
                    x = c.x - halfW * 0.68f,
                    y = c.y + rndF(-10f, 10f),
                    dir = PI.toFloat(),
                    lenDp = rndF(26f, 44f),
                    offsetDp = rndF(-12f, 12f),
                    delayFrac = rndF(0f, 0.16f),
                    widthDp = rndF(1.2f, 2.2f),
                    life = 0.34f
                )
            )
        }
        spawnPuff(
            x = c.x - halfW * 0.54f,
            y = c.y + 8f,
            n = 3,
            sizeDp = 8.5f,
            vxDp = -24f,
            vyDp = -10f,
            life = 0.58f
        )
        spawnPuff(
            x = c.x + halfW * 0.54f,
            y = c.y + 8f,
            n = 3,
            sizeDp = 8.5f,
            vxDp = 24f,
            vyDp = -10f,
            life = 0.58f
        )
    }

    private fun spawnPuff(
        x: Float,
        y: Float,
        n: Int,
        sizeDp: Float,
        vxDp: Float,
        vyDp: Float,
        life: Float
    ) {
        val lobes = buildList {
            add(PuffLobe(angle = 0f, distDp = 0f, radiusDp = sizeDp, phase = 0f, speed = 1f))
            for (i in 0 until n) {
                add(
                    PuffLobe(
                        angle = (i.toFloat() / n) * TAU + rndF(0f, 0.6f),
                        distDp = rndF(0.25f, 0.85f) * sizeDp,
                        radiusDp = rndF(0.55f, 1f) * sizeDp,
                        phase = rndF(0f, TAU),
                        speed = rndF(0.6f, 1.3f)
                    )
                )
            }
        }
        addParticle(
            PuffParticle(
                x = x,
                y = y,
                vxDp = vxDp,
                vyDp = vyDp,
                sizeDp = sizeDp,
                lobes = lobes,
                drawArcs = sizeDp > 6.5f,
                life = life
            )
        )
    }

    private fun spawnShards(
        x: Float,
        y: Float,
        count: Int,
        speedDp: Float,
        sizeDp: Float,
        life: Float
    ) {
        val basePts = listOf(-1.4f to 0.1f, -0.6f to -0.7f, 1.2f to -0.45f, 1.5f to 0.15f, 0.4f to 0.75f)
        for (i in 0 until count) {
            val angle = (-PI / 2f).toFloat() + rndF(-1.45f, 1.45f)
            addParticle(
                ShardParticle(
                    x = x,
                    y = y,
                    angle = angle,
                    speedDp = speedDp * rndF(0.55f, 1.1f),
                    sizeDp = sizeDp * rndF(0.65f, 1.3f),
                    spin = rndF(-6.5f, 6.5f),
                    gravityDp = 380f,
                    pts = basePts.map { (ux, uy) -> ux * rndF(0.8f, 1.2f) to uy * rndF(0.8f, 1.2f) },
                    life = life * rndF(0.75f, 1.0f)
                )
            )
        }
    }
}

val LocalSakugaVfx = staticCompositionLocalOf<SakugaVfxController?> { null }

@Composable
fun rememberSakugaVfxController(): SakugaVfxController {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
    return remember(reducedMotion) { SakugaVfxController(reducedMotion) }
}

/**
 * Full-screen non-touch-intercepting overlay Canvas drawing active Sakuga VFX particles.
 * Idles at 0% CPU whenever no particles are alive.
 */
@Composable
fun SakugaVfxCanvasOverlay(
    controller: SakugaVfxController,
    modifier: Modifier = Modifier
) {
    val palette = SakugaPalette.fromGlassColors(LocalNovaGlassColors.current)
    val density = LocalDensity.current.density

    LaunchedEffect(controller, controller.hasActiveEffects) {
        if (!controller.hasActiveEffects) return@LaunchedEffect
        var lastNanos = 0L
        while (controller.hasActiveEffects) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) {
                    0.016f
                } else {
                    ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.034f)
                }
                lastNanos = frameNanos
                controller.step(dt)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                controller.overlayOriginInRoot = coords.positionInRoot()
            }
    ) {
        if (controller.hasActiveEffects) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                controller.drawAll(scope = this, density = density, palette = palette)
            }
        }
    }
}
