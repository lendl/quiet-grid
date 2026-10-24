package com.quietgrid.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.R
import com.quietgrid.app.toIntent
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId

private const val CHANNEL_ID = "daily_reminder"
private const val NOTIFICATION_ID = 1001
private const val HALLOWEEN_ACCENT = 0xFFEE9A4D.toInt()

object DailyReminderNotifier {
    fun hasPermission(context: Context): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun post(context: Context, games: List<GameId>, halloween: Boolean = false) {
        if (!hasPermission(context)) return
        ensureChannel(context)
        val titles = games.joinToString(", ") { context.getString(GameCatalog.get(it).titleRes) }
        val intent = LaunchRequest.DailyTab.toIntent(context)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        if (halloween) {
            builder
                .setSmallIcon(R.drawable.ic_notification_pumpkin)
                .setColor(HALLOWEEN_ACCENT)
                .setContentTitle(context.getString(R.string.daily_reminder_title_halloween))
                .setContentText(context.getString(R.string.daily_reminder_text_halloween, titles))
            ContextCompat.getDrawable(context, R.drawable.ic_halloween_jack_o_lantern)?.let { builder.setLargeIcon(it.toBitmap()) }
        } else {
            builder
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(context.getString(R.string.daily_reminder_title))
                .setContentText(context.getString(R.string.daily_reminder_text, titles))
        }
        val notification = builder.build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.daily_reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = context.getString(R.string.daily_reminder_channel_description)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
