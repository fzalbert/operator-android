package com.rabbitmes.mobile.core

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Идентификатор устройства для бэка (смены, выполнение целей). */
@Singleton
class DeviceInfo @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty().ifBlank { Build.MODEL }
    }
}
