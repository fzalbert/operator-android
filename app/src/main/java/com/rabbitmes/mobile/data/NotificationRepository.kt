package com.rabbitmes.mobile.data

import android.content.Context
import android.util.Log
import com.rabbitmes.mobile.domain.NotificationType
import com.rabbitmes.mobile.domain.NotificationUi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import ru.profikrol.operator.BuildConfig
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.data.remote.grpc.NotificationServiceGrpcKt
import ru.profikrol.operator.data.remote.grpc.notificationClientMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val sessionStore: SessionStore,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _notifications = MutableStateFlow(readStored())
    val notifications: StateFlow<List<NotificationUi>> = _notifications

    init {
        scope.launch {
            sessionStore.user
                .mapNotNull { user ->
                    val token = user?.token?.takeIf(::looksLikeJwt) ?: return@mapNotNull null
                    user.id to token
                }
                .distinctUntilChanged()
                .collectLatest { (userId, token) ->
                    Log.d(TAG, "Starting notification stream. userId=$userId")
                    connectWithRetry(token)
                }
        }
    }

    fun markAsRead(id: Long) {
        update { list ->
            list.map { if (it.id == id) it.copy(isUnread = false) else it }
        }
    }

    fun markAllAsRead() = update { list -> list.map { it.copy(isUnread = false) } }

    fun clear() = update { emptyList() }

    private suspend fun connectWithRetry(token: String) {
        var retryDelayMs = INITIAL_RETRY_MS
        while (scope.isActive) {
            val channel = newChannel()
            try {
                val stub = NotificationServiceGrpcKt.NotificationServiceCoroutineStub(channel)
                val request = notificationClientMessage { this.token = token }
                stub.notificationStream(request).collect { message ->
                    retryDelayMs = INITIAL_RETRY_MS
                    Log.d(
                        TAG,
                        "Notification received. type=${message.notificationType}, category=${message.notificationCategory}",
                    )
                    val now = System.currentTimeMillis()
                    val item = NotificationUi(
                        id = now,
                        title = titleFor(message.notificationType),
                        description = message.message,
                        time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now)),
                        isUnread = true,
                        type = categoryFor(message.notificationCategory),
                        backendType = message.notificationType,
                    )
                    update { list -> listOf(item) + list }
                }
                Log.w(TAG, "Notification server stream completed without error")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (unauthenticated: StatusException) {
                if (unauthenticated.status.code == Status.Code.UNAUTHENTICATED) {
                    Log.w(TAG, "Notification stream rejected the access token")
                    return
                }
                Log.w(TAG, "Notification stream disconnected; reconnecting after ${retryDelayMs}ms", unauthenticated)
                delay(retryDelayMs)
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
            } catch (error: Throwable) {
                Log.w(TAG, "Notification stream disconnected; reconnecting after ${retryDelayMs}ms", error)
                delay(retryDelayMs)
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
            } finally {
                channel.shutdownNow()
                channel.awaitTermination(1, TimeUnit.SECONDS)
            }
        }
    }

    private fun newChannel(): ManagedChannel {
        val builder = OkHttpChannelBuilder
            .forAddress(BuildConfig.NOTIFICATIONS_GRPC_HOST, BuildConfig.NOTIFICATIONS_GRPC_PORT)
            .idleTimeout(365, TimeUnit.DAYS)
            .maxInboundMessageSize(MAX_INBOUND_MESSAGE_BYTES)

        if (BuildConfig.NOTIFICATIONS_GRPC_TLS) {
            builder.useTransportSecurity()
            if (BuildConfig.DEBUG) {
                // Development endpoint uses a certificate that may not match the IP host.
                val trustManager = DevelopmentTrustManager
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(trustManager), SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory)
                builder.hostnameVerifier(DevelopmentHostnameVerifier)
            }
        } else {
            builder.usePlaintext()
        }
        return builder.build()
    }

    private fun update(transform: (List<NotificationUi>) -> List<NotificationUi>) {
        _notifications.update(transform)
        preferences.edit().putString(KEY_NOTIFICATIONS, encode(_notifications.value)).apply()
    }

    private fun readStored(): List<NotificationUi> = runCatching {
        val array = JSONArray(preferences.getString(KEY_NOTIFICATIONS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    NotificationUi(
                        id = item.getLong("id"),
                        title = item.getString("title"),
                        description = item.getString("description"),
                        time = item.getString("time"),
                        isUnread = item.getBoolean("unread"),
                        type = runCatching { NotificationType.valueOf(item.getString("category")) }
                            .getOrDefault(NotificationType.DEFAULT),
                        backendType = item.optString("backendType").ifBlank { null },
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun encode(items: List<NotificationUi>): String = JSONArray().apply {
        items.take(MAX_STORED).forEach { item ->
            put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("description", item.description)
                put("time", item.time)
                put("unread", item.isUnread)
                put("category", item.type.name)
                put("backendType", item.backendType)
            })
        }
    }.toString()

    private fun categoryFor(value: String): NotificationType = when (value.uppercase()) {
        "CRITICAL" -> NotificationType.CRITICAL
        "WARNING" -> NotificationType.WARNING
        "INFO" -> NotificationType.INFO
        else -> NotificationType.DEFAULT
    }

    private fun titleFor(value: String): String = when (value.uppercase()) {
        "TEMP_EXCEEDED" -> "Температура превышена"
        "TEMP_DROPPED" -> "Температура понижена"
        "AMMONIA_HIGH" -> "Высокий уровень аммиака"
        "FEED_CRITICAL" -> "Критический уровень корма"
        "MORTALITY_CRITICAL" -> "Критическая смертность"
        "WEIGHT_DROP" -> "Снижение веса"
        "TASK_OVERDUE" -> "Задача просрочена"
        "INSEMINATION_DAY_BEFORE" -> "Осеменение завтра"
        "INSEMINATION_DAY" -> "Осеменение сегодня"
        "PALPATION_DAY_BEFORE" -> "Пальпация завтра"
        "PALPATION_DAY" -> "Пальпация сегодня"
        "NEST_INSTALL" -> "Установка гнёзд"
        "WEIGHT_CHECK" -> "Контрольное взвешивание"
        "RELOCATION_WEANING" -> "Переселение и отъём"
        "SLAUGHTER_PREP" -> "Подготовка к забою"
        else -> "Уведомление"
    }

    private fun looksLikeJwt(token: String): Boolean = token.count { it == '.' } == 2

    private companion object {
        const val TAG = "NotificationStream"
        const val PREFERENCES = "operator_notifications"
        const val KEY_NOTIFICATIONS = "items"
        const val MAX_STORED = 200
        const val MAX_INBOUND_MESSAGE_BYTES = 4 * 1024 * 1024
        const val INITIAL_RETRY_MS = 1_000L
        const val MAX_RETRY_MS = 30_000L
    }

    private object DevelopmentTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    private object DevelopmentHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String?, session: javax.net.ssl.SSLSession?): Boolean = true
    }
}
