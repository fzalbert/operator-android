package com.rabbitmes.mobile.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rabbitmes.mobile.AppErrorMessage
import com.rabbitmes.mobile.core.ActionRunner
import com.rabbitmes.mobile.core.UserMessages
import com.rabbitmes.mobile.core.runCatchingCancellable
import com.rabbitmes.mobile.data.NotificationRepository
import com.rabbitmes.mobile.data.mapper.isOpen
import com.rabbitmes.mobile.data.task.ProductionTaskRepository
import com.rabbitmes.mobile.session.EmployeeSession
import com.rabbitmes.mobile.session.ShiftRepository
import com.rabbitmes.mobile.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.repository.AuthRepository
import javax.inject.Inject

/** Оболочка приложения: вход и выход, сеть, общие сообщения и индикатор занятости. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val employeeSession: EmployeeSession,
    private val shiftRepository: ShiftRepository,
    private val taskRepository: ProductionTaskRepository,
    private val syncManager: SyncManager,
    private val notificationRepository: NotificationRepository,
    private val messages: UserMessages,
    private val actionRunner: ActionRunner,
) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    val error: StateFlow<AppErrorMessage?> = messages.error
    val busy: StateFlow<Boolean> = actionRunner.busy
    val pendingSyncEvents: StateFlow<Int> = shiftRepository.shift
        .map { it.pendingSyncEvents }
        .stateIn(viewModelScope, SharingStarted.Eagerly, shiftRepository.current.pendingSyncEvents)
    private val _openNotificationsRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openNotificationsRequests: SharedFlow<Unit> = _openNotificationsRequests

    init {
        if (sessionStore.currentUser != null) onLoggedIn()
        viewModelScope.launch {
            // Сессию могут сбросить снаружи, например при неудачном обновлении токена.
            sessionStore.user.collect { user -> if (user == null) _isLoggedIn.value = false }
        }
    }

    fun onLoggedIn() {
        employeeSession.startFromSession()
        taskRepository.reset()
        shiftRepository.reset()
        messages.show(null)
        _isLoggedIn.value = true
        viewModelScope.launch {
            runCatchingCancellable {
                shiftRepository.restoreCached()
                taskRepository.restoreCached()
                syncManager.restorePendingCount()
            }.onFailure { Log.e(TAG, "Offline cache restore failed", it) }
            refreshProfile()
        }
    }

    fun logout() {
        taskRepository.stopAutoRefresh()
        notificationRepository.clear()
        _isLoggedIn.value = false
        actionRunner.launch(viewModelScope, "Logout failed", "Не удалось выйти из профиля") {
            authRepository.logout()
        }
    }

    fun setOnline(isOnline: Boolean) = syncManager.setOnline(isOnline)

    fun onAppStarted() {
        if (_isLoggedIn.value) taskRepository.startAutoRefresh()
    }

    fun onAppStopped() = taskRepository.stopAutoRefresh()

    fun onDestinationChanged() = messages.show(null)

    fun consumeError(id: Long) = messages.consumeError(id)

    fun requestOpenNotifications() {
        if (_isLoggedIn.value) _openNotificationsRequests.tryEmit(Unit)
    }

    private suspend fun refreshProfile() {
        runCatchingCancellable { shiftRepository.refreshFromProfile() }
            .onSuccess {
                if (shiftRepository.current.isOpen()) {
                    runCatchingCancellable { taskRepository.load(showLoading = false) }
                        .onFailure { Log.e(TAG, "Tasks load after login failed", it) }
                    taskRepository.startAutoRefresh()
                } else {
                    taskRepository.clearTasks()
                    taskRepository.stopAutoRefresh()
                }
            }
            .onFailure { error -> messages.handle(error, "Не удалось обновить профиль", "Profile refresh failed") }
    }

    private companion object {
        const val TAG = "AppViewModel"
    }
}
