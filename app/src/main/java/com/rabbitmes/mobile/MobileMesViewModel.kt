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
import javax.inject.Inject
import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.data.NotificationRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.operations.PROBLEM_COMMENT_KEY
import com.rabbitmes.mobile.ui.operations.PROBLEM_REASON_KEY

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

@HiltViewModel
class MobileMesViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    var screen: AppScreen by mutableStateOf(AppScreen.Login)
        private set
    var currentEmployee: Employee by mutableStateOf(MockRepository.employees.first())
        private set
    var shift: ShiftState by mutableStateOf(ShiftState(currentEmployee.id))
        private set
    var tasks: List<MobileTask> by mutableStateOf(MockRepository.initialTasks())
        private set
    var remarks: List<AcceptanceRemark> by mutableStateOf(emptyList())
        private set

    var lastScannedRfid: String? by mutableStateOf(null)
    private val scannedRfidByTaskId = mutableStateMapOf<String, String>()
    var lastMessage: String? by mutableStateOf(null)
        private set
    val notifications = mutableStateListOf<NotificationUi>()

    val employees = MockRepository.employees
    val workshop = MockRepository.workshop
    val rabbits = MockRepository.rabbits
    val allCages = MockRepository.allCages
    val operations = MockRepository.operationDefinitions

    init {
        viewModelScope.launch {
            notificationRepository.notifications.collect { items ->
                notifications.clear()
                notifications.addAll(items)
            }
        }
        if (sessionStore.currentUser != null) {
            onLoggedInFromSession()
        }
    }

    fun navigate(target: AppScreen) { screen = target; lastMessage = null }
    fun onLoggedInFromSession() {
        val role = sessionStore.currentUser?.role
        currentEmployee = when (role) {
            UserRole.Technologist,
            UserRole.SuperAdmin -> employees.first { it.role == RoleId.CHIEF_TECHNOLOGIST }
            UserRole.Operator, null -> employees.first { it.role == RoleId.OPERATOR }
        }
        shift = ShiftState(currentEmployee.id)
        screen = defaultScreenForRole()
        lastMessage = null
    }
    fun logout() {
        notificationRepository.clear()
        screen = AppScreen.Login
        viewModelScope.launch {
            authRepository.logout()
        }
    }
    fun startShift() { shift = shift.copy(startedAt = "08:00", finishedAt = null) }
    fun finishShift(reason: String) { shift = shift.copy(finishedAt = "18:00"); lastMessage = "Смена завершена: $reason" }
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
    fun tasksForCurrentEmployee() = tasks.filter { task ->
        task.assignedEmployeeId == currentEmployee.id &&
            definition(task.operationType).allowedRoles.contains(currentEmployee.role)
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
    fun beginTask(taskId: String) = updateTask(taskId) { it.copy(status = TaskStatus.IN_PROGRESS).markOffline() }

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

        val rabbit = MockRepository.rabbitByRfid(rfid)
        val cage = MockRepository.cageByRfid(rfid)
        val targetId = rabbit?.id ?: cage?.id
        val currentTask = tasks.first { it.id == taskId }
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
                if (item.targetId == targetId) {
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

    fun markChecklistItem(taskId: String, itemId: String, status: ChecklistStatus, reason: String = "", comment: String = "") = updateTask(taskId) { task ->
        task.copy(checklist = task.checklist.map { if (it.id == itemId) it.copy(status = status, result = it.result.copy(problemReason = reason, comment = comment)) else it }).markOffline()
    }

    fun completeChecklistItem(taskId: String, itemId: String, values: Map<String, String>) = updateTask(taskId) { task ->
        task.copy(
            status = TaskStatus.IN_PROGRESS,
            checklist = task.checklist.map { item ->
                if (item.id == itemId) {
                    item.copy(
                        status = ChecklistStatus.DONE,
                        result = item.result.copy(
                            values = item.result.values + values,
                            completedAt = "now",
                        ),
                    )
                } else {
                    item
                }
            },
            result = task.result.copy(values = task.result.values + values),
        ).markOffline()
    }

    fun completeTask(taskId: String) {
        val currentTask = tasks.first { it.id == taskId }
        val checklist = currentTask.checklist
        val pending = checklist.count { it.status == ChecklistStatus.PENDING }
        if (pending > 0) {
            lastMessage = "Нельзя завершить задачу: осталось $pending необработанных пунктов чек-листа"
            return
        }
        updateTask(taskId) { current ->
            val acceptance = if (current.requiresAcceptance) AcceptanceStatus.WAITING else AcceptanceStatus.NOT_REQUIRED
            current.copy(
                status = if (current.requiresAcceptance) TaskStatus.DONE else TaskStatus.SENT,
                acceptanceStatus = acceptance,
                checklist = checklist,
                result = current.result.copy(completedAt = "now")
            ).markOffline()
        }
    }

    fun skipTask(taskId: String, reason: String) = updateTask(taskId) { it.copy(status = TaskStatus.SKIPPED, result = it.result.copy(problemReason = reason, comment = reason)).markOffline() }

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
