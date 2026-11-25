package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

enum class NovaNotificationSound(
    val id: String,
    val label: String,
    val channelId: String,
    val channelName: String,
    @RawRes val rawResId: Int?
) {
    GLASS_CHIME(
        id = "GLASS_CHIME",
        label = "Glass Chime",
        channelId = "nova_site_glass_chime_v1",
        channelName = "Site Notifications · Glass Chime",
        rawResId = R.raw.nova_glass_chime
    ),
    CRYSTAL_PING(
        id = "CRYSTAL_PING",
        label = "Crystal Ping",
        channelId = "nova_site_crystal_ping_v1",
        channelName = "Site Notifications · Crystal Ping",
        rawResId = R.raw.nova_crystal_ping
    ),
    AURORA_PULSE(
        id = "AURORA_PULSE",
        label = "Aurora Pulse",
        channelId = "nova_site_aurora_pulse_v1",
        channelName = "Site Notifications · Aurora Pulse",
        rawResId = R.raw.nova_aurora_pulse
    ),
    SYSTEM_DEFAULT(
        id = "SYSTEM_DEFAULT",
        label = "System Default",
        channelId = "nova_site_system_default_v1",
        channelName = "Site Notifications · Default",
        rawResId = null
    );

    companion object {
        fun fromId(id: String?): NovaNotificationSound {
            return entries.firstOrNull { it.id == id } ?: GLASS_CHIME
        }
    }
}

object NovaNotificationHelper {

    private const val DOWNLOAD_CHANNEL_ID = "nova_downloads_chime_v1"
    private var activePreviewPlayer: MediaPlayer? = null

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun previewSound(context: Context, sound: NovaNotificationSound) {
        runCatching {
            activePreviewPlayer?.release()
            activePreviewPlayer = null

            val resId = sound.rawResId
            if (resId != null) {
                val player = MediaPlayer.create(context.applicationContext, resId) ?: return
                activePreviewPlayer = player
                player.setOnCompletionListener { mp ->
                    mp.release()
                    if (activePreviewPlayer === mp) activePreviewPlayer = null
                }
                player.setOnErrorListener { mp, _, _ ->
                    mp.release()
                    if (activePreviewPlayer === mp) activePreviewPlayer = null
                    true
                }
                player.start()
            } else {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
                RingtoneManager.getRingtone(context.applicationContext, uri)?.play()
            }
        }
    }

    fun showSiteNotification(
        context: Context,
        host: String,
        title: String,
        body: String,
        sound: NovaNotificationSound,
        targetUrl: String
    ) {
        if (!hasNotificationPermission(context)) return
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val soundUri = resolveSoundUri(context, sound.rawResId)
            ensureChannel(
                nm = nm,
                channelId = sound.channelId,
                channelName = sound.channelName,
                description = "Notifications from allowed websites using ${sound.label} sound",
                soundUri = soundUri,
                importance = NotificationManager.IMPORTANCE_HIGH
            )

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(targetUrl)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                host.hashCode(),
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, sound.channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title.ifBlank { host })
                .setContentText(body.ifBlank { "New update from $host" })
                .setSubText(host)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && soundUri != null) {
                builder.setSound(soundUri)
            }

            val notifId = (host + ":" + System.currentTimeMillis()).hashCode()
            nm.notify(notifId, builder.build())
        }
    }

    fun showDownloadNotification(context: Context, fileName: String) {
        if (!hasNotificationPermission(context)) return
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val soundUri = resolveSoundUri(context, R.raw.nova_download_chime)
            ensureChannel(
                nm = nm,
                channelId = DOWNLOAD_CHANNEL_ID,
                channelName = "Nova Downloads",
                description = "Alerts when files finish downloading",
                soundUri = soundUri,
                importance = NotificationManager.IMPORTANCE_DEFAULT
            )

            val launchIntent = PendingIntent.getActivity(
                context,
                fileName.hashCode(),
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Download complete")
                .setContentText(fileName)
                .setAutoCancel(true)
                .setContentIntent(launchIntent)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && soundUri != null) {
                builder.setSound(soundUri)
            }

            nm.notify(fileName.hashCode(), builder.build())
        }
    }

    private fun resolveSoundUri(context: Context, @RawRes rawResId: Int?): Uri? {
        return if (rawResId != null) {
            Uri.parse("android.resource://${context.packageName}/$rawResId")
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    private fun ensureChannel(
        nm: NotificationManager,
        channelId: String,
        channelName: String,
        description: String,
        soundUri: Uri?,
        importance: Int
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (nm.getNotificationChannel(channelId) != null) return
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            this.description = description
            if (soundUri != null) {
                setSound(soundUri, audioAttrs)
            }
        }
        nm.createNotificationChannel(channel)
    }
}
