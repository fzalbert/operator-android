package com.rabbitmes.mobile.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Запуск действий из ViewModel с единой обработкой ошибок.
 * [launchBlocking] показывает на весь экран индикатор и не даёт запустить
 * второе такое действие, пока идёт первое.
 */
@Singleton
class ActionRunner @Inject constructor(
    private val messages: UserMessages,
) {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun launch(
        scope: CoroutineScope,
        logMessage: String,
        fallbackMessage: String = DEFAULT_ERROR,
        block: suspend () -> Unit,
    ): Job = scope.launch {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            messages.handle(error, fallbackMessage, logMessage)
        }
    }

    fun launchBlocking(
        scope: CoroutineScope,
        logMessage: String,
        fallbackMessage: String,
        block: suspend () -> Unit,
    ): Job? {
        if (_busy.value) return null
        _busy.value = true
        return launch(scope, logMessage, fallbackMessage) {
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }

    private companion object {
        const val DEFAULT_ERROR = "Произошла непредвиденная ошибка"
    }
}
