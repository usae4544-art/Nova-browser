package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import kotlin.math.abs

object WebAppInstaller {

    const val EXTRA_STANDALONE_WEB_APP = "extra_standalone_web_app"

    fun isPinShortcutSupported(context: Context): Boolean {
        return runCatching { ShortcutManagerCompat.isRequestPinShortcutSupported(context) }
            .getOrDefault(false)
    }

    /**
     * Installs any website to the Android Home Screen like Chrome's "Install App / Add to Home screen",
     * using Android's official ShortcutManagerCompat API (zero extra permissions required).
     */
    fun installWebsiteToHomeScreen(
        context: Context,
        title: String,
        url: String
    ): Result<Boolean> {
        return runCatching {
            val cleanUrl = url.trim()
            require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                "Open a valid website before installing."
            }
            val cleanTitle = title.trim()
                .ifBlank { UrlResolver.displayHostOrTitle(cleanUrl, "Web App") }
                .take(28)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(cleanUrl)
                putExtra(EXTRA_STANDALONE_WEB_APP, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val iconBitmap = createLiquidGlassSiteIcon(cleanTitle, cleanUrl)
            val shortcutId = "nova_webapp_${abs(cleanUrl.hashCode())}"

            val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(cleanTitle)
                .setLongLabel(cleanTitle)
                .setIcon(IconCompat.createWithBitmap(iconBitmap))
                .setIntent(launchIntent)
                .build()

            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return@runCatching false
            }

            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }
    }

    fun createLiquidGlassSiteIcon(title: String, url: String): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val palettes = listOf(
            intArrayOf(Color.parseColor("#0EA5E9"), Color.parseColor("#6366F1")),
            intArrayOf(Color.parseColor("#8B5CF6"), Color.parseColor("#EC4899")),
            intArrayOf(Color.parseColor("#06B6D4"), Color.parseColor("#3B82F6")),
            intArrayOf(Color.parseColor("#10B981"), Color.parseColor("#0284C7")),
            intArrayOf(Color.parseColor("#6366F1"), Color.parseColor("#A855F7"))
        )
        val palette = palettes[abs(url.hashCode()) % palettes.size]

        val rect = RectF(8f, 8f, size - 8f, size - 8f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                palette[0], palette[1], Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, 44f, 44f, bgPaint)

        // Subtle Liquid Glass top specular highlight
        val sheenRect = RectF(12f, 12f, size - 12f, size * 0.48f)
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 12f, 0f, size * 0.48f,
                Color.argb(95, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(sheenRect, 40f, 40f, sheenPaint)

        // Crisp rim border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(140, 255, 255, 255)
        }
        canvas.drawRoundRect(rect, 44f, 44f, borderPaint)

        // Site Initial glyph
        val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "N"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 86f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initial, size / 2f, textY, textPaint)

        return bitmap
    }
}
