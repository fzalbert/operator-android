package com.rabbitmes.mobile.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rabbitmes.mobile.AppRoute
import com.rabbitmes.mobile.core.ActionRunner
import com.rabbitmes.mobile.core.UserMessages
import com.rabbitmes.mobile.data.mapper.allTargets
import com.rabbitmes.mobile.data.mapper.mortalityRoundKindTitle
import com.rabbitmes.mobile.data.mapper.mortalityRoundTargetLabel
import com.rabbitmes.mobile.data.mapper.productionIdValue
import com.rabbitmes.mobile.data.mapper.productionResultJson
import com.rabbitmes.mobile.data.reference.FarmReferenceRepository
import com.rabbitmes.mobile.data.reference.OperationCatalog
import com.rabbitmes.mobile.data.task.ProductionTaskRemote
import com.rabbitmes.mobile.data.task.ProductionTaskRepository
import com.rabbitmes.mobile.data.task.ProductionTaskRepository.Companion.SERVER_TYPE_CHECKLIST
import com.rabbitmes.mobile.data.task.ProductionTaskRepository.Companion.SERVER_TYPE_TARGET
import com.rabbitmes.mobile.domain.AttachmentType
import com.rabbitmes.mobile.domain.ChecklistItem
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.domain.ExecutionResult
import com.rabbitmes.mobile.domain.MediaAttachment
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.OperationDefinition
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.PROBLEM_COMMENT_KEY
import com.rabbitmes.mobile.domain.PROBLEM_REASON_KEY
import com.rabbitmes.mobile.domain.TargetType
import com.rabbitmes.mobile.domain.TaskStatus
import com.rabbitmes.mobile.domain.matchesRfid
import com.rabbitmes.mobile.domain.operations.TargetResult
import com.rabbitmes.mobile.domain.operations.TargetResultContext
import com.rabbitmes.mobile.domain.operations.TargetResults
import com.rabbitmes.mobile.session.ShiftRepository
import com.rabbitmes.mobile.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.profikrol.operator.data.local.offline.OfflineActionPayload
import ru.profikrol.operator.data.local.offline.OfflineActionType
import ru.profikrol.operator.data.remote.production.AddProductionTargetRequest
import ru.profikrol.operator.domain.nfc.NfcReader
import javax.inject.Inject

/**
 * Выполнение одной production-задачи: старт, сканирование RFID, отметка целей
 * и пунктов чек-листа, события обхода и завершение.
 *
 * Изменения сразу применяются к задаче локально и уходят на сервер через [SyncManager].
 */
@HiltViewModel
class TaskExecutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: ProductionTaskRepository,
    private val remote: ProductionTaskRemote,
    private val references: FarmReferenceRepository,
    private val catalog: OperationCatalog,
    private val shiftRepository: ShiftRepository,
    private val syncManager: SyncManager,
    private val nfcReader: NfcReader,
    private val messages: UserMessages,
    private val actionRunner: ActionRunner,
) : ViewModel() {
    val taskId: String = savedStateHandle.toRoute<AppRoute.TaskExecution>().taskId
    val task: StateFlow<MobileTask?> = taskRepository.tasks
        .map { tasks -> tasks.firstOrNull { it.id == taskId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, taskRepository.task(taskId))

    /** Отсканированный RFID — временный ввод формы, он не восстанавливается из результатов задачи. */
    private val _scannedRfid = MutableStateFlow<String?>(null)
    val scannedRfid: StateFlow<String?> = _scannedRfid.asStateFlow()
    private val _scannedValues = MutableStateFlow<Map<String, String>>(emptyMap())
    val scannedValues: StateFlow<Map<String, String>> = _scannedValues.asStateFlow()
    private var isScanning = false

    private val isOnline: Boolean get() = shiftRepository.isOnline

    fun definition(type: OperationType): OperationDefinition = catalog.definition(type)

    fun rabbitIdForRfid(rfid: String): String? = references.rabbitIdForRfid(rfid)

    /** Редактировать можно только текущую задачу из очереди, и только пока она не закрыта. */
    fun canEdit(task: MobileTask): Boolean =
        taskRepository.canWorkOn(task.id) &&
            task.status != TaskStatus.DONE &&
            task.status != TaskStatus.SENT &&
            task.status != TaskStatus.SKIPPED

    fun begin() {
        if (!taskRepository.canWorkOn(taskId)) {
            messages.show("Сначала завершите предыдущую задачу")
            return
        }
        taskRepository.update(taskId) { it.copy(status = TaskStatus.IN_PROGRESS) }
        syncManager.enqueue(taskId, OfflineActionType.START_PRODUCTION_TASK)
        messages.show(if (isOnline) "Задача начата" else "Задача начата офлайн")
    }

    fun updateValue(key: String, value: String) =
        taskRepository.update(taskId) { it.copy(result = it.result.copy(values = it.result.values + (key to value))) }

    fun addPhoto(label: String, localUri: String) = addAttachment(AttachmentType.PHOTO, label, localUri)

    fun addVideo(label: String, localUri: String) = addAttachment(AttachmentType.VIDEO, label, localUri)

    fun addFile(label: String, localUri: String) = addAttachment(AttachmentType.FILE, label, localUri)

    fun addComment(comment: String) =
        taskRepository.update(taskId) { it.copy(result = it.result.copy(comment = comment)) }

    /**
     * Задачу нельзя выполнить: на сервере по всем открытым целям ставится проблема,
     * и задача завершается.
     */
    fun reject(reason: String, comment: String? = null) {
        val task = task.value ?: return
        val rejectionComment = comment ?: task.result.comment
        syncManager.enqueue(
            taskId,
            OfflineActionType.CANCEL_PRODUCTION_TASK,
            OfflineActionPayload(reason = reason, comment = rejectionComment.ifBlank { null }),
        )
        taskRepository.update(taskId) {
            it.copy(
                status = TaskStatus.DONE,
                result = it.result.copy(problemReason = reason, comment = rejectionComment, completedAt = "now"),
            )
        }
        messages.show(if (isOnline) "Задача завершена с проблемой" else "Проблема сохранена офлайн")
    }

    fun startRfidScan(values: Map<String, String>) {
        values.forEach { (key, value) -> updateValue(key, value) }
        if (!nfcReader.isAvailable) {
            messages.showError("NFC недоступен. Включите NFC на устройстве и попробуйте снова")
            return
        }
        isScanning = true
        messages.show("Поднесите RFID-метку к устройству")
        nfcReader.start { payload ->
            viewModelScope.launch {
                if (!isScanning) return@launch
                val rfid = payload.value.trim()
                if (rfid.isBlank()) return@launch
                _scannedRfid.value = rfid
                _scannedValues.value = values
                messages.show("RFID считан")
                stopRfidScan()
            }
        }
    }

    fun stopRfidScan() {
        if (!isScanning) return
        isScanning = false
        nfcReader.stop()
    }

    /** RFID отсканирован или введён на форме: закрываем подходящую цель задачи. */
    fun submitScan(rfid: String, values: Map<String, String>) {
        scanRfidAndCompleteItem(rfid, values)
        _scannedRfid.value = null
        _scannedValues.value = emptyMap()
    }

    fun markChecklistItem(itemId: String, status: ChecklistStatus, reason: String = "", comment: String = "") {
        if (status == ChecklistStatus.DONE || status == ChecklistStatus.PROBLEM) {
            completeItem(itemId, status, reason, comment)
        } else {
            updateItemLocally(itemId, status, reason, comment)
        }
    }

    fun completeChecklistItem(itemId: String, values: Map<String, String>) =
        completeItem(itemId, ChecklistStatus.DONE, values = values)

    fun complete(commentOverride: String? = null) {
        val task = task.value ?: return
        val pending = task.checklist.count { it.status == ChecklistStatus.PENDING }
        if (pending > 0 && task.operationType != OperationType.NEST_SELECTION) {
            messages.show("Нельзя завершить задачу: осталось $pending необработанных пунктов чек-листа")
            return
        }
        val comment = commentOverride ?: task.result.comment
        val submitsStandaloneResult = task.checklist.isEmpty() && task.operationType != OperationType.MORTALITY_ROUND
        if (submitsStandaloneResult) {
            syncManager.enqueue(
                taskId,
                OfflineActionType.SUBMIT_PRODUCTION_RESULT,
                OfflineActionPayload(values = mapOf(SyncManager.RESULT_JSON_KEY to task.productionResultJson(comment))),
            )
        }
        syncManager.enqueue(taskId, OfflineActionType.COMPLETE_PRODUCTION_TASK)
        taskRepository.update(taskId) { it.copy(status = TaskStatus.DONE, result = it.result.copy(completedAt = "now")) }
        messages.show(if (isOnline) "Задача завершена" else "Задача завершена офлайн и добавлена в очередь")
    }

    /** Событие обхода создаёт на сервере новую цель, поэтому работает только онлайн. */
    fun addMortalityRoundProblem(
        targetKind: String,
        rowId: String,
        cageId: String,
        rabbitId: String,
        comment: String,
        count: Int?,
        aliveBorn: Int?,
        stillborn: Int?,
    ) {
        val kind = targetKind.trim()
        val row = rowId.trim()
        val cage = cageId.trim().productionIdValue()
        val rabbit = rabbitId.trim()
        val note = comment.trim()
        val targetType = when (kind) {
            "feed_check", "water_check" -> TargetType.ROW
            "nest_control", "mortality_count" -> TargetType.CAGE
            "female_culling" -> TargetType.RABBIT
            else -> TargetType.HANGAR
        }
        val targetLabel = mortalityRoundTargetLabel(kind, row, cage, rabbit, count)
        val serverComment = listOf(row, note).filter(String::isNotBlank).joinToString(". ")
        val values = buildMap {
            put("Тип", mortalityRoundKindTitle(kind))
            if (row.isNotBlank()) put("Ряд", row)
            if (cage.isNotBlank()) put("Клетка", cage)
            if (rabbit.isNotBlank()) put("Самка", rabbit)
            if (note.isNotBlank()) put("Комментарий", note)
            if (count != null) put("Количество", count.toString())
            if (aliveBorn != null) put("Живорождённые", aliveBorn.toString())
            if (stillborn != null) put("Мертворождённые", stillborn.toString())
            if (aliveBorn != null && stillborn != null) put("Всего родилось", (aliveBorn + stillborn).toString())
        }

        actionRunner.launchBlocking(viewModelScope, "Create mortality round target failed", "Не удалось сохранить событие обхода") {
            val createdTargetId = remote.addTarget(
                taskId,
                AddProductionTargetRequest(targetKind = kind, cageId = cage.toLongOrNull(), rabbitId = rabbit.toLongOrNull()),
            )
            val targets = remote.details(taskId).allTargets()
            val createdTarget = targets.firstOrNull { it.id == createdTargetId }
                ?: targets.lastOrNull { target ->
                    target.status.orEmpty().equals("pending", ignoreCase = true) &&
                        (
                            target.targetKind.equals(kind, ignoreCase = true) ||
                                target.displayCode.equals(mortalityRoundKindTitle(kind), ignoreCase = true) ||
                                target.displayCode.equals(targetLabel, ignoreCase = true)
                            )
                }
            val targetId = createdTarget?.id ?: createdTargetId
            Log.d(TAG, "Created mortality round target. taskId=$taskId targetId=$targetId kind=$kind")

            when (kind) {
                "nest_control" -> remote.completeTarget(
                    taskId,
                    targetId,
                    buildJsonObject {
                        put("aliveBorn", requireNotNull(aliveBorn))
                        put("stillborn", requireNotNull(stillborn))
                        put("bornTotal", aliveBorn + stillborn)
                    },
                )
                "mortality_count" -> remote.reportMortalityCount(taskId, targetId, requireNotNull(count))
                else -> remote.reportTargetProblem(taskId, targetId, serverComment)
            }

            val event = ChecklistItem(
                id = "mortality-event-$kind-$targetId-${System.currentTimeMillis()}",
                label = targetLabel,
                targetType = targetType,
                targetId = rabbit.ifBlank { cage.ifBlank { row.ifBlank { targetId } } },
                serverType = "mortality-round-event",
                status = ChecklistStatus.PROBLEM,
                result = ExecutionResult(values = values, completedAt = "now", problemReason = kind, comment = serverComment),
            )
            taskRepository.rememberMortalityRoundEvent(taskId, event)
            taskRepository.update(taskId) { current ->
                current.copy(
                    status = TaskStatus.IN_PROGRESS,
                    checklist = current.checklist.filter { item ->
                        item.serverType != SERVER_TYPE_TARGET || item.status == ChecklistStatus.PENDING
                    } + event,
                )
            }
            messages.show("Событие обхода сохранено")
        }
    }

    override fun onCleared() {
        stopRfidScan()
        super.onCleared()
    }

    private fun scanRfidAndCompleteItem(rfid: String, values: Map<String, String>) {
        val task = task.value ?: return
        val normalizedRfid = rfid.trim()
        if (normalizedRfid.isBlank()) {
            messages.show("RFID не может быть пустым")
            return
        }
        if (task.checklist.any { it.status != ChecklistStatus.PENDING && it.matchesRfid(normalizedRfid) }) {
            messages.show("RFID $normalizedRfid уже использован в этой задаче")
            return
        }
        val rabbitTarget = task.checklist.firstOrNull { item ->
            item.targetType == TargetType.RABBIT &&
                item.status == ChecklistStatus.PENDING &&
                item.matchesRfid(normalizedRfid, rabbitIdForRfid(normalizedRfid))
        }
        // Заселение самки: RFID новый, поэтому берём первую свободную цель.
        val settlementTarget = if (task.operationType == OperationType.FEMALE_DELIVERY) {
            task.checklist.firstOrNull { it.serverType == SERVER_TYPE_TARGET && it.status == ChecklistStatus.PENDING }
                ?: task.checklist.firstOrNull { it.status == ChecklistStatus.PENDING }
        } else {
            null
        }
        val targetId = settlementTarget?.targetId ?: rabbitTarget?.targetId
        val problemReason = values[PROBLEM_REASON_KEY].orEmpty()
        val problemComment = values[PROBLEM_COMMENT_KEY].orEmpty()
        val resultValues = values - PROBLEM_REASON_KEY - PROBLEM_COMMENT_KEY

        val scannedItem = rabbitTarget ?: targetId?.let { id -> task.checklist.firstOrNull { it.targetId == id } }
        val matchingItem = when {
            targetId == null && problemReason.isBlank() -> null
            problemReason.isNotBlank() -> scannedItem?.takeIf { it.status == ChecklistStatus.PENDING }
                ?: task.checklist.firstOrNull { it.status == ChecklistStatus.PENDING }
            else -> scannedItem ?: task.checklist.filter { it.status == ChecklistStatus.PENDING }.singleOrNull()
        }
        if (matchingItem == null) {
            Log.w(TAG, "No checklist item for scanned RFID; nothing is sent. taskId=$taskId")
            taskRepository.update(taskId) {
                it.copy(
                    status = TaskStatus.IN_PROGRESS,
                    result = it.result.copy(
                        scannedRfid = normalizedRfid,
                        values = it.result.values + resultValues + ("lastScan" to normalizedRfid),
                    ),
                )
            }
            messages.show("RFID сохранен: $normalizedRfid")
            return
        }
        if (matchingItem.status != ChecklistStatus.PENDING) {
            messages.show("Пункт чек-листа уже обработан: ${matchingItem.label}")
            return
        }
        val status = if (problemReason.isBlank()) ChecklistStatus.DONE else ChecklistStatus.PROBLEM
        if (matchingItem.serverType == SERVER_TYPE_TARGET || matchingItem.serverType == SERVER_TYPE_CHECKLIST) {
            completeItem(matchingItem.id, status, problemReason, problemComment, resultValues + ("rfid" to normalizedRfid))
            return
        }
        taskRepository.update(taskId) { current ->
            current.copy(
                status = TaskStatus.IN_PROGRESS,
                checklist = current.checklist.map { item ->
                    if (item.id == matchingItem.id) item.withResult(status, problemReason, problemComment, resultValues + ("rfid" to normalizedRfid)) else item
                },
                result = current.result.copy(
                    scannedRfid = normalizedRfid,
                    values = current.result.values + resultValues + ("lastScan" to normalizedRfid),
                ),
            )
        }
        messages.show(
            if (problemReason.isBlank()) {
                "Скан принят: $normalizedRfid. Пункт чек-листа закрыт автоматически."
            } else {
                "Замечание сохранено: $problemReason"
            },
        )
    }

    private fun completeItem(
        itemId: String,
        status: ChecklistStatus,
        reason: String = "",
        comment: String = "",
        values: Map<String, String> = emptyMap(),
    ) {
        _scannedRfid.value = null
        _scannedValues.value = emptyMap()
        val task = task.value ?: return
        val item = task.checklist.firstOrNull { it.id == itemId }
        when (item?.serverType) {
            SERVER_TYPE_CHECKLIST -> {
                updateItemLocally(itemId, ChecklistStatus.DONE, reason, comment, values)
                syncManager.enqueue(taskId, OfflineActionType.COMPLETE_PRODUCTION_CHECKLIST_ITEM, OfflineActionPayload(itemId = itemId))
                messages.show(if (isOnline) "Пункт выполнен" else "Пункт сохранён офлайн")
            }
            SERVER_TYPE_TARGET -> completeTarget(task, item, status, reason, comment, values)
            else -> updateItemLocally(itemId, status, reason, comment, values)
        }
    }

    private fun completeTarget(
        task: MobileTask,
        item: ChecklistItem,
        status: ChecklistStatus,
        reason: String,
        comment: String,
        values: Map<String, String>,
    ) {
        val result = when (val built = TargetResults.build(task.operationType, item, values, TargetResultContext(::cellIdFor))) {
            is TargetResult.Invalid -> {
                messages.show(built.message)
                return
            }
            is TargetResult.Ready -> built
        }
        val payload = if (status == ChecklistStatus.PROBLEM) {
            val problemComment = comment.ifBlank { reason.ifBlank { values["palpationResult"].orEmpty() } }.ifBlank { "Есть замечание" }
            OfflineActionPayload(itemId = item.id, reason = reason.ifBlank { null }, comment = problemComment, values = values)
        } else {
            OfflineActionPayload(
                itemId = item.id,
                reason = reason.ifBlank { null },
                comment = comment.ifBlank { null },
                values = values + (SyncManager.RESULT_JSON_KEY to result.json.toString()),
            )
        }
        val type = if (status == ChecklistStatus.PROBLEM) OfflineActionType.PROBLEM_PRODUCTION_TARGET else OfflineActionType.COMPLETE_PRODUCTION_TARGET

        taskRepository.rememberTargetResult(taskId, item.withResult(status, reason, comment, values))
        updateItemLocally(item.id, status, reason, comment, values)
        syncManager.enqueue(taskId, type, payload)

        // Заселение и переселение закрываются сами после последней цели.
        val closesTask = task.operationType == OperationType.FEMALE_DELIVERY || task.operationType == OperationType.ANIMAL_TRANSFER
        val isLastTarget = task.checklist.count { it.status == ChecklistStatus.PENDING } == 1
        if (closesTask && isLastTarget) {
            syncManager.enqueue(taskId, OfflineActionType.COMPLETE_PRODUCTION_TASK)
            taskRepository.update(taskId) { it.copy(status = TaskStatus.DONE, result = it.result.copy(completedAt = "now")) }
        }
        messages.show(
            when {
                !isOnline -> "Результат сохранён офлайн"
                closesTask && isLastTarget ->
                    if (task.operationType == OperationType.ANIMAL_TRANSFER) "Переселение успешно завершено" else "Заселение успешно завершено"
                result.rfid != null -> "RFID сохранён: ${result.rfid}"
                else -> "Позиция выполнена"
            },
        )
    }

    /** id клетки по выбору оператора: подпись из списка, id или значение с префиксом. */
    private fun cellIdFor(selected: String): Long? =
        references.cells.firstOrNull { cell ->
            cell.displayName.equals(selected, ignoreCase = true) || cell.id.toString() == selected.productionIdValue()
        }?.id ?: selected.productionIdValue().toLongOrNull()

    private fun updateItemLocally(
        itemId: String,
        status: ChecklistStatus,
        reason: String = "",
        comment: String = "",
        values: Map<String, String> = emptyMap(),
    ) = taskRepository.update(taskId) { task ->
        task.copy(
            status = TaskStatus.IN_PROGRESS,
            checklist = task.checklist.map { item -> if (item.id == itemId) item.withResult(status, reason, comment, values) else item },
            result = task.result.copy(values = task.result.values + values),
        )
    }

    private fun ChecklistItem.withResult(
        status: ChecklistStatus,
        reason: String,
        comment: String,
        values: Map<String, String>,
    ) = copy(
        status = status,
        result = result.copy(
            values = result.values + values,
            scannedRfid = values["rfid"] ?: result.scannedRfid,
            completedAt = "now",
            problemReason = reason.ifBlank { null },
            comment = comment,
        ),
    )

    private fun addAttachment(type: AttachmentType, label: String, localUri: String) {
        val attachment = MediaAttachment(
            id = "media-${System.currentTimeMillis()}",
            type = type,
            name = label,
            localUri = localUri,
            createdAt = "now",
            uploaded = false,
        )
        taskRepository.update(taskId) { task ->
            task.copy(
                result = task.result.copy(
                    photos = if (type == AttachmentType.PHOTO) task.result.photos + label else task.result.photos,
                    videos = if (type == AttachmentType.VIDEO) task.result.videos + label else task.result.videos,
                    attachments = task.result.attachments + attachment,
                ),
            )
        }
    }

    private companion object {
        const val TAG = "TaskExecution"
    }
}
