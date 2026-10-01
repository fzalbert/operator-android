package com.rabbitmes.mobile.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import ru.profikrol.operator.data.local.SessionStore
import javax.inject.Inject

/** Поднимает стрим уведомлений после перезагрузки устройства и обновления приложения. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var sessionStore: SessionStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (sessionStore.isLoggedIn) NotificationStreamService.start(context)
    }
}
