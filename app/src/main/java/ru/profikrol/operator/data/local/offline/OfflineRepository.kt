package ru.profikrol.operator.data.local.offline

import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.ShiftState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
enum class OfflineActionType {
    START_PRODUCTION_TASK,
    START_WORK_TASK,
    COMPLETE_PRODUCTION_TARGET,
    PROBLEM_PRODUCTION_TARGET,
    COMPLETE_WORK_SUBTASK,
    COMPLETE_PRODUCTION_TASK,
    COMPLETE_WORK_TASK,
    ACCEPT_WORK_REPORT,
}

@Serializable
data class OfflineActionPayload(
    val itemId: String? = null,
    val reason: String? = null,
    val comment: String? = null,
    val values: Map<String, String> = emptyMap(),
    val generalSubtaskIds: List<Long> = emptyList(),
)

@Singleton
class OfflineRepository @Inject constructor(
    private val dao: OfflineDao,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun restoreTasks(employeeId: String): List<MobileTask> =
        dao.cachedTasks(employeeId)?.tasksJson?.let { runCatching { json.decodeFromString<List<MobileTask>>(it) }.getOrNull() }.orEmpty()

    suspend fun saveTasks(employeeId: String, tasks: List<MobileTask>) =
        dao.saveTasks(TaskCacheEntity(employeeId, json.encodeToString(tasks), System.currentTimeMillis()))

    suspend fun restoreShift(employeeId: String): ShiftState? =
        dao.cachedShift(employeeId)?.shiftJson?.let { runCatching { json.decodeFromString<ShiftState>(it) }.getOrNull() }

    suspend fun saveShift(employeeId: String, shift: ShiftState) =
        dao.saveShift(ShiftCacheEntity(employeeId, json.encodeToString(shift), System.currentTimeMillis()))

    suspend fun enqueue(employeeId: String, taskId: String, type: OfflineActionType, payload: OfflineActionPayload = OfflineActionPayload()) =
        dao.enqueue(OfflineActionEntity(employeeId = employeeId, taskId = taskId, type = type.name, payloadJson = json.encodeToString(payload)))

    suspend fun actions(employeeId: String) = dao.pendingActions(employeeId)
    suspend fun pendingCount(employeeId: String) = dao.pendingCount(employeeId)
    suspend fun remove(id: Long) = dao.deleteAction(id)
    suspend fun failed(id: Long, error: Throwable) = dao.recordFailure(id, error.message ?: error::class.java.simpleName)
    fun payload(action: OfflineActionEntity): OfflineActionPayload = json.decodeFromString(action.payloadJson)
}
