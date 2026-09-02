package com.rabbitmes.mobile

import android.util.Log
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.domain.repository.AuthRepository
import ru.profikrol.operator.data.remote.profile.ProfileApi
import ru.profikrol.operator.data.remote.profile.ShiftDto
import ru.profikrol.operator.data.remote.rabbit.RabbitApi
import ru.profikrol.operator.data.remote.rabbit.RabbitDto
import ru.profikrol.operator.data.remote.cell.CellApi
import ru.profikrol.operator.data.remote.cell.CellDto
import ru.profikrol.operator.data.remote.worktask.WorkTaskApi
import ru.profikrol.operator.data.remote.worktask.WorkTaskDto
import ru.profikrol.operator.data.remote.worktask.CompleteWorkSubtaskRequest
import ru.profikrol.operator.data.remote.worktask.CompleteWorkTaskRequest
import ru.profikrol.operator.data.remote.production.CompleteTargetRequest
import ru.profikrol.operator.data.remote.production.ProductionTaskApi
import ru.profikrol.operator.data.remote.production.ProductionTaskDetailsDto
import javax.inject.Inject
import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.data.NotificationRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.operations.PROBLEM_COMMENT_KEY
import com.rabbitmes.mobile.ui.operations.PROBLEM_REASON_KEY
import java.io.IOException
import retrofit2.HttpException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val API_LOG_TAG = "RabbitApi"
private const val TASKS_REFRESH_INTERVAL_MS = 30_000L
private const val RABBITS_PAGE_SIZE = 100
private const val CELLS_PAGE_SIZE = 100
private const val USE_GENERAL_TEMPLATE_FOR_ALL_OPERATIONS = false
private const val START_TASK_STATUS_POLL_ATTEMPTS = 12
private const val START_TASK_STATUS_POLL_DELAY_MS = 500L

data class AppErrorMessage(
    val id: Long,
    val message: String,
)

sealed class AppScreen {
    data object Login : AppScreen()
    data object Shift : AppScreen()
    data object Tasks : AppScreen()
    data object Map : AppScreen()
    data object Sync : AppScreen()
    data object Profile : AppScreen()
    data object Notifications : AppScreen()
    data object AcceptanceQueue : AppScreen()
    data class TaskExecution(val taskId: String) : AppScreen()
    data class RfidScan(
        val taskId: String,
        val values: kotlin.collections.Map<String, String> = emptyMap(),
    ) : AppScreen()
    data class Acceptance(val taskId: String) : AppScreen()
    data class AnimalHistory(val rabbitId: String) : AppScreen()
    data class RabbitProfile(val rfidCode: String, val taskId: String) : AppScreen()
}

private fun ShiftDto?.toShiftState(employeeId: String, previous: ShiftState): ShiftState =
    if (this == null) {
        ShiftState(
            employeeId = employeeId,
            isOnline = previous.isOnline,
            pendingSyncEvents = previous.pendingSyncEvents,
        )
    } else {
        ShiftState(
            employeeId = employeeId,
            startedAt = openedAt,
            finishedAt = closedAt,
            isOnline = previous.isOnline,
            pendingSyncEvents = previous.pendingSyncEvents,
        )
    }

private fun WorkTaskDto.toMobileTask(
    employeeId: String,
    rabbits: List<RabbitDto> = emptyList(),
    cells: List<CellDto> = emptyList(),
): MobileTask {
    val operationType = resolveOperationType()
    val isGeneral = operationType == OperationType.CUSTOM_TASK
    val targetType = MockRepository.operation(operationType).targetType
    val checklist = if (isGeneral) {
        emptyList()
    } else if (operationType == OperationType.INSEMINATION) {
        rabbits.toRabbitChecklist(taskId = id)
    } else if (operationType == OperationType.NEST_SELECTION) {
        cells.take(1).toCageChecklist(
            taskId = id,
            serverSubtaskId = subtasks.firstOrNull()?.id,
        )
    } else if (subtasks.isNotEmpty()) {
        subtasks.map { subtask ->
            ChecklistItem(
                id = subtask.id.toString(),
                label = subtask.name.ifBlank { "Подзадача ${subtask.id}" },
                targetType = targetType,
                targetId = subtask.id.toString(),
                serverType = subtask.type,
                status = subtask.status.toChecklistStatus(),
                result = ExecutionResult(
                    completedAt = subtask.completedAt,
                    problemReason = subtask.report?.abortReason ?: subtask.skipReason,
                ),
            )
        }
    } else if (targetType == TargetType.RABBIT) {
        rabbits.toRabbitChecklist(taskId = id)
    } else {
        emptyList()
    }
    val taskStatus = status.toTaskStatus()
    return MobileTask(
        id = id.toString(),
        title = name.ifBlank { operationName.orEmpty().ifBlank { programName.orEmpty().ifBlank { "Задача $id" } } },
        operationType = operationType,
        workshopId = manufactureId?.toString().orEmpty(),
        hangarId = manufactureId?.toString().orEmpty(),
        assignedEmployeeId = employeeId,
        dueDate = scheduledDate,
        plannedStart = startedAt.toDisplayTime(),
        plannedDurationMinutes = durationMinutes ?: 0,
        priority = Priority.NORMAL,
        status = taskStatus,
        checklist = checklist,
        requiresAcceptance = requiresAcceptance,
        acceptanceStatus = when {
            !requiresAcceptance -> AcceptanceStatus.NOT_REQUIRED
            status.normalizedStatus() == "AWAITING_ACCEPTANCE" -> AcceptanceStatus.WAITING
            completedAt != null -> AcceptanceStatus.WAITING
            else -> AcceptanceStatus.NOT_REQUIRED
        },
        result = ExecutionResult(
            completedAt = completedAt,
        ),
        description = description.orEmpty().ifBlank {
            subtasks.map { it.description.orEmpty().trim() }
                .filter(String::isNotBlank)
                .distinct()
                .joinToString("\n")
        },
        operationTypeTitle = operationName.orEmpty().ifBlank { operationType.title },
        isGeneral = isGeneral,
        pendingGeneralSubtaskIds = if (isGeneral) {
            subtasks.filterNot { it.status.normalizedStatus() in COMPLETED_SUBTASK_STATUSES }
                .map { it.id }
        } else {
            emptyList()
        },
        workReportId = report?.id,
    )
}

private val COMPLETED_SUBTASK_STATUSES = setOf("COMPLETED", "DONE", "FINISHED", "SKIPPED")

private fun WorkTaskDto.resolveOperationType(): OperationType {
    val candidates = listOf(operationId, operationName, name, operationCategory)
        .map { it.orEmpty().trim() }
        .filter(String::isNotBlank)
    val candidateKeys = candidates.map { it.operationLookupKey() }
    val resolved = OPERATION_ALIASES[operationId.orEmpty().operationLookupKey()]
        ?: candidateKeys.firstNotNullOfOrNull(OPERATION_ALIASES::get)
        ?: OperationType.entries.firstOrNull { type ->
            candidates.any { candidate ->
                candidate.equals(type.name, ignoreCase = true) ||
                    candidate.equals(type.title, ignoreCase = true) ||
                    candidate.operationLookupKey() == type.title.operationLookupKey()
            }
        }
    return resolved ?: OperationType.CUSTOM_TASK
}

private val GENERAL_FORM_OPERATION_TYPES = setOf(
    OperationType.INSEMINATION,
    OperationType.PALPATION,
    OperationType.ANIMAL_SETTLEMENT,
    OperationType.NEST_PREPARATION,
    OperationType.OKROL,
    OperationType.NEST_SELECTION,
    OperationType.NEST_CONTROL,
    OperationType.WEIGHING,
    OperationType.ANIMAL_DEPARTURE,
    OperationType.WEANING,
    OperationType.SLAUGHTER_SHIPMENT,
    OperationType.CLEANING,
    OperationType.FEMALE_DELIVERY,
    OperationType.DEWORMING_DOSATRON,
    OperationType.MORTALITY_ROUND,
    OperationType.FIRST_WEIGHING,
    OperationType.LIGHT_STIMULATION,
    OperationType.LIGHTING_CHECK,
    OperationType.FEED_CHECK,
    OperationType.MANUAL_FEEDING,
)

private val OPERATION_ALIASES = mapOf(
    "animal placement" to OperationType.ANIMAL_SETTLEMENT,
    "animal settlement" to OperationType.ANIMAL_SETTLEMENT,
    "animal transfer" to OperationType.ANIMAL_TRANSFER,
    "animal relocation" to OperationType.ANIMAL_TRANSFER,
    "перевод животных" to OperationType.ANIMAL_TRANSFER,
    "переселение" to OperationType.ANIMAL_TRANSFER,
    "переселение животных" to OperationType.ANIMAL_TRANSFER,
    "kindling" to OperationType.OKROL,
    "nest equalization" to OperationType.NEST_SELECTION,
    "female arrival" to OperationType.FEMALE_DELIVERY,
    "aisle cleaning" to OperationType.DAILY_CLEANING,
    "slaughter shipping" to OperationType.SLAUGHTER_SHIPMENT,
    "light biostimulation" to OperationType.LIGHT_STIMULATION,
    "deworming dosatron" to OperationType.DEWORMING_DOSATRON,
    "mortality round" to OperationType.MORTALITY_ROUND,
    "mortality journal" to OperationType.MORTALITY_JOURNAL,
    "manual feeding" to OperationType.MANUAL_FEEDING,
    "nest control" to OperationType.NEST_CONTROL,
    "nest preparation" to OperationType.NEST_PREPARATION,
    "kindling preparation" to OperationType.OKROL_PREPARATION,
    "hangar acceptance" to OperationType.HANGAR_ACCEPTANCE,
    "water check" to OperationType.WATER_CHECK,
    "feed check" to OperationType.FEED_CHECK,
    "final round" to OperationType.FINAL_ROUND,
    "second round" to OperationType.SECOND_ROUND,
    "females delivery" to OperationType.FEMALE_DELIVERY,
    "culling" to OperationType.ANIMAL_DEPARTURE,
    "light check" to OperationType.LIGHTING_CHECK,
    "управление световым днем" to OperationType.LIGHT_STIMULATION,
    "управление световым днем в определенный ангар" to OperationType.LIGHT_STIMULATION,
    "управление светодвым днем" to OperationType.LIGHT_STIMULATION,
    "управление подачей кормов" to OperationType.MANUAL_FEEDING,
    "управление подачей кормов в определенный ангар" to OperationType.MANUAL_FEEDING,
    "подача кормов" to OperationType.MANUAL_FEEDING,
    "дегельминтизация" to OperationType.DEWORMING_DOSATRON,
    "first weighing" to OperationType.FIRST_WEIGHING,
    "first weigh" to OperationType.FIRST_WEIGHING,
    "первое взвешивание" to OperationType.FIRST_WEIGHING,
)

private fun String.operationLookupKey(): String = trim()
    .lowercase()
    .replace('ё', 'е')
    .replace(Regex("[^a-zа-я0-9]+"), " ")
    .trim()

private fun ProductionTaskDetailsDto.toMobileTask(employeeId: String): MobileTask {
    val operationKey = task.operationCode.orEmpty().operationLookupKey()
    val operationType = OPERATION_ALIASES[operationKey]
        ?: OperationType.entries.firstOrNull { it.name.operationLookupKey() == operationKey }
        ?: OperationType.CUSTOM_TASK
    return MobileTask(
        id = task.id,
        title = task.title.orEmpty().ifBlank { operationType.title },
        operationType = operationType,
        workshopId = task.workshopId.toString(),
        hangarId = task.hangarId?.toString().orEmpty(),
        assignedEmployeeId = task.assignedEmployeeId ?: employeeId,
        dueDate = task.scheduledDate,
        plannedStart = "—",
        plannedDurationMinutes = task.durationMinutes ?: 0,
        priority = Priority.NORMAL,
        status = task.executionStatus.orEmpty().toTaskStatus(),
        checklist = targets.sortedBy { it.sortOrder }.map { target ->
            ChecklistItem(
                id = target.id,
                label = target.displayCode.orEmpty().ifBlank {
                    target.cageId?.let { "Клетка $it" } ?: "Позиция ${target.id.take(8)}"
                },
                targetType = when (target.targetType?.lowercase()) {
                    "cage" -> TargetType.CAGE
                    "hangar" -> TargetType.HANGAR
                    else -> TargetType.RABBIT
                },
                targetId = target.targetId ?: target.cageId?.toString() ?: target.id,
                serverType = "production-target",
                status = target.status.orEmpty().toChecklistStatus(),
                result = ExecutionResult(scannedRfid = target.scanIdentifier, completedAt = target.completedAt),
            )
        },
        requiresAcceptance = task.requiresAcceptance,
        description = task.description.orEmpty(),
        operationTypeTitle = task.title.orEmpty().ifBlank { operationType.title },
        isGeneral = false,
    )
}

private fun List<RabbitDto>.toRabbitChecklist(taskId: Long): List<ChecklistItem> =
    asSequence()
        .mapNotNull { rabbit ->
            val rfid = rabbit.rfid?.trim().orEmpty()
            if (rfid.isBlank()) return@mapNotNull null
            ChecklistItem(
                id = "task-$taskId-rabbit-${rabbit.id ?: rfid}",
                label = buildString {
                    append("RFID: ")
                    append(rfid)
                    if (rabbit.age > 0) append(" · Возраст: ${rabbit.age}")
                },
                targetType = TargetType.RABBIT,
                targetId = rfid,
            )
        }
        .distinctBy { it.targetId.lowercase() }
        .toList()

private fun List<CellDto>.toCageChecklist(
    taskId: Long,
    serverSubtaskId: Long?,
): List<ChecklistItem> =
    map { cell ->
        ChecklistItem(
            id = serverSubtaskId?.toString() ?: "task-$taskId-cell-${cell.id}",
            label = cell.displayName,
            targetType = TargetType.CAGE,
            targetId = cell.displayName,
        )
    }

private fun String.toTaskStatus(): TaskStatus = when (normalizedStatus()) {
    "NEW", "CREATED", "PLANNED", "PENDING" -> TaskStatus.NEW
    "IN_PROGRESS", "STARTED", "OPEN", "OPENED" -> TaskStatus.IN_PROGRESS
    "BLOCKED", "PROBLEM", "FAILED", "ABORTED" -> TaskStatus.BLOCKED
    "DONE", "COMPLETED", "FINISHED", "AWAITING_ACCEPTANCE" -> TaskStatus.DONE
    "SENT", "ACCEPTED", "APPROVED" -> TaskStatus.SENT
    "SKIPPED", "CANCELLED", "CANCELED" -> TaskStatus.SKIPPED
    else -> TaskStatus.NEW
}

private fun String.toChecklistStatus(): ChecklistStatus = when (normalizedStatus()) {
    "DONE", "COMPLETED", "FINISHED", "ACCEPTED", "APPROVED" -> ChecklistStatus.DONE
    "PROBLEM", "FAILED", "BLOCKED", "ABORTED", "REJECTED" -> ChecklistStatus.PROBLEM
    "SKIPPED", "CANCELLED", "CANCELED" -> ChecklistStatus.SKIPPED
    else -> ChecklistStatus.PENDING
}

private fun String.normalizedStatus(): String = trim()
    .uppercase()
    .replace('-', '_')
    .replace(' ', '_')

private fun String?.toDisplayTime(): String = this
    ?.substringAfter('T', "")
    ?.take(5)
    ?.takeIf(String::isNotBlank)
    ?: "—"

private fun Throwable.toUserMessage(fallback: String): String = when (this) {
    is HttpException -> "$fallback: ошибка сервера ${code()}"
    is IOException -> "$fallback: нет соединения с сервером"
    else -> fallback
}

@HiltViewModel
class MobileMesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val profileApi: ProfileApi,
    private val workTaskApi: WorkTaskApi,
    private val productionTaskApi: ProductionTaskApi,
    private val rabbitApi: RabbitApi,
    private val cellApi: CellApi,
) : ViewModel() {
    private var nextErrorId = 0L
    private val globalErrorHandler = CoroutineExceptionHandler { _, error ->
        handleError(error, "Произошла непредвиденная ошибка", "Unhandled coroutine error")
    }

    var screen: AppScreen by mutableStateOf(AppScreen.Login)
        private set
    var currentEmployee: Employee by mutableStateOf(MockRepository.employees.first())
        private set
    var shift: ShiftState by mutableStateOf(ShiftState(currentEmployee.id))
        private set
    var tasks: List<MobileTask> by mutableStateOf(emptyList())
        private set
    var isShiftActionInProgress: Boolean by mutableStateOf(false)
        private set
    var isServerActionInProgress: Boolean by mutableStateOf(false)
        private set
    var isTasksLoading: Boolean by mutableStateOf(false)
        private set
    private var isTasksRequestInProgress = false
    private var isTasksReloadRequested = false
    private var hasLoadedRemoteTasks = false
    private var tasksAutoRefreshJob: Job? = null
    var remarks: List<AcceptanceRemark> by mutableStateOf(emptyList())
        private set

    var lastScannedRfid: String? by mutableStateOf(null)
    private val scannedRfidByTaskId = mutableStateMapOf<String, String>()
    var lastMessage: String? by mutableStateOf(null)
        private set
    var appError: AppErrorMessage? by mutableStateOf(null)
        private set
    val notifications = mutableStateListOf<NotificationUi>()

    val employees = MockRepository.employees
    val workshop = MockRepository.workshop
    val rabbits = MockRepository.rabbits
    val allCages = MockRepository.allCages
    val operations = MockRepository.operationDefinitions
    private var serverCells by mutableStateOf<List<CellDto>>(emptyList())
    private val deviceId: String by lazy {
        Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            .orEmpty()
            .ifBlank { android.os.Build.MODEL }
    }

    init {
        safeLaunch("Notification subscription failed") {
            notificationRepository.notifications.collect { items ->
                notifications.clear()
                notifications.addAll(items)
            }
        }
        if (sessionStore.currentUser != null) {
            onLoggedInFromSession()
        }
    }

    fun consumeAppError(id: Long) {
        if (appError?.id == id) appError = null
    }

    private fun safeLaunch(
        logMessage: String,
        fallbackMessage: String = "Произошла непредвиденная ошибка",
        block: suspend () -> Unit,
    ): Job = viewModelScope.launch(globalErrorHandler) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            handleError(error, fallbackMessage, logMessage)
        }
    }

    private fun launchServerAction(
        logMessage: String,
        fallbackMessage: String,
        block: suspend () -> Unit,
    ): Job? {
        if (isServerActionInProgress) return null
        isServerActionInProgress = true
        return safeLaunch(logMessage, fallbackMessage) {
            try {
                block()
            } finally {
                isServerActionInProgress = false
            }
        }
    }

    private fun handleError(
        error: Throwable,
        fallbackMessage: String,
        logMessage: String,
        showToUser: Boolean = true,
    ) {
        if (error is CancellationException) throw error
        Log.e(API_LOG_TAG, logMessage, error)
        val message = error.toUserMessage(fallbackMessage)
        lastMessage = message
        if (showToUser) {
            appError = AppErrorMessage(++nextErrorId, message)
        }
    }

    fun navigate(target: AppScreen) { screen = target; lastMessage = null }
    fun onLoggedInFromSession() {
        val sessionUser = sessionStore.currentUser
        val role = sessionUser?.role
        val mockEmployee = when (role) {
            UserRole.Technologist,
            UserRole.SuperAdmin -> employees.first { it.role == RoleId.CHIEF_TECHNOLOGIST }
            UserRole.Operator, null -> employees.first { it.role == RoleId.OPERATOR }
        }
        val displayName = sessionUser?.displayName
            .orEmpty()
            .ifBlank { mockEmployee.fullName }
        val initials = displayName
            .split(" ")
            .filter(String::isNotBlank)
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifBlank { mockEmployee.initials }

        currentEmployee = mockEmployee.copy(
            id = sessionUser?.id ?: mockEmployee.id,
            fullName = displayName,
            initials = initials,
        )
        shift = ShiftState(currentEmployee.id)
        screen = defaultScreenForRole()
        lastMessage = null
        refreshProfileAndTasks()
        startTasksAutoRefresh()
    }
    fun logout() {
        stopTasksAutoRefresh()
        notificationRepository.clear()
        screen = AppScreen.Login
        safeLaunch("Logout failed", fallbackMessage = "Не удалось выйти из профиля") {
            authRepository.logout()
        }
    }
    fun startShift() {
        if (isShiftActionInProgress) return
        isShiftActionInProgress = true
        safeLaunch("Open shift action failed", fallbackMessage = "Не удалось открыть смену") {
            try {
                runCatching { profileApi.openShift() }
                    .onSuccess { remoteShift ->
                        shift = remoteShift.toShiftState(currentEmployee.id, shift)
                        lastMessage = "Смена открыта"
                        loadMyTasks()
                    }
                    .onFailure { error ->
                        if (error is HttpException && error.code() == 400) {
                            runCatching { profileApi.getMyProfile() }
                                .onSuccess { profile ->
                                    if (profile.shift?.isOpen == true) {
                                        shift = profile.shift.toShiftState(currentEmployee.id, shift)
                                        lastMessage = "Смена уже открыта"
                                        loadMyTasks()
                                    } else {
                                        handleError(error, "Не удалось открыть смену", "Open shift failed")
                                    }
                                }
                                .onFailure {
                                    handleError(error, "Не удалось открыть смену", "Open shift failed")
                                }
                        } else {
                            handleError(error, "Не удалось открыть смену", "Open shift failed")
                        }
                    }
            } finally {
                isShiftActionInProgress = false
            }
        }
    }
    fun finishShift(reason: String) {
        if (isShiftActionInProgress) return
        isShiftActionInProgress = true
        safeLaunch("Close shift action failed", fallbackMessage = "Не удалось закрыть смену") {
            try {
                runCatching { profileApi.closeShift() }
                    .onSuccess { remoteShift ->
                        shift = remoteShift.toShiftState(currentEmployee.id, shift)
                        lastMessage = "Смена завершена"
                    }
                    .onFailure { error ->
                        handleError(error, "Не удалось закрыть смену", "Close shift failed. reason=$reason")
                    }
            } finally {
                isShiftActionInProgress = false
            }
        }
    }

    private fun refreshProfileAndTasks() {
        safeLaunch("Profile and tasks refresh failed") {
            runCatching { profileApi.getMyProfile() }
                .onSuccess { profile ->
                    currentEmployee = currentEmployee.copy(id = profile.employeeId)
                    shift = profile.shift.toShiftState(currentEmployee.id, shift)
                }
                .onFailure { error -> handleError(error, "Не удалось обновить профиль", "Profile refresh failed") }
            loadMyTasks()
        }
    }

    private suspend fun loadMyTasks(showLoading: Boolean = true) {
        if (isTasksRequestInProgress) {
            isTasksReloadRequested = true
            return
        }
        isTasksRequestInProgress = true
        if (showLoading) isTasksLoading = true
        try {
            val productionTasks = if (currentEmployee.role == RoleId.OPERATOR) {
                runCatching {
                    productionTaskApi.getEmployeeTasks(currentEmployee.id, currentEmployee.id)
                        .map { productionTaskApi.getTask(currentEmployee.id, it.id).toMobileTask(currentEmployee.id) }
                }.onFailure { error ->
                    Log.e(API_LOG_TAG, "Production tasks request failed", error)
                }.getOrDefault(emptyList())
            } else emptyList()
            runCatching {
                if (currentEmployee.role == RoleId.CHIEF_TECHNOLOGIST) {
                    val ownTasks = workTaskApi.getMyWorkTasks()
                    val acceptanceTasks = workTaskApi.getWorkTasksForAcceptance()
                    ru.profikrol.operator.data.remote.worktask.WorkTaskPageDto(
                        items = (ownTasks.items + acceptanceTasks.items).distinctBy(WorkTaskDto::id),
                        total = (ownTasks.items + acceptanceTasks.items).distinctBy(WorkTaskDto::id).size,
                    )
                } else {
                    workTaskApi.getMyWorkTasks()
                }
            }
                .onSuccess { page ->
                    val latestTasks = page.items
                        .groupBy { task ->
                            task.scheduledDate to (task.operationId ?: "work-task:${task.id}")
                        }
                        .values
                        .mapNotNull { duplicates ->
                            duplicates.maxWithOrNull(
                                compareBy<WorkTaskDto> { it.programScheduleId ?: Long.MIN_VALUE }
                                    .thenBy { it.id },
                            )
                        }
                    val needsRabbitChecklist = latestTasks.any { task ->
                        task.resolveOperationType() == OperationType.INSEMINATION ||
                            (task.subtasks.isEmpty() &&
                                MockRepository.operation(task.resolveOperationType()).targetType == TargetType.RABBIT)
                    }
                    val rabbits = if (needsRabbitChecklist) {
                        runCatching { loadAllRabbits() }
                            .onFailure { error ->
                                handleError(
                                    error = error,
                                    fallbackMessage = "Не удалось загрузить список кроликов",
                                    logMessage = "Rabbits request failed",
                                    showToUser = showLoading,
                                )
                            }
                            .getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    val needsCells = latestTasks.any {
                        it.resolveOperationType() == OperationType.ANIMAL_TRANSFER ||
                            MockRepository.operation(it.resolveOperationType()).targetType == TargetType.CAGE
                    }
                    val cells = if (needsCells) {
                        runCatching { loadAllCells() }
                            .onSuccess { serverCells = it }
                            .onFailure { error ->
                                handleError(
                                    error = error,
                                    fallbackMessage = "Не удалось загрузить список клеток",
                                    logMessage = "Cells request failed",
                                    showToUser = showLoading,
                                )
                            }
                            .getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    val remoteTasks = latestTasks
                        // Animal settlement is owned by Production API. A legacy task
                        // has numeric subtask ids and cannot complete a targetTask UUID.
                        .filterNot { it.resolveOperationType() == OperationType.ANIMAL_SETTLEMENT }
                        .map { dto ->
                        dto.toMobileTask(currentEmployee.id, rabbits, cells).let { task ->
                            if (
                                currentEmployee.role == RoleId.CHIEF_TECHNOLOGIST &&
                                dto.requiresAcceptance &&
                                dto.status.normalizedStatus() == "AWAITING_ACCEPTANCE"
                            ) {
                                task.copy(
                                    acceptanceRole = currentEmployee.role,
                                    acceptanceStatus = AcceptanceStatus.WAITING,
                                )
                            } else task
                        }
                    }
                    val loadedTasks = (productionTasks + remoteTasks).distinctBy(MobileTask::id)
                    val showcaseTransferTask = MockRepository.initialTasks()
                        .firstOrNull { it.operationType == OperationType.ANIMAL_TRANSFER }
                        ?.copy(
                            id = "demo-animal-transfer",
                            assignedEmployeeId = currentEmployee.id,
                            plannedStart = "00:00",
                            priority = Priority.URGENT,
                            status = TaskStatus.NEW,
                        )
                    tasks = if (
                        showcaseTransferTask != null &&
                        loadedTasks.none { it.operationType == OperationType.ANIMAL_TRANSFER }
                    ) {
                        (listOf(showcaseTransferTask) + loadedTasks).distinctBy(MobileTask::id)
                    } else {
                        loadedTasks
                    }
                    hasLoadedRemoteTasks = true
                    if (
                        currentEmployee.role == RoleId.CHIEF_TECHNOLOGIST &&
                        remoteTasks.isNotEmpty() &&
                        screen == AppScreen.Shift
                    ) {
                        screen = AppScreen.AcceptanceQueue
                    }
                }
                .onFailure { error ->
                    handleError(
                        error = error,
                        fallbackMessage = "Не удалось загрузить задачи",
                        logMessage = "Work tasks request failed",
                        showToUser = showLoading,
                    )
                }
        } finally {
            isTasksRequestInProgress = false
            if (showLoading) isTasksLoading = false
            if (isTasksReloadRequested) {
                isTasksReloadRequested = false
                loadMyTasks(showLoading = false)
            }
        }
    }

    private suspend fun waitForTaskToOpen(taskId: String): Boolean {
        repeat(START_TASK_STATUS_POLL_ATTEMPTS) {
            loadMyTasks(showLoading = false)
            val status = taskOrNull(taskId)?.status
            if (status != null && status != TaskStatus.NEW) return true
            delay(START_TASK_STATUS_POLL_DELAY_MS)
        }
        return false
    }

    private suspend fun loadAllRabbits(): List<RabbitDto> {
        val result = mutableListOf<RabbitDto>()
        val knownKeys = mutableSetOf<String>()
        var page = 1
        while (true) {
            val batch = rabbitApi.getRabbits(page = page, pageSize = RABBITS_PAGE_SIZE)
            val newItems = batch.items.filter { rabbit ->
                val key = rabbit.id?.toString() ?: rabbit.rfid?.trim()?.lowercase().orEmpty()
                key.isNotBlank() && knownKeys.add(key)
            }
            result += newItems
            if (page >= batch.totalPages || newItems.isEmpty()) break
            page += 1
        }
        return result
    }

    private suspend fun loadAllCells(): List<CellDto> {
        val result = mutableListOf<CellDto>()
        var page = 1
        while (true) {
            val batch = cellApi.getCells(page = page, pageSize = CELLS_PAGE_SIZE)
            result += batch.items
            if (page >= batch.totalPages || batch.items.isEmpty()) break
            page += 1
        }
        return result.distinctBy(CellDto::id)
    }

    fun startTasksAutoRefresh() {
        if (sessionStore.currentUser == null || tasksAutoRefreshJob?.isActive == true) return
        tasksAutoRefreshJob = safeLaunch("Tasks auto refresh failed", fallbackMessage = "Не удалось обновить задачи") {
            loadMyTasks(showLoading = false)
            while (currentCoroutineContext().isActive) {
                delay(TASKS_REFRESH_INTERVAL_MS)
                loadMyTasks(showLoading = false)
            }
        }
    }

    fun stopTasksAutoRefresh() {
        tasksAutoRefreshJob?.cancel()
        tasksAutoRefreshJob = null
    }
    fun setOnline(isOnline: Boolean) {
        if (shift.isOnline == isOnline) return
        val pending = shift.pendingSyncEvents
        shift = shift.copy(isOnline = isOnline)
        lastMessage = if (isOnline) {
            if (pending > 0) "Интернет появился: можно синхронизировать накопленные изменения" else "Онлайн режим включен"
        } else {
            "Нет интернета: приложение перешло в офлайн режим"
        }
    }

    fun syncNow() {
        val hasPending = shift.pendingSyncEvents > 0 || tasks.any { it.offlineEvents > 0 }
        if (!shift.isOnline) {
            lastMessage = "Нет интернета: синхронизация будет доступна после перехода онлайн"
            return
        }
        if (!hasPending) {
            lastMessage = "Нет изменений для синхронизации"
            return
        }
        shift = shift.copy(pendingSyncEvents = 0)
        tasks = tasks.map { it.copy(offlineEvents = 0) }
        lastMessage = "Очередь синхронизации отправлена"
    }
    fun markNotificationAsRead(id: Long) {
        notificationRepository.markAsRead(id)
    }
    fun markAllNotificationsAsRead() {
        notificationRepository.markAllAsRead()
    }

    fun task(id: String) = tasks.first { it.id == id }
    fun taskOrNull(id: String) = tasks.firstOrNull { it.id == id }
    fun scannedRfidForTask(taskId: String): String? = scannedRfidByTaskId[taskId] ?: task(taskId).result.scannedRfid
    fun rememberScannedRfid(taskId: String, rfid: String) {
        lastScannedRfid = rfid
        scannedRfidByTaskId[taskId] = rfid
    }
    fun nextPendingRfid(taskId: String): String? {
        val item = task(taskId).checklist.firstOrNull { it.status == ChecklistStatus.PENDING } ?: return null
        return when (item.targetType) {
            TargetType.RABBIT -> MockRepository.rabbit(item.targetId)?.rfid ?: item.targetId
            TargetType.CAGE -> allCages.firstOrNull { it.id == item.targetId }?.rfid
            TargetType.ROW,
            TargetType.HANGAR -> null
        }
    }
    fun canReviewAcceptance() = tasks.any { it.acceptanceRole == currentEmployee.role }
    fun tasksForCurrentEmployee() = if (hasLoadedRemoteTasks) {
        tasks
    } else {
        tasks.filter { task ->
            task.assignedEmployeeId == currentEmployee.id &&
                definition(task.operationType).allowedRoles.contains(currentEmployee.role)
        }
    }
    fun tasksForAcceptance() = tasks.filter { it.requiresAcceptance && it.status == TaskStatus.DONE && it.acceptanceStatus == AcceptanceStatus.WAITING && it.acceptanceRole == currentEmployee.role }
    fun nextTask() = tasksForCurrentEmployee().filter { it.status != TaskStatus.DONE && it.status != TaskStatus.SENT && it.status != TaskStatus.SKIPPED }.minWithOrNull(compareBy<MobileTask> { it.priority.weight }.thenBy { it.plannedStart })
    fun definition(type: OperationType): OperationDefinition {
        val definition = MockRepository.operation(type)
        if (serverCells.isEmpty()) return definition
        val cellOptions = serverCells.map(CellDto::displayName)
        return definition.copy(
            fields = definition.fields.map { field ->
                if (field.id == "sourceCage" || field.id == "destinationCage" || field.id == "cellId") {
                    field.copy(options = listOfNotNull(field.options.firstOrNull()) + cellOptions)
                } else field
            },
        )
    }
    private fun defaultScreenForRole(): AppScreen = when {
        currentEmployee.role == RoleId.CHIEF_TECHNOLOGIST && tasksForAcceptance().isNotEmpty() -> AppScreen.AcceptanceQueue
        else -> AppScreen.Shift
    }

    private fun queueOfflineChange(): ShiftState =
        if (shift.isOnline) shift else shift.copy(pendingSyncEvents = shift.pendingSyncEvents + 1)

    private fun updateTask(taskId: String, transform: (MobileTask) -> MobileTask) {
        tasks = tasks.map { task ->
            if (task.id == taskId) {
                val updated = transform(task)
                if (shift.isOnline) updated.copy(offlineEvents = task.offlineEvents) else updated
            } else {
                task
            }
        }
        shift = queueOfflineChange()
    }
    fun beginTask(taskId: String) {
        val remoteTaskId = taskId.toLongOrNull()
        if (remoteTaskId == null) {
            val task = taskOrNull(taskId)
            if (task?.checklist?.any { it.serverType == "production-target" } == true) {
                launchServerAction("Start production task action failed", fallbackMessage = "Не удалось начать задачу") {
                    runCatching { productionTaskApi.startTask(currentEmployee.id, taskId) }
                        .onSuccess { details ->
                            val updated = details.toMobileTask(currentEmployee.id)
                            tasks = tasks.map { if (it.id == taskId) updated else it }
                            lastMessage = "Задача начата"
                        }
                        .onFailure { error ->
                            handleError(error, "Не удалось начать задачу", "Start production task failed. taskId=$taskId")
                        }
                }
            } else {
                updateTask(taskId) { it.copy(status = TaskStatus.IN_PROGRESS).markOffline() }
            }
            return
        }
        launchServerAction("Start work task action failed", fallbackMessage = "Не удалось начать задачу") {
            runCatching { workTaskApi.startWorkTask(remoteTaskId) }
                .onSuccess { remoteTask ->
                    updateTask(taskId) { task ->
                        task.copy(
                            status = remoteTask.status.toTaskStatus(),
                            plannedStart = remoteTask.startedAt.toDisplayTime(),
                        )
                    }
                    lastMessage = "Задача начата"
                }
                .onFailure { error ->
                    if (error is HttpException && error.code() == 409) {
                        lastMessage = "Открываем задачу..."
                        val opened = waitForTaskToOpen(taskId)
                        if (opened) {
                            lastMessage = "Задача открыта"
                        } else {
                            updateTask(taskId) { task ->
                                task.copy(status = TaskStatus.IN_PROGRESS)
                            }
                            lastMessage = "Задача открыта, данные обновятся автоматически"
                        }
                    } else {
                        handleError(error, "Не удалось начать задачу", "Start work task failed. taskId=$remoteTaskId")
                    }
                }
        }
    }

    fun updateTaskValue(taskId: String, key: String, value: String) = updateTask(taskId) { it.copy(result = it.result.copy(values = it.result.values + (key to value))).markOffline() }
    private fun media(type: AttachmentType, label: String, localUri: String) = MediaAttachment(
        id = "media-${System.currentTimeMillis()}",
        type = type,
        name = label,
        localUri = localUri,
        createdAt = "now",
        uploaded = false
    )
    fun addPhoto(taskId: String, label: String, localUri: String) = updateTask(taskId) { val attachment = media(AttachmentType.PHOTO, label, localUri); it.copy(result = it.result.copy(photos = it.result.photos + label, attachments = it.result.attachments + attachment)).markOffline() }
    fun addVideo(taskId: String, label: String, localUri: String) = updateTask(taskId) { val attachment = media(AttachmentType.VIDEO, label, localUri); it.copy(result = it.result.copy(videos = it.result.videos + label, attachments = it.result.attachments + attachment)).markOffline() }
    fun addFile(taskId: String, label: String, localUri: String) = updateTask(taskId) { val attachment = media(AttachmentType.FILE, label, localUri); it.copy(result = it.result.copy(attachments = it.result.attachments + attachment)).markOffline() }
    fun addComment(taskId: String, comment: String) = updateTask(taskId) { it.copy(result = it.result.copy(comment = comment)).markOffline() }

    fun scanRfidAndCompleteItem(taskId: String, rfid: String, values: Map<String, String> = emptyMap()) {
        Log.d("RFID_TEST", "MobileMesViewModel получил: $rfid")

        val currentTask = tasks.first { it.id == taskId }
        Log.d(
            "RFID_SETTLEMENT",
            "VM received click. taskId=$taskId operation=${currentTask.operationType} checklist=${currentTask.checklist.size} productionTargets=${currentTask.checklist.count { it.serverType == "production-target" }} rfid=$rfid",
        )
        val normalizedRfid = rfid.trim()
        if (normalizedRfid.isBlank()) {
            lastMessage = "RFID не может быть пустым"
            return
        }
        if (currentTask.checklist.any { it.result.scannedRfid.equals(normalizedRfid, ignoreCase = true) }) {
            lastMessage = "RFID $normalizedRfid уже использован в этой задаче"
            return
        }
        val pendingServerRabbits = currentTask.checklist.filter { item ->
            item.targetType == TargetType.RABBIT && item.status == ChecklistStatus.PENDING
        }
        val serverRabbitTarget = pendingServerRabbits.firstOrNull { item ->
            item.targetType == TargetType.RABBIT && item.targetId.equals(rfid, ignoreCase = true)
        } ?: pendingServerRabbits.singleOrNull()
        val productionSettlementTarget = if (currentTask.operationType == OperationType.ANIMAL_SETTLEMENT) {
            currentTask.checklist.firstOrNull { item ->
                item.serverType == "production-target" && item.status == ChecklistStatus.PENDING
            }
        } else null
        val settlementFallbackTarget = if (currentTask.operationType == OperationType.ANIMAL_SETTLEMENT) {
            currentTask.checklist.firstOrNull { it.status == ChecklistStatus.PENDING }
        } else null
        val effectiveRfid = if (productionSettlementTarget != null || settlementFallbackTarget != null) rfid else serverRabbitTarget?.targetId ?: rfid
        rememberScannedRfid(taskId, effectiveRfid)

        val rabbit = MockRepository.rabbitByRfid(effectiveRfid)
        val cage = MockRepository.cageByRfid(effectiveRfid)
        val targetId = productionSettlementTarget?.targetId
            ?: settlementFallbackTarget?.targetId
            ?: serverRabbitTarget?.targetId
            ?: rabbit?.id
            ?: cage?.id
        val problemReason = values[PROBLEM_REASON_KEY].orEmpty()
        val problemComment = values[PROBLEM_COMMENT_KEY].orEmpty()
        val resultValues = values - PROBLEM_REASON_KEY - PROBLEM_COMMENT_KEY

        if (targetId == null && problemReason.isBlank()) {
            Log.w("RFID_SETTLEMENT", "No target resolved; request is not sent. taskId=$taskId")
            updateTask(taskId) { task ->
                task.copy(
                    status = TaskStatus.IN_PROGRESS,
                    result = task.result.copy(
                        scannedRfid = effectiveRfid,
                        values = task.result.values + resultValues + ("lastScan" to effectiveRfid),
                    ),
                ).markOffline()
            }
            lastMessage = "RFID сохранен: $effectiveRfid"
            return
        }

        val scannedItem = targetId?.let { scannedTargetId ->
            currentTask.checklist.firstOrNull { it.targetId == scannedTargetId }
        }
        val matchingItem = if (problemReason.isNotBlank()) {
            scannedItem?.takeIf { it.status == ChecklistStatus.PENDING }
                ?: currentTask.checklist.firstOrNull { it.status == ChecklistStatus.PENDING }
        } else {
            scannedItem ?: currentTask.checklist
                .filter { it.status == ChecklistStatus.PENDING }
                .singleOrNull()
        }
        if (matchingItem == null) {
            Log.w("RFID_SETTLEMENT", "No matching checklist item; request is not sent. taskId=$taskId targetId=$targetId")
            updateTask(taskId) { task ->
                task.copy(
                    status = TaskStatus.IN_PROGRESS,
                    result = task.result.copy(
                        scannedRfid = effectiveRfid,
                        values = task.result.values + resultValues + ("lastScan" to effectiveRfid),
                    ),
                ).markOffline()
            }
            lastMessage = "RFID сохранен: $effectiveRfid"
            return
        }
        if (matchingItem.status != ChecklistStatus.PENDING) {
            Log.w("RFID_SETTLEMENT", "Target is not pending. itemId=${matchingItem.id} status=${matchingItem.status}")
            lastMessage = "Пункт чек-листа уже обработан: ${matchingItem.label}"
            return
        }
        if (matchingItem.serverType == "production-target") {
            Log.d("RFID_SETTLEMENT", "Production target selected. taskId=$taskId targetId=${matchingItem.id} rfid=$effectiveRfid")
            completeChecklistItemOnServer(
                taskId = taskId,
                itemId = matchingItem.id,
                status = if (problemReason.isBlank()) ChecklistStatus.DONE else ChecklistStatus.PROBLEM,
                reason = problemReason,
                comment = problemComment,
                values = resultValues + ("rfid" to effectiveRfid),
            )
            return
        }
        if (matchingItem.id.toLongOrNull() != null && taskId.toLongOrNull() != null) {
            completeChecklistItemOnServer(
                taskId = taskId,
                itemId = matchingItem.id,
                status = if (problemReason.isBlank()) ChecklistStatus.DONE else ChecklistStatus.PROBLEM,
                reason = problemReason,
                comment = problemComment,
                values = resultValues + ("rfid" to effectiveRfid),
            )
            return
        }
        updateTask(taskId) { task ->
            val checklist = task.checklist.map { item ->
                if (item.targetId.equals(targetId, ignoreCase = true)) {
                    item.copy(
                        status = if (problemReason.isBlank()) ChecklistStatus.DONE else ChecklistStatus.PROBLEM,
                        result = item.result.copy(
                            values = item.result.values + resultValues,
                            scannedRfid = effectiveRfid,
                            completedAt = "now",
                            problemReason = problemReason.ifBlank { null },
                            comment = problemComment,
                        ),
                    )
                } else item
            }
            task.copy(
                status = TaskStatus.IN_PROGRESS,
                checklist = checklist,
                result = task.result.copy(
                    scannedRfid = rfid,
                    values = (task.result.values - PROBLEM_REASON_KEY - PROBLEM_COMMENT_KEY) +
                        resultValues +
                        ("lastScan" to effectiveRfid),
                ),
            ).markOffline()
        }
        lastMessage = if (problemReason.isBlank()) {
            "Скан принят: $effectiveRfid. Пункт чек-листа закрыт автоматически."
        } else {
            "Замечание сохранено: $problemReason"
        }
    }

    fun markChecklistItem(
        taskId: String,
        itemId: String,
        status: ChecklistStatus,
        reason: String = "",
        comment: String = "",
    ) {
        if (status == ChecklistStatus.DONE || status == ChecklistStatus.PROBLEM) {
            completeChecklistItemOnServer(
                taskId = taskId,
                itemId = itemId,
                status = status,
                reason = reason,
                comment = comment,
            )
            return
        }
        updateChecklistItemLocally(taskId, itemId, status, reason, comment)
    }

    fun completeChecklistItem(taskId: String, itemId: String, values: Map<String, String>) {
        completeChecklistItemOnServer(
            taskId = taskId,
            itemId = itemId,
            status = ChecklistStatus.DONE,
            values = values,
        )
    }

    private fun completeChecklistItemOnServer(
        taskId: String,
        itemId: String,
        status: ChecklistStatus,
        reason: String = "",
        comment: String = "",
        values: Map<String, String> = emptyMap(),
    ) {
        val task = taskOrNull(taskId) ?: return
        val item = task.checklist.firstOrNull { it.id == itemId }
        if (item?.serverType == "production-target") {
            val rfid = values["rfid"]?.trim()
            if (task.operationType == OperationType.ANIMAL_SETTLEMENT && rfid.isNullOrBlank()) {
                lastMessage = "Для заселения RFID обязателен"
                return
            }
            val isAnimalTargetTask = task.operationType == OperationType.ANIMAL_SETTLEMENT ||
                task.operationType == OperationType.ANIMAL_TRANSFER
            val operationTitle = if (task.operationType == OperationType.ANIMAL_TRANSFER) "Переселение" else "Заселение"
            val resultJson = buildJsonObject {
                values.filterKeys { it != "rfid" && it != PROBLEM_REASON_KEY && it != PROBLEM_COMMENT_KEY }
                    .forEach { (key, value) -> put(key, value) }
                if (task.operationType == OperationType.ANIMAL_SETTLEMENT) {
                    // One RFID scan represents one newly settled rabbit.
                    // Production API validates this value as a JSON integer.
                    put("animalCount", 1)
                }
            }.toString()
            launchServerAction("Complete production target failed", fallbackMessage = "Не удалось сохранить заселение") {
                Log.d(API_LOG_TAG, "Sending production target completion. taskId=$taskId targetId=$itemId rfid=$rfid")
                runCatching {
                    productionTaskApi.completeTarget(
                        employeeId = currentEmployee.id,
                        taskId = taskId,
                        targetId = itemId,
                        request = CompleteTargetRequest(
                            employeeId = currentEmployee.id,
                            resultJson = resultJson,
                            rfid = rfid,
                            deviceId = deviceId,
                        ),
                    )
                }.onSuccess {
                    updateChecklistItemLocally(taskId, itemId, status, reason, comment, values)
                    val isLastTarget = task.checklist.count { it.status == ChecklistStatus.PENDING } == 1
                    if (isLastTarget && isAnimalTargetTask) {
                        runCatching { productionTaskApi.completeTask(currentEmployee.id, taskId) }
                            .onSuccess {
                                updateTask(taskId) { current -> current.copy(status = TaskStatus.DONE, result = current.result.copy(completedAt = "now")) }
                                lastMessage = "$operationTitle успешно завершено"
                            }
                            .onFailure { error -> handleError(error, "RFID сохранён, но задачу не удалось закрыть", "Complete production animal target task failed. taskId=$taskId operation=${task.operationType}") }
                    } else {
                        lastMessage = if (rfid != null) "RFID сохранён: $rfid" else "Позиция выполнена"
                    }
                }.onFailure { error ->
                    handleError(error, "Не удалось сохранить результат", "Complete production target failed. taskId=$taskId targetId=$itemId")
                }
            }
            return
        }
        val subtaskId = itemId.toLongOrNull()
        val isRemoteTask = taskId.toLongOrNull() != null
        if (subtaskId == null || !isRemoteTask) {
            updateChecklistItemLocally(taskId, itemId, status, reason, comment, values)
            return
        }

        launchServerAction("Complete work subtask action failed", fallbackMessage = "Не удалось завершить подзадачу") {
            val rfid = values["rfid"]?.trim().orEmpty()
            val reportComment = buildList {
                if (rfid.isNotEmpty()) add("RFID: $rfid")
                if (comment.isNotBlank()) add(comment.trim())
            }.joinToString("; ").ifBlank { null }
            runCatching {
                workTaskApi.completeWorkSubtask(
                    subtaskId = subtaskId,
                    request = CompleteWorkSubtaskRequest(
                        abortReason = reason.ifBlank { null },
                        comment = reportComment,
                        // The legacy API accepts only its operation-specific params DTO.
                        // RFID belongs to the production target API, so keep it in the
                        // legacy report comment instead of sending an incompatible map.
                        params = null,
                    ),
                )
            }.onSuccess {
                updateChecklistItemLocally(taskId, itemId, status, reason, comment, values)
                val current = taskOrNull(taskId)
                val isLastSettlementItem = current?.operationType == OperationType.ANIMAL_SETTLEMENT &&
                    current.checklist.count { it.status == ChecklistStatus.PENDING } <= 1
                if (isLastSettlementItem) {
                    runCatching {
                        workTaskApi.completeWorkTask(
                            id = taskId.toLong(),
                            request = CompleteWorkTaskRequest(comment = reportComment),
                        )
                    }.onSuccess { completedTask ->
                        updateTask(taskId) { task -> task.copy(status = completedTask.status.toTaskStatus(), result = task.result.copy(completedAt = completedTask.completedAt ?: "now")) }
                        lastMessage = "Заселение успешно завершено"
                    }.onFailure { error ->
                        handleError(error, "RFID сохранён, но задачу не удалось закрыть", "Complete legacy settlement task failed. taskId=$taskId")
                    }
                } else if (reason.isBlank()) {
                    lastMessage = "Подзадача выполнена"
                } else {
                    lastMessage = "Подзадача завершена с замечанием"
                }
            }.onFailure { error ->
                handleError(error, "Не удалось завершить подзадачу", "Complete work subtask failed. subtaskId=$subtaskId")
            }
        }
    }

    private fun updateChecklistItemLocally(
        taskId: String,
        itemId: String,
        status: ChecklistStatus,
        reason: String = "",
        comment: String = "",
        values: Map<String, String> = emptyMap(),
    ) = updateTask(taskId) { task ->
        task.copy(
            status = TaskStatus.IN_PROGRESS,
            checklist = task.checklist.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        status = status,
                        result = item.result.copy(
                            values = item.result.values + values,
                            scannedRfid = values["rfid"] ?: item.result.scannedRfid,
                            completedAt = "now",
                            problemReason = reason.ifBlank { null },
                            comment = comment,
                        ),
                    )
                } else {
                    item
                }
            },
            result = task.result.copy(values = task.result.values + values),
        ).markOffline()
    }

    fun completeTask(taskId: String, commentOverride: String? = null) {
        val currentTask = tasks.first { it.id == taskId }
        val completionComment = commentOverride ?: currentTask.result.comment
        val checklist = if (USE_GENERAL_TEMPLATE_FOR_ALL_OPERATIONS) {
            currentTask.checklist.map { item ->
                if (item.status == ChecklistStatus.PENDING) item.copy(status = ChecklistStatus.DONE) else item
            }
        } else currentTask.checklist
        val pending = checklist.count { it.status == ChecklistStatus.PENDING }
        if (pending > 0) {
            lastMessage = "Нельзя завершить задачу: осталось $pending необработанных пунктов чек-листа"
            return
        }
        val remoteTaskId = taskId.toLongOrNull()
        if (remoteTaskId == null) {
            if (currentTask.checklist.any { it.serverType == "production-target" }) {
                launchServerAction("Complete production task failed", fallbackMessage = "Не удалось завершить задачу") {
                    runCatching { productionTaskApi.completeTask(currentEmployee.id, taskId) }
                        .onSuccess {
                            updateTask(taskId) { current -> current.copy(status = TaskStatus.DONE, checklist = checklist, result = current.result.copy(completedAt = "now")) }
                            lastMessage = "Задача завершена"
                        }
                        .onFailure { error -> handleError(error, "Не удалось завершить задачу", "Complete production task failed. taskId=$taskId") }
                }
            } else {
                updateTask(taskId) { current ->
                    val acceptance = if (current.requiresAcceptance) AcceptanceStatus.WAITING else AcceptanceStatus.NOT_REQUIRED
                    current.copy(
                        status = TaskStatus.DONE,
                        acceptanceStatus = acceptance,
                        checklist = checklist,
                        result = current.result.copy(completedAt = "now"),
                    ).markOffline()
                }
            }
            return
        }

        launchServerAction("Complete work task action failed", fallbackMessage = "Не удалось завершить задачу") {
            runCatching {
                if (currentTask.isGeneral) {
                    currentTask.pendingGeneralSubtaskIds.forEach { subtaskId ->
                        workTaskApi.completeWorkSubtask(
                            subtaskId = subtaskId,
                            request = CompleteWorkSubtaskRequest(),
                        )
                    }
                }
                workTaskApi.completeWorkTask(
                    id = remoteTaskId,
                    request = CompleteWorkTaskRequest(
                        abortReason = currentTask.result.problemReason,
                        comment = completionComment.ifBlank { null },
                    ),
                )
            }.onSuccess { remoteTask ->
                updateTask(taskId) { current ->
                    current.copy(
                        status = remoteTask.status.toTaskStatus(),
                        acceptanceStatus = if (current.requiresAcceptance) {
                            AcceptanceStatus.WAITING
                        } else {
                            AcceptanceStatus.NOT_REQUIRED
                        },
                        checklist = checklist,
                        result = current.result.copy(completedAt = remoteTask.completedAt ?: "now"),
                    )
                }
                lastMessage = "Задача завершена"
            }.onFailure { error ->
                if (currentTask.isGeneral && error is HttpException && error.code() == 409) {
                    updateTask(taskId) { current ->
                        current.copy(
                            status = TaskStatus.DONE,
                            acceptanceStatus = if (current.requiresAcceptance) {
                                AcceptanceStatus.WAITING
                            } else {
                                AcceptanceStatus.NOT_REQUIRED
                            },
                            checklist = checklist,
                            result = current.result.copy(completedAt = "now"),
                        )
                    }
                    lastMessage = if (currentTask.requiresAcceptance) {
                        "Задача уже отправлена на приёмку"
                    } else {
                        "Задача уже завершена"
                    }
                } else {
                    handleError(error, "Не удалось завершить задачу", "Complete work task failed. taskId=$remoteTaskId")
                }
            }
        }
    }

    fun skipTask(taskId: String, reason: String) = updateTask(taskId) { it.copy(status = TaskStatus.SKIPPED, result = it.result.copy(problemReason = reason, comment = reason)).markOffline() }

    fun rejectGeneralTask(taskId: String, reason: String, commentOverride: String? = null) {
        val currentTask = tasks.first { it.id == taskId }
        val rejectionComment = commentOverride ?: currentTask.result.comment
        val remoteTaskId = taskId.toLongOrNull()
        if (remoteTaskId == null) {
            skipTask(taskId, reason)
            return
        }

        updateTask(taskId) { task ->
            task.copy(result = task.result.copy(problemReason = reason)).markOffline()
        }
        launchServerAction("Reject general work task action failed", fallbackMessage = "Не удалось отклонить задачу") {
            runCatching {
                workTaskApi.completeWorkTask(
                    id = remoteTaskId,
                    request = CompleteWorkTaskRequest(
                        abortReason = reason,
                            comment = rejectionComment.ifBlank { null },
                    ),
                )
            }.onSuccess { remoteTask ->
                updateTask(taskId) { task ->
                    task.copy(
                        status = remoteTask.status.toTaskStatus(),
                        result = task.result.copy(
                            problemReason = reason,
                            completedAt = remoteTask.completedAt ?: "now",
                        ),
                    )
                }
                lastMessage = "Задача отклонена"
            }.onFailure { error ->
                handleError(error, "Не удалось отклонить задачу", "Reject general work task failed. taskId=$remoteTaskId")
            }
        }
    }

    fun addRemark(taskId: String, itemId: String?, reason: String, comment: String, attachments: List<MediaAttachment>) {
        remarks = listOf(AcceptanceRemark("remark-${System.currentTimeMillis()}", taskId, itemId, reason, comment, attachments, "now")) + remarks
        if (itemId != null) updateTask(taskId) { task -> task.copy(checklist = task.checklist.map { if (it.id == itemId) it.copy(reviewStatus = ReviewStatus.REJECTED, reviewerComment = comment) else it }).markOffline() }
        else shift = queueOfflineChange()
    }

    fun acceptTask(taskId: String, comment: String = "") {
        val task = tasks.firstOrNull { it.id == taskId } ?: return
        val reportId = task.workReportId
        if (reportId == null) {
            lastMessage = "У задачи отсутствует серверный отчёт для приёмки"
            return
        }
        launchServerAction("Accept work report failed", fallbackMessage = "Не удалось подтвердить выполнение задачи") {
            runCatching { workTaskApi.acceptWorkReport(reportId) }
                .onSuccess { report ->
                    updateTask(taskId) { current ->
                        current.copy(
                            status = TaskStatus.SENT,
                            acceptanceStatus = AcceptanceStatus.ACCEPTED,
                            acceptedByEmployeeId = report.acceptedByEmployeeId ?: currentEmployee.id,
                            acceptanceComment = comment,
                            checklist = current.checklist.map { item ->
                                item.copy(reviewStatus = if (item.reviewStatus == ReviewStatus.REJECTED) ReviewStatus.REJECTED else ReviewStatus.ACCEPTED)
                            },
                        )
                    }
                    lastMessage = "Выполнение задачи подтверждено"
                }
                .onFailure { error ->
                    handleError(error, "Не удалось подтвердить выполнение задачи", "Accept work report failed. reportId=$reportId")
                }
        }
    }

    fun rejectTask(taskId: String, comment: String) {
        lastMessage = "Возврат на доработку пока не поддерживается сервером"
    }
}
