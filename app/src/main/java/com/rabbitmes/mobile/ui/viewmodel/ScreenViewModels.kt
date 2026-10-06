package com.rabbitmes.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rabbitmes.mobile.core.UserMessages
import com.rabbitmes.mobile.data.NotificationRepository
import com.rabbitmes.mobile.data.task.ProductionTaskRepository
import com.rabbitmes.mobile.domain.Employee
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.NotificationUi
import com.rabbitmes.mobile.domain.ShiftState
import com.rabbitmes.mobile.session.EmployeeSession
import com.rabbitmes.mobile.session.ShiftRepository
import com.rabbitmes.mobile.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private fun <T> ViewModel.state(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
    flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    shiftRepository: ShiftRepository,
    taskRepository: ProductionTaskRepository,
    messages: UserMessages,
) : ViewModel() {
    val tasks: StateFlow<List<MobileTask>> = state(taskRepository.tasks, emptyList())
    val nextTask: StateFlow<MobileTask?> = state(
        taskRepository.tasks.map(taskRepository::nextTask),
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
) : ViewModel() {
    val employee: StateFlow<Employee> = employeeSession.employee
    val tasks: StateFlow<List<MobileTask>> = state(taskRepository.tasks, emptyList())
    val operations: List<String> = OperationType.entries.filter { it != OperationType.GENERAL }.map { it.title }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    val notifications: StateFlow<List<NotificationUi>> = notificationRepository.notifications

    fun markAsRead(id: Long) = notificationRepository.markAsRead(id)

    fun markAllAsRead() = notificationRepository.markAllAsRead()
}
