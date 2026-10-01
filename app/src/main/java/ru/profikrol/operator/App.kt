package ru.profikrol.operator

import android.app.Application
import com.rabbitmes.mobile.notifications.NotificationStreamService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.profikrol.operator.data.local.SessionStore
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var sessionStore: SessionStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        // Стрим уведомлений работает в foreground service, пока пользователь залогинен.
        appScope.launch {
            sessionStore.user
                .map { it != null }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    if (loggedIn) {
                        NotificationStreamService.start(this@App)
                    } else {
                        NotificationStreamService.stop(this@App)
                    }
                }
        }
    }
}
