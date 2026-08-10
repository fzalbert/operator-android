package com.rabbitmes.mobile

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.domain.repository.AuthRepository
import ru.profikrol.operator.data.remote.profile.ProfileApi
import ru.profikrol.operator.data.remote.profile.ShiftDto
import ru.profikrol.operator.data.remote.rabbit.RabbitApi
import ru.profikrol.operator.data.remote.rabbit.RabbitDto
import ru.profikrol.operator.data.remote.worktask.WorkTaskApi
import ru.profikrol.operator.data.remote.worktask.WorkTaskDto
import ru.profikrol.operator.data.remote.worktask.CompleteWorkSubtaskRequest
import ru.profikrol.operator.data.remote.worktask.CompleteWorkTaskRequest
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

private const val API_LOG_TAG = "RabbitApi"
private const val TASKS_REFRESH_INTERVAL_MS = 30_000L
private const val RABBITS_PAGE_SIZE = 100

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
): MobileTask {
    val operationType = resolveOperationType()
    val isGeneral = operationType == OperationType.CUSTOM_TASK
    val targetType = MockRepository.operation(operationType).targetType
    val checklist = if (isGeneral) {
        emptyList()
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
        description = description.ifBlank {
            subtasks.map { it.description.trim() }
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
    )
}

private val COMPLETED_SUBTASK_STATUSES = setOf("COMPLETED", "DONE", "FINISHED", "SKIPPED")

private fun WorkTaskDto.resolveOperationType(): OperationType {
    val candidates = listOf(operationId, operationName, operationCategory)
        .map { it.orEmpty().trim() }
        .filter(String::isNotBlank)
    val resolved = OPERATION_ID_ALIASES[operationId.orEmpty().trim().lowercase()]
        ?: OperationType.entries.firstOrNull { type ->
            candidates.any { candidate ->
                candidate.equals(type.name, ignoreCase = true) ||
                    candidate.equals(type.title, ignoreCase = true)
            }
        }
    return resolved?.takeIf(SPECIALIZED_OPERATION_TYPES::contains)
        ?: OperationType.CUSTOM_TASK
}

private val SPECIALIZED_OPERATION_TYPES = setOf(
    OperationType.INSEMINATION,
    OperationType.NEST_PREPARATION,
    OperationType.NEST_SELECTION,
    OperationType.LACTATION_CONTROL,
    OperationType.WEIGHING,
    OperationType.LIGHT_STIMULATION,
    OperationType.LIGHTING_CHECK,
)

private val OPERATION_ID_ALIASES = mapOf(
    "animal_placement" to OperationType.ANIMAL_SETTLEMENT,
    "females_delivery" to OperationType.FEMALE_DELIVERY,
    "culling" to OperationType.ANIMAL_DEPARTURE,
    "light_check" to OperationType.LIGHTING_CHECK,
)

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
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val profileApi: ProfileApi,
    private val workTaskApi: WorkTaskApi,
    private val rabbitApi: RabbitApi,
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
    var isTasksLoading: Boolean by mutableStateOf(false)
        private set
    private var isTasksRequestInProgress = false
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
        safeLaunch("Open shift action failed", fallbackMessage = "Не удалось открыть смену") {
            isShiftActionInProgress = true
            try {
                runCatching { profileApi.openShift() }
                    .onSuccess { remoteShift ->
                        shift = remoteShift.toShiftState(currentEmployee.id, shift)
                        lastMessage = "Смена открыта"
                        loadMyTasks()
                    }
                    .onFailure { error ->
                        handleError(error, "Не удалось открыть смену", "Open shift failed")
                    }
            } finally {
                isShiftActionInProgress = false
            }
        }
    }
    fun finishShift(reason: String) {
        if (isShiftActionInProgress) return
        safeLaunch("Close shift action failed", fallbackMessage = "Не удалось закрыть смену") {
            isShiftActionInProgress = true
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
                    shift = profile.shift.toShiftState(currentEmployee.id, shift)
                }
                .onFailure { error -> handleError(error, "Не удалось обновить профиль", "Profile refresh failed") }
            loadMyTasks()
        }
    }

    private suspend fun loadMyTasks(showLoading: Boolean = true) {
        if (isTasksRequestInProgress) return
        isTasksRequestInProgress = true
        if (showLoading) isTasksLoading = true
        try {
            runCatching { workTaskApi.getMyWorkTasks() }
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
                        task.subtasks.isEmpty() &&
                            MockRepository.operation(task.resolveOperationType()).targetType == TargetType.RABBIT
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
                    val remoteTasks = latestTasks
                        .map { it.toMobileTask(currentEmployee.id, rabbits) }
                    tasks = remoteTasks
                    hasLoadedRemoteTasks = true
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
        }
    }

    private suspend fun loadAllRabbits(): List<RabbitDto> {
        val result = mutableListOf<RabbitDto>()
        val knownKeys = mutableSetOf<String>()
        var page = 1
        while (true) {
            val batch = rabbitApi.getRabbits(page = page, pageSize = RABBITS_PAGE_SIZE)
            val newItems = batch.filter { rabbit ->
                val key = rabbit.id?.toString() ?: rabbit.rfid?.trim()?.lowercase().orEmpty()
                key.isNotBlank() && knownKeys.add(key)
            }
            result += newItems
            if (batch.size < RABBITS_PAGE_SIZE || newItems.isEmpty()) break
            page += 1
        }
        return result
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
            TargetType.RABBIT -> rabbits.firstOrNull { it.id == item.targetId }?.rfid
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
    fun definition(type: OperationType) = MockRepository.operation(type)
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
            updateTask(taskId) { it.copy(status = TaskStatus.IN_PROGRESS).markOffline() }
            return
        }
        safeLaunch("Start work task action failed", fallbackMessage = "Не удалось начать задачу") {
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
                    handleError(error, "Не удалось начать задачу", "Start work task failed. taskId=$remoteTaskId")
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
        rememberScannedRfid(taskId, rfid)

        val currentTask = tasks.first { it.id == taskId }
        val serverRabbitTarget = currentTask.checklist.firstOrNull { item ->
            item.targetType == TargetType.RABBIT && item.targetId.equals(rfid, ignoreCase = true)
        }
        val rabbit = MockRepository.rabbitByRfid(rfid)
        val cage = MockRepository.cageByRfid(rfid)
        val targetId = rabbit?.id ?: cage?.id ?: serverRabbitTarget?.targetId
        val problemReason = values[PROBLEM_REASON_KEY].orEmpty()
        val problemComment = values[PROBLEM_COMMENT_KEY].orEmpty()
        val resultValues = values - PROBLEM_REASON_KEY - PROBLEM_COMMENT_KEY

        if (targetId == null && problemReason.isBlank()) {
            updateTask(taskId) { task ->
                task.copy(
                    status = TaskStatus.IN_PROGRESS,
                    result = task.result.copy(
                        scannedRfid = rfid,
                        values = task.result.values + resultValues + ("lastScan" to rfid),
                    ),
                ).markOffline()
            }
            lastMessage = "RFID сохранен: $rfid"
            return
        }

        val scannedItem = targetId?.let { scannedTargetId ->
            currentTask.checklist.firstOrNull { it.targetId == scannedTargetId }
        }
        val matchingItem = if (problemReason.isNotBlank()) {
            scannedItem?.takeIf { it.status == ChecklistStatus.PENDING }
                ?: currentTask.checklist.firstOrNull { it.status == ChecklistStatus.PENDING }
        } else {
            scannedItem
        }
        if (matchingItem == null) {
            updateTask(taskId) { task ->
                task.copy(
                    status = TaskStatus.IN_PROGRESS,
                    result = task.result.copy(
                        scannedRfid = rfid,
                        values = task.result.values + resultValues + ("lastScan" to rfid),
                    ),
                ).markOffline()
            }
            lastMessage = "RFID сохранен: $rfid"
            return
        }
        if (matchingItem.status != ChecklistStatus.PENDING) {
            lastMessage = "Пункт чек-листа уже обработан: ${matchingItem.label}"
            return
        }
        updateTask(taskId) { task ->
            val checklist = task.checklist.map { item ->
                if (item.targetId.equals(targetId, ignoreCase = true)) {
                    item.copy(
                        status = if (problemReason.isBlank()) ChecklistStatus.DONE else ChecklistStatus.PROBLEM,
                        result = item.result.copy(
                            values = item.result.values + resultValues,
                            scannedRfid = rfid,
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
                        ("lastScan" to rfid),
                ),
            ).markOffline()
        }
        lastMessage = if (problemReason.isBlank()) {
            "Скан принят: $rfid. Пункт чек-листа закрыт автоматически."
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
        val subtaskId = itemId.toLongOrNull()
        val isRemoteTask = taskId.toLongOrNull() != null
        if (subtaskId == null || !isRemoteTask) {
            updateChecklistItemLocally(taskId, itemId, status, reason, comment, values)
            return
        }

        safeLaunch("Complete work subtask action failed", fallbackMessage = "Не удалось завершить подзадачу") {
            runCatching {
                workTaskApi.completeWorkSubtask(
                    subtaskId = subtaskId,
                    request = CompleteWorkSubtaskRequest(
                        abortReason = reason.ifBlank { null },
                        comment = comment.ifBlank { null },
                    ),
                )
            }.onSuccess {
                updateChecklistItemLocally(taskId, itemId, status, reason, comment, values)
                lastMessage = if (reason.isBlank()) {
                    "Подзадача выполнена"
                } else {
                    "Подзадача завершена с замечанием"
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
        val checklist = if (currentTask.operationType == OperationType.NEST_CONTROL) {
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
            updateTask(taskId) { current ->
                val acceptance = if (current.requiresAcceptance) AcceptanceStatus.WAITING else AcceptanceStatus.NOT_REQUIRED
                current.copy(
                    status = TaskStatus.DONE,
                    acceptanceStatus = acceptance,
                    checklist = checklist,
                    result = current.result.copy(completedAt = "now"),
                ).markOffline()
            }
            return
        }

        safeLaunch("Complete work task action failed", fallbackMessage = "Не удалось завершить задачу") {
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
                handleError(error, "Не удалось завершить задачу", "Complete work task failed. taskId=$remoteTaskId")
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
        safeLaunch("Reject general work task action failed", fallbackMessage = "Не удалось отклонить задачу") {
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

    fun acceptTask(taskId: String, comment: String = "") = updateTask(taskId) { task ->
        task.copy(status = TaskStatus.SENT, acceptanceStatus = AcceptanceStatus.ACCEPTED, acceptedByEmployeeId = currentEmployee.id, acceptanceComment = comment, checklist = task.checklist.map { it.copy(reviewStatus = if (it.reviewStatus == ReviewStatus.REJECTED) ReviewStatus.REJECTED else ReviewStatus.ACCEPTED) }).markOffline()
    }
    fun rejectTask(taskId: String, comment: String) = updateTask(taskId) { it.copy(status = TaskStatus.BLOCKED, acceptanceStatus = AcceptanceStatus.REJECTED, acceptedByEmployeeId = currentEmployee.id, acceptanceComment = comment).markOffline() }
}
