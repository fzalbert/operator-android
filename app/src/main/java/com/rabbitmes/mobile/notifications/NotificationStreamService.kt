package com.rabbitmes.mobile.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.rabbitmes.mobile.data.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.profikrol.operator.data.local.SessionStore
import javax.inject.Inject

/**
 * Держит процесс живым, пока пользователь залогинен, чтобы gRPC-стрим уведомлений
 * работал при свёрнутом и закрытом приложении, и показывает новые уведомления
 * системными. Сам стрим живёт в [NotificationRepository].
 */
@AndroidEntryPoint
class NotificationStreamService : Service() {
    @Inject lateinit var repository: NotificationRepository
    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var systemNotifications: SystemNotifications

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (sessionStore.currentUser == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this,
            SystemNotifications.STREAM_NOTIFICATION_ID,
            systemNotifications.streamNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            } else {
                0
            },
        )
        if (!started) {
            started = true
            scope.launch {
                repository.incoming.collect(systemNotifications::show)
            }
            scope.launch {
                sessionStore.user.first { it == null }
                Log.d(TAG, "Session closed, stopping notification service")
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "NotificationService"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, NotificationStreamService::class.java))
            }.onFailure { error ->
                // Android 12+ запрещает стартовать foreground service из фона.
                // Сервис поднимется при следующем открытии приложения.
                Log.w(TAG, "Unable to start notification service", error)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotificationStreamService::class.java))
        }
    }
}
