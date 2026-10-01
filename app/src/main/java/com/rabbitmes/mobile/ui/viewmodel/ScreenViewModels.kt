package com.rabbitmes.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rabbitmes.mobile.core.ActionRunner
import com.rabbitmes.mobile.core.UserMessages
import com.rabbitmes.mobile.core.runCatchingCancellable
import com.rabbitmes.mobile.data.NotificationRepository
import com.rabbitmes.mobile.data.mapper.isOpen
import com.rabbitmes.mobile.data.reference.OperationCatalog
import com.rabbitmes.mobile.data.task.ProductionTaskRepository
import com.rabbitmes.mobile.domain.Employee
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.NotificationUi
import com.rabbitmes.mobile.domain.OperationDefinition
import com.rabbitmes.mobile.domain.ShiftState
import com.rabbitmes.mobile.session.EmployeeSession
import com.rabbitmes.mobile.session.ShiftRepository
import com.rabbitmes.mobile.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import retrofit2.HttpException
import javax.inject.Inject

private fun <T> ViewModel.state(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
    flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

private fun ProductionTaskRepository.visibleTasksFlow(employeeSession: EmployeeSession) =
    combine(tasks, employeeSession.employee) { tasks, employee -> visibleTasks(tasks, employee) }

@HiltViewModel
class ShiftViewModel @Inject constructor(
    employeeSession: EmployeeSession,
    private val shiftRepository: ShiftRepository,
    private val taskRepository: ProductionTaskRepository,
    notificationRepository: NotificationRepository,
    private val messages: UserMessages,
    private val actionRunner: ActionRunner,
) : ViewModel() {
    val employee: StateFlow<Employee> = employeeSession.employee
    val shift: StateFlow<ShiftState> = shiftRepository.shift
    val tasks: StateFlow<List<MobileTask>> = state(taskRepository.visibleTasksFlow(employeeSession), emptyList())
    val nextTask: StateFlow<MobileTask?> = state(
        combine(taskRepository.tasks, employeeSession.employee) { tasks, employee -> taskRepository.nextTask(tasks, employee) },
        null,
    )
    val message: StateFlow<String?> = messages.info
    val unreadNotifications: StateFlow<Int> = state(notificationRepository.notifications.map { list -> list.count { it.isUnread } }, 0)
    val isBusy: StateFlow<Boolean> = actionRunner.busy
    val isTasksLoading: StateFlow<Boolean> = taskRepository.isLoading

    fun startShift() {
        actionRunner.launchBlocking(viewModelScope, "Open shift action failed", "Не удалось открыть смену") {
            runCatchingCancellable { shiftRepository.open() }
                .onSuccess {
                    messages.show("Смена открыта")
                    onShiftOpened()
                }
                .onFailure { error ->
                    // 400: смена уже открыта на сервере. Проверяем по профилю.
                    val alreadyOpen = error is HttpException && error.code() == 400 &&
                        runCatchingCancellable { shiftRepository.refreshFromProfile() }.isSuccess &&
                        shiftRepository.current.isOpen()
                    if (alreadyOpen) {
                        messages.show("Смена уже открыта")
                        onShiftOpened()
                    } else {
                        messages.handle(error, "Не удалось открыть смену", "Open shift failed")
                    }
                }
        }
    }

    fun finishShift(reason: String) {
        actionRunner.launchBlocking(viewModelScope, "Close shift action failed", "Не удалось закрыть смену") {
            runCatchingCancellable { shiftRepository.close() }
                .onSuccess {
                    taskRepository.stopAutoRefresh()
                    messages.show("Смена завершена")
                }
                .onFailure { error ->
                    if (error !is HttpException || error.code() != 400) {
                        messages.handle(error, "Не удалось закрыть смену", "Close shift failed. reason=$reason")
                        return@onFailure
                    }
                    // 400: смена уже закрыта на сервере. Проверяем по профилю.
                    runCatchingCancellable { shiftRepository.refreshFromProfile() }
                        .onSuccess {
                            if (!shiftRepository.current.isOpen()) {
                                taskRepository.clearTasks()
                                taskRepository.stopAutoRefresh()
                                messages.show("Смена уже завершена")
                            } else {
                                messages.handle(error, "Не удалось закрыть смену", "Close shift failed. reason=$reason")
                            }
                        }
                        .onFailure { profileError ->
                            messages.handle(profileError, "Не удалось проверить состояние смены", "Shift state refresh after close failure failed")
                        }
                }
        }
    }

    private suspend fun onShiftOpened() {
        taskRepository.load()
        taskRepository.startAutoRefresh()
    }
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    employeeSession: EmployeeSession,
    shiftRepository: ShiftRepository,
    taskRepository: ProductionTaskRepository,
    messages: UserMessages,
) : ViewModel() {
    val tasks: StateFlow<List<MobileTask>> = state(taskRepository.visibleTasksFlow(employeeSession), emptyList())
    val nextTask: StateFlow<MobileTask?> = state(
        combine(taskRepository.tasks, employeeSession.employee) { tasks, employee -> taskRepository.nextTask(tasks, employee) },
        null,
    )
    val message: StateFlow<String?> = messages.info
    val shiftStarted: StateFlow<Boolean> = state(shiftRepository.shift.map { it.startedAt != null }, false)
}

@HiltViewModel
class SyncViewModel @Inject constructor(
    shiftRepository: ShiftRepository,
    taskRepository: ProductionTaskRepository,
    private val syncManager: SyncManager,
) : ViewModel() {
    val shift: StateFlow<ShiftState> = shiftRepository.shift
    val tasks: StateFlow<List<MobileTask>> = taskRepository.tasks

    fun syncNow() = syncManager.syncNow()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    employeeSession: EmployeeSession,
    taskRepository: ProductionTaskRepository,
    catalog: OperationCatalog,
) : ViewModel() {
    val employee: StateFlow<Employee> = employeeSession.employee
    val tasks: StateFlow<List<MobileTask>> = state(taskRepository.visibleTasksFlow(employeeSession), emptyList())
    val operations: List<OperationDefinition> = catalog.all
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    val notifications: StateFlow<List<NotificationUi>> = notificationRepository.notifications

    fun markAsRead(id: Long) = notificationRepository.markAsRead(id)

    fun markAllAsRead() = notificationRepository.markAllAsRead()
}
