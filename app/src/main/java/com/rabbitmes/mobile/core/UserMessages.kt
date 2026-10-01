package com.rabbitmes.mobile.core

import android.util.Log
import com.rabbitmes.mobile.AppErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сообщения пользователю, общие для всех экранов: строка статуса
 * (показывается на экранах смены и задач) и ошибки для снекбара.
 */
@Singleton
class UserMessages @Inject constructor() {
    private var nextErrorId = 0L
    private val _info = MutableStateFlow<String?>(null)
    val info: StateFlow<String?> = _info.asStateFlow()
    private val _error = MutableStateFlow<AppErrorMessage?>(null)
    val error: StateFlow<AppErrorMessage?> = _error.asStateFlow()

    fun show(message: String?) {
        _info.value = message
    }

    fun showError(message: String) {
        _info.value = message
        _error.value = AppErrorMessage(++nextErrorId, message)
    }

    fun handle(
        error: Throwable,
        fallbackMessage: String,
        logMessage: String,
        showToUser: Boolean = true,
    ) {
        if (error is CancellationException) throw error
        Log.e(TAG, logMessage, error)
        val message = error.toUserMessage(fallbackMessage)
        if (showToUser) showError(message) else _info.value = message
    }

    fun consumeError(id: Long) {
        if (_error.value?.id == id) _error.value = null
    }

    private companion object {
        const val TAG = "RabbitApi"
    }
}
