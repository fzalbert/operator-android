package com.rabbitmes.mobile.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rabbitmes.mobile.domain.NotificationType
import com.rabbitmes.mobile.domain.NotificationUi
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.profikrol.operator.MainActivity
import ru.profikrol.operator.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        manager.createNotificationChannelsCompat(
            listOf(
                channel(CHANNEL_STREAM, "Связь с сервером", NotificationManagerCompat.IMPORTANCE_MIN),
                channel(CHANNEL_CRITICAL, "Критичные события", NotificationManagerCompat.IMPORTANCE_HIGH),
                channel(CHANNEL_GENERAL, "Уведомления", NotificationManagerCompat.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun streamNotification(): Notification =
        NotificationCompat.Builder(context, CHANNEL_STREAM)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Профикроль на связи")
            .setContentText("Получаем уведомления фермы")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openAppIntent(openNotifications = false))
            .build()

    fun show(item: NotificationUi) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val critical = item.type == NotificationType.CRITICAL
        val notification = NotificationCompat.Builder(context, if (critical) CHANNEL_CRITICAL else CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.title)
            .setContentText(item.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.description))
            .setPriority(if (critical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (critical) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(openNotifications = true))
            .build()
        manager.notify((item.id % Int.MAX_VALUE).toInt(), notification)
    }

    private fun openAppIntent(openNotifications: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN_NOTIFICATIONS, openNotifications)
        return PendingIntent.getActivity(
            context,
            if (openNotifications) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun channel(id: String, name: String, importance: Int) =
        NotificationChannelCompat.Builder(id, importance).setName(name).build()

    companion object {
        const val STREAM_NOTIFICATION_ID = 1001
        const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
        private const val CHANNEL_STREAM = "notification_stream"
        private const val CHANNEL_CRITICAL = "alerts_critical"
        private const val CHANNEL_GENERAL = "alerts"
    }
}
