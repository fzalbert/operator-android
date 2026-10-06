package com.rabbitmes.mobile.sync

import android.util.Log
import com.rabbitmes.mobile.core.UserMessages
import com.rabbitmes.mobile.core.runCatchingCancellable
import com.rabbitmes.mobile.core.toHttpDebugMessage
import com.rabbitmes.mobile.core.toUserMessage
import com.rabbitmes.mobile.data.mapper.allTargets
import com.rabbitmes.mobile.data.mapper.toChecklistStatus
import com.rabbitmes.mobile.data.task.ProductionTaskRemote
import com.rabbitmes.mobile.data.task.ProductionTaskRepository
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.domain.TaskStatus
import com.rabbitmes.mobile.session.EmployeeSession
import com.rabbitmes.mobile.session.ShiftRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
import ru.profikrol.operator.data.local.offline.OfflineActionPayload
import ru.profikrol.operator.data.local.offline.OfflineActionType
import ru.profikrol.operator.data.local.offline.OfflineRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единственный путь отправки изменений задач на сервер.
 *
 * Любое действие сначала сохраняется в офлайн-очередь, а затем отправляется
 * сразу, если есть сеть, или при её появлении. Ответ 409 («уже сделано»)
 * считается успехом, если сервер подтверждает нужное состояние.
 *
 * Если сервер отклонил действие окончательно (4xx), оно убирается из очереди,
 * пользователь видит ошибку, а задачи перечитываются с сервера, чтобы откатить
 * локальное состояние. При временной ошибке (нет сети, 5xx) действие остаётся в очереди.
 */
@Singleton
class SyncManager @Inject constructor(
    private val offlineRepository: OfflineRepository,
    private val remote: ProductionTaskRemote,
    private val taskRepository: ProductionTaskRepository,
    private val shiftRepository: ShiftRepository,
    private val employeeSession: EmployeeSession,
    private val messages: UserMessages,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var flushJob: Job? = null
    private var flushRequested = false
    private val enqueueLock = Mutex()

    fun enqueue(taskId: String, type: OfflineActionType, payload: OfflineActionPayload = OfflineActionPayload()) {
        val employeeId = employeeSession.id
        scope.launch {
            // Mutex честный: действия попадают в очередь в порядке вызова, например
            // цель задачи всегда раньше завершения самой задачи.
            enqueueLock.withLock { offlineRepository.enqueue(employeeId, taskId, type, payload) }
            updatePendingCount()
            if (shiftRepository.isOnline) flush()
        }
    }

    fun setOnline(isOnline: Boolean) {
        val wasOnline = shiftRepository.isOnline
        shiftRepository.setOnline(isOnline)
        val pending = shiftRepository.current.pendingSyncEvents
        if (wasOnline != isOnline) {
            messages.show(
                when {
                    !isOnline -> "Нет интернета: приложение перешло в офлайн режим"
                    pending > 0 -> "Интернет появился: синхронизируем изменения"
                    else -> "Онлайн режим включен"
                },
            )
        }
        if (isOnline && pending > 0) flush()
    }

    /** Ручная синхронизация с экрана очереди: сообщает результат, даже если отправлять нечего. */
    fun syncNow() {
        if (!shiftRepository.isOnline) {
            messages.show("Нет интернета: синхронизация будет доступна после перехода онлайн")
            return
        }
        flush(reportResult = true)
    }

    suspend fun restorePendingCount() {
        updatePendingCount()
        if (shiftRepository.isOnline && shiftRepository.current.pendingSyncEvents > 0) flush()
    }

    private fun flush(reportResult: Boolean = false) {
        if (flushJob?.isActive == true) {
            flushRequested = true
            return
        }
        flushJob = scope.launch {
            try {
                do {
                    flushRequested = false
                    sendQueued(reportResult)
                } while (flushRequested)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                messages.handle(error, "Не удалось синхронизировать изменения", "Offline synchronization failed")
            }
        }
    }

    private suspend fun sendQueued(reportResult: Boolean) {
        val queued = offlineRepository.actions(employeeSession.id)
        if (queued.isEmpty()) {
            shiftRepository.setPendingSyncEvents(0)
            taskRepository.clearOfflineMarks()
            if (reportResult) messages.show("Нет изменений для синхронизации")
            return
        }
        var sent = 0
        var rejected = false
        for (action in queued) {
            val type = OfflineActionType.entries.firstOrNull { it.name == action.type }
            if (type == null) {
                // Действие из старой версии приложения (например, work-задачи), которое
                // больше нельзя отправить. Убираем, чтобы оно не блокировало очередь.
                Log.w(TAG, "Dropping unsupported offline action. type=${action.type} taskId=${action.taskId}")
                offlineRepository.remove(action.id)
                continue
            }
            try {
                execute(action.taskId, type, offlineRepository.payload(action))
                offlineRepository.remove(action.id)
                sent++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (error.isPermanent()) {
                    Log.e(TAG, "Server rejected offline action ${action.type}: ${error.toHttpDebugMessage()}. taskId=${action.taskId}", error)
                    offlineRepository.remove(action.id)
                    rejected = true
                    messages.showError(error.toUserMessage(rejectionMessage(type)))
                    continue
                }
                Log.w(TAG, "Offline action ${action.type} postponed: ${error.toHttpDebugMessage()}", error)
                offlineRepository.failed(action.id, error)
                break
            }
        }
        val pending = updatePendingCount()
        if (pending == 0) {
            taskRepository.clearOfflineMarks()
            taskRepository.load(showLoading = false)
            if (reportResult && !rejected) messages.show("Все изменения синхронизированы")
        } else {
            if (rejected) taskRepository.load(showLoading = false)
            if (reportResult) messages.show("Отправлено: $sent. В очереди осталось: $pending")
        }
    }

    private suspend fun execute(taskId: String, type: OfflineActionType, payload: OfflineActionPayload) {
        when (type) {
            OfflineActionType.START_PRODUCTION_TASK -> remote.start(taskId)
            OfflineActionType.COMPLETE_PRODUCTION_CHECKLIST_ITEM -> idempotent(
                action = { remote.completeChecklistItem(taskId, requireNotNull(payload.itemId)) },
                alreadyDone = { itemProcessed(taskId, payload.itemId) },
            )
            OfflineActionType.COMPLETE_PRODUCTION_TARGET -> {
                val result = Json.parseToJsonElement(payload.values.getValue(RESULT_JSON_KEY)).jsonObject
                idempotent(
                    action = { remote.completeTarget(taskId, requireNotNull(payload.itemId), result, payload.values["rfid"]) },
                    alreadyDone = { itemProcessed(taskId, payload.itemId) },
                )
            }
            OfflineActionType.PROBLEM_PRODUCTION_TARGET -> idempotent(
                action = {
                    remote.reportTargetProblem(
                        taskId,
                        requireNotNull(payload.itemId),
                        payload.comment ?: payload.reason ?: "Есть замечание",
                    )
                },
                alreadyDone = { itemProcessed(taskId, payload.itemId) },
            )
            OfflineActionType.SUBMIT_PRODUCTION_RESULT -> remote.submitResult(taskId, payload.values.getValue(RESULT_JSON_KEY))
            OfflineActionType.CANCEL_PRODUCTION_TASK -> reportTaskProblem(taskId, payload.reason, payload.comment)
            OfflineActionType.COMPLETE_PRODUCTION_TASK -> idempotent(
                action = { remote.complete(taskId) },
                alreadyDone = { remote.task(taskId).status == TaskStatus.DONE },
            )
        }
    }

    /** Проблема по всем незакрытым целям задачи и завершение; без открытых целей — замечание к задаче. */
    private suspend fun reportTaskProblem(taskId: String, reason: String?, comment: String?) {
        val pendingTargets = remote.details(taskId).allTargets().filter { target ->
            target.isCompleted != true && target.status.orEmpty().toChecklistStatus() == ChecklistStatus.PENDING
        }
        val problemComment = listOfNotNull(reason, comment)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(". ")
            .ifBlank { "Невозможно выполнить задачу" }
        // Открытых целей нет: замечание пишется к самой задаче, и сервер сразу её закрывает.
        if (pendingTargets.isEmpty()) {
            idempotent(
                action = { remote.reportTaskProblem(taskId, problemComment) },
                alreadyDone = { remote.task(taskId).status == TaskStatus.DONE },
            )
            return
        }
        pendingTargets.forEach { target -> remote.reportTargetProblem(taskId, target.id, problemComment) }
        idempotent(
            action = { remote.complete(taskId) },
            alreadyDone = { remote.task(taskId).status == TaskStatus.DONE },
        )
    }

    private suspend fun itemProcessed(taskId: String, itemId: String?): Boolean {
        val item = remote.task(taskId).checklist.firstOrNull { it.id == itemId }
        return item != null && item.status != ChecklistStatus.PENDING
    }

    /** 409 — успех, если сервер подтверждает, что действие уже выполнено. */
    private suspend fun idempotent(action: suspend () -> Unit, alreadyDone: suspend () -> Boolean) {
        runCatchingCancellable { action() }.getOrElse { error ->
            if (error is HttpException && error.code() == 409 && alreadyDone()) return
            throw error
        }
    }

    private suspend fun updatePendingCount(): Int {
        val pending = offlineRepository.pendingCount(employeeSession.id)
        shiftRepository.setPendingSyncEvents(pending)
        shiftRepository.persist()
        return pending
    }

    private fun Throwable.isPermanent(): Boolean =
        this is HttpException && code() in 400..499 && code() != 401 && code() != 408 && code() != 429

    private fun rejectionMessage(type: OfflineActionType): String = when (type) {
        OfflineActionType.START_PRODUCTION_TASK -> "Не удалось начать задачу"
        OfflineActionType.COMPLETE_PRODUCTION_CHECKLIST_ITEM -> "Не удалось отметить пункт"
        OfflineActionType.COMPLETE_PRODUCTION_TARGET,
        OfflineActionType.PROBLEM_PRODUCTION_TARGET -> "Не удалось сохранить результат"
        OfflineActionType.CANCEL_PRODUCTION_TASK -> "Не удалось отметить проблему"
        OfflineActionType.SUBMIT_PRODUCTION_RESULT,
        OfflineActionType.COMPLETE_PRODUCTION_TASK -> "Не удалось завершить задачу"
    }

    companion object {
        const val RESULT_JSON_KEY = "_resultJson"
        private const val TAG = "RabbitSync"
    }
}
