package com.rabbitmes.mobile.shift.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rabbitmes.mobile.core.ActionRunner
import com.rabbitmes.mobile.core.UserMessages
import com.rabbitmes.mobile.core.problemCode
import com.rabbitmes.mobile.core.runCatchingCancellable
import com.rabbitmes.mobile.data.NotificationRepository
import com.rabbitmes.mobile.data.mapper.isOpen
import com.rabbitmes.mobile.data.task.ProductionTaskRepository
import com.rabbitmes.mobile.session.EmployeeSession
import com.rabbitmes.mobile.session.ShiftRepository
import com.rabbitmes.mobile.shift.data.WorkplaceRepository
import com.rabbitmes.mobile.shift.model.AssignedTask
import com.rabbitmes.mobile.shift.model.Hangar
import com.rabbitmes.mobile.shift.model.Workshop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

/** Состояние экрана «Смена». */
sealed interface ShiftUiState {
    /** Смена не начата. */
    data class Idle(val userName: String) : ShiftUiState

    /** Выбор ангара перед началом смены. */
    data class PickHangar(
        val workshops: List<Workshop>,
        val selected: Hangar?,
    ) : ShiftUiState {
        val selectedWorkshop: Workshop?
            get() = selected?.let { hangar -> workshops.firstOrNull { hangar in it.hangars } }
    }

    /** Смена идёт. [task] == null — ждём назначения задачи. */
    data class Active(
        val userName: String,
        val startedAtMillis: Long,
        val workshop: Workshop,
        val hangar: Hangar,
        val task: AssignedTask?,
    ) : ShiftUiState
}

/** Шапка экрана: роль, непрочитанные уведомления и неотправленные изменения. */
data class ShiftHeaderState(
    val roleTitle: String,
    val unreadNotifications: Int,
    val pendingSyncEvents: Int,
    val isOnline: Boolean = true,
)

/**
 * Смена через ProductionProgram: цеха из профиля, открытие смены в выбранном
 * ангаре и её закрытие, задача — первая в очереди задач смены.
 */
@HiltViewModel
class ShiftHomeViewModel @Inject constructor(
    private val workplaceRepository: WorkplaceRepository,
    private val shiftRepository: ShiftRepository,
    private val taskRepository: ProductionTaskRepository,
    private val messages: UserMessages,
    private val actionRunner: ActionRunner,
    employeeSession: EmployeeSession,
    notificationRepository: NotificationRepository,
) : ViewModel() {
    /** Выбор ангара открыт; null — закрыт. */
    private data class Picker(val selected: Hangar?)

    private val picker = MutableStateFlow<Picker?>(null)

    private val nextTask = taskRepository.tasks.map(taskRepository::nextTask)

    val state: StateFlow<ShiftUiState> = combine(
        workplaceRepository.workshops,
        shiftRepository.shift,
        picker,
        nextTask,
        employeeSession.employee,
    ) { workshops, shift, picker, task, employee ->
        val userName = employee.fullName.substringBefore(' ')
        when {
            picker != null -> ShiftUiState.PickHangar(workshops, picker.selected)
            shift.isOpen() -> {
                val hangarId = shift.hangarId?.toString()
                val workshop = workshops.firstOrNull { ws -> ws.hangars.any { it.id == hangarId } }
                val hangar = workshop?.hangars?.firstOrNull { it.id == hangarId }
                    ?: Hangar(id = hangarId.orEmpty(), name = hangarId.orEmpty())
                ShiftUiState.Active(
                    userName = userName,
                    startedAtMillis = shift.startedAt.toEpochMillis(),
                    workshop = workshop ?: Workshop(id = "", name = "", hangars = listOf(hangar)),
                    hangar = hangar,
                    task = task?.let {
                        AssignedTask(id = it.id, title = it.title, hangarName = hangar.name, overdueDate = it.overdueDate)
                    },
                )
            }
            else -> ShiftUiState.Idle(userName)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShiftUiState.Idle(employeeSession.current.fullName.substringBefore(' ')))

    val header: StateFlow<ShiftHeaderState> = combine(
        employeeSession.employee,
        notificationRepository.notifications,
        shiftRepository.shift,
    ) { employee, notifications, shift ->
        ShiftHeaderState(
            roleTitle = employee.role.title,
            unreadNotifications = notifications.count { it.isUnread },
            pendingSyncEvents = shift.pendingSyncEvents,
            isOnline = shift.isOnline,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShiftHeaderState(employeeSession.current.role.title, 0, 0))

    val isBusy: StateFlow<Boolean> = actionRunner.busy

    fun startShift() {
        picker.value = Picker(selected = null)
        if (shiftRepository.manufactures.value.isEmpty()) {
            actionRunner.launch(viewModelScope, "Workshops refresh failed", "Не удалось загрузить цеха") {
                workplaceRepository.refresh()
            }
        }
    }

    fun selectHangar(hangar: Hangar) {
        picker.value = Picker(selected = hangar)
    }

    fun cancelPick() {
        picker.value = null
    }

    /** Открывает смену в выбранном ангаре. */
    fun confirmHangar() {
        val hangarId = picker.value?.selected?.id?.toLongOrNull() ?: return
        actionRunner.launchBlocking(viewModelScope, "Start shift failed", "Не удалось начать смену") {
            runCatchingCancellable { shiftRepository.open(hangarId) }.getOrElse { error ->
                val conflictCode = (error as? HttpException)?.takeIf { it.code() == 409 }?.problemCode()
                when (conflictCode) {
                    // В ангаре работает другой сотрудник: выбор остаётся открытым, можно взять другой ангар.
                    HANGAR_OCCUPIED -> {
                        messages.showError("Ангар занят: в нём открыта смена другого сотрудника")
                        return@launchBlocking
                    }
                    // Смена уже открыта на сервере (например, с другого устройства).
                    SHIFT_ALREADY_OPEN -> {
                        val alreadyOpen = runCatchingCancellable { shiftRepository.refreshFromProfile() }.isSuccess &&
                            shiftRepository.current.isOpen()
                        if (!alreadyOpen) throw error
                    }
                    else -> throw error
                }
            }
            picker.value = null
            messages.show(null)
            viewModelScope.launch { runCatchingCancellable { taskRepository.load() } }
            taskRepository.startAutoRefresh()
        }
    }

    /**
     * Закрывает смену. Закрытие идёт на сервер напрямую, мимо офлайн-очереди, поэтому
     * сначала должны уйти накопленные изменения задач. Задачу в работе после закрытия
     * продолжит следующий оператор ангара.
     */
    fun closeShift() {
        if (shiftRepository.current.pendingSyncEvents > 0) {
            messages.showError("Дождитесь отправки изменений, потом закройте смену")
            return
        }
        actionRunner.launchBlocking(viewModelScope, "Close shift failed", "Не удалось закрыть смену") {
            shiftRepository.close()
            taskRepository.stopAutoRefresh()
            taskRepository.clearTasks()
            messages.show("Смена закрыта")
        }
    }

    private companion object {
        /** Коды 409 от `shifts/start`. */
        const val SHIFT_ALREADY_OPEN = "shift_already_open"
        const val HANGAR_OCCUPIED = "hangar_occupied"
    }
}

/** Время начала смены с сервера (ISO, UTC) в миллисекундах; если не разобрать — сейчас. */
private fun String?.toEpochMillis(): Long {
    val value = this ?: return System.currentTimeMillis()
    return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .recoverCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())
}
