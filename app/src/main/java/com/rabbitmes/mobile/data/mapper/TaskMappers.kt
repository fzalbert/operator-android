package com.rabbitmes.mobile.data.mapper

import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.profikrol.operator.data.remote.cell.CellDto
import ru.profikrol.operator.data.remote.cell.localizeCellPositions
import ru.profikrol.operator.data.remote.production.ProductionTargetDto
import ru.profikrol.operator.data.remote.production.ProductionTaskDetailsDto
import ru.profikrol.operator.data.remote.profile.ShiftDto
import ru.profikrol.operator.data.remote.rabbit.RabbitDto
import ru.profikrol.operator.data.remote.worktask.WorkTaskDto

internal fun ShiftDto?.toShiftState(employeeId: String, previous: ShiftState): ShiftState =
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

internal fun ShiftState.isOpen(): Boolean = startedAt != null && finishedAt == null

internal fun WorkTaskDto.toMobileTask(
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

internal fun WorkTaskDto.resolveOperationType(): OperationType {
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
    "weighing cage" to OperationType.WEIGHING,
    "light biostimulation" to OperationType.LIGHT_STIMULATION,
    "deworming dosatron" to OperationType.DEWORMING_DOSATRON,
    "mortality round" to OperationType.MORTALITY_ROUND,
    "обход ангара" to OperationType.MORTALITY_ROUND,
    "обход ангара с подсчетом падежа" to OperationType.MORTALITY_ROUND,
    "обход ангара и подсчет падежа" to OperationType.MORTALITY_ROUND,
    "обход ангара подсчет падежа" to OperationType.MORTALITY_ROUND,
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

internal fun mortalityRoundTargetLabel(
    targetKind: String,
    rowId: String = "",
    cageId: String = "",
    rabbitId: String = "",
    count: Int? = null,
): String = when (targetKind) {
    "light_check" -> "Свет"
    "feed_check" -> "Корм · ряд ${rowId.ifBlank { "—" }}"
    "water_check" -> "Вода · ряд ${rowId.ifBlank { "—" }}"
    "nest_control" -> "Гнездо / клетка ${cageId.ifBlank { "—" }}"
    "mortality_count" -> "Погибшие животные: ${count ?: 0} / клетка ${cageId.ifBlank { "—" }}"
    "female_culling" -> "Выбраковка самки ${rabbitId.ifBlank { "—" }}"
    else -> targetKind
}

internal fun mortalityRoundKindTitle(targetKind: String): String = when (targetKind) {
    "light_check" -> "Свет"
    "feed_check" -> "Корм"
    "water_check" -> "Вода"
    "nest_control" -> "Гнездо"
    "mortality_count" -> "Погибшие животные"
    "female_culling" -> "Выбраковка самки"
    else -> targetKind
}

private data class ProductionExecutionItem(
    val value: ProductionTargetDto,
    val serverType: String,
)

private fun ProductionTaskDetailsDto.allExecutionItems(): List<ProductionExecutionItem> =
    (targets + task.targets).map { ProductionExecutionItem(it, "production-target") }
        .plus((checklist + task.checkList).map { ProductionExecutionItem(it, "production-checklist") })
        .distinctBy { it.value.id }

internal fun ProductionTaskDetailsDto.allTargets(): List<ProductionTargetDto> =
    allExecutionItems().map { it.value }

private fun ProductionTargetDto.toDisplayLabel(targetType: TargetType): String {
    title?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    val code = displayCode?.trim().orEmpty().localizeCellPositions()
    if (targetType == TargetType.CAGE) {
        val cage = cageId?.toString() ?: targetId?.trim().orEmpty()
        return when {
            code.isNotBlank() && !code.all(Char::isDigit) -> code
            cage.isNotBlank() -> "Клетка $cage"
            code.isNotBlank() -> "Клетка $code"
            else -> "Клетка ${id.take(8)}"
        }
    }
    return code.ifBlank {
        targetKind?.let(::mortalityRoundKindTitle)
            ?: targetId
            ?: cageId?.let { "Клетка $it" }
            ?: "Позиция ${id.take(8)}"
    }
}

internal fun String.productionIdValue(): String {
    val trimmed = trim()
    if (trimmed.startsWith("ID ", ignoreCase = true)) {
        return trimmed.removePrefix("ID ").substringBefore(" ").trim()
    }
    return trimmed
}

internal fun MobileTask.productionResultJson(comment: String = result.comment): String = buildJsonObject {
    val fields = MockRepository.operation(operationType).fields.associateBy { it.id }
    result.values.forEach { (key, value) ->
        if (value.isBlank()) return@forEach
        when (fields[key]?.type) {
            FieldType.BOOLEAN -> put(key, value.toBooleanStrictOrNull() ?: false)
            FieldType.NUMBER, FieldType.TEMPERATURE, FieldType.HOURS -> {
                value.toLongOrNull()?.let { put(key, it) }
                    ?: value.toDoubleOrNull()?.let { put(key, it) }
                    ?: put(key, value)
            }
            FieldType.PHOTO, FieldType.VIDEO, FieldType.FILE -> Unit
            else -> put(key, value)
        }
    }
    if (comment.isNotBlank()) put("comment", comment)
}.toString()

internal fun ProductionTaskDetailsDto.toMobileTask(employeeId: String): MobileTask {
    val operationCandidates = listOfNotNull(task.operationCode, task.title, task.description)
    val operationKeys = operationCandidates.map { it.operationLookupKey() }
    val operationType = operationKeys.firstNotNullOfOrNull(OPERATION_ALIASES::get)
        ?: operationKeys.firstNotNullOfOrNull { operationKey ->
            OperationType.entries.firstOrNull { type ->
                type.name.operationLookupKey() == operationKey ||
                    type.title.operationLookupKey() == operationKey
            }
        }
        ?: OperationType.CUSTOM_TASK
    return MobileTask(
        id = task.id,
        title = task.title.orEmpty().ifBlank { operationType.title },
        operationType = operationType,
        workshopId = task.workshopId.toString(),
        hangarId = task.hangarId?.toString().orEmpty(),
        assignedEmployeeId = task.assignedEmployeeId.orEmpty(),
        dueDate = task.scheduledDate,
        plannedStart = "—",
        plannedDurationMinutes = task.durationMinutes ?: 0,
        priority = Priority.NORMAL,
        status = task.executionStatus.orEmpty().toTaskStatus(),
        checklist = allExecutionItems().sortedBy { it.value.sortOrder }.map { executionItem ->
            val target = executionItem.value
            val targetType = when (target.targetType?.lowercase()) {
                "cage" -> TargetType.CAGE
                "hangar" -> TargetType.HANGAR
                "row" -> TargetType.ROW
                "rabbit" -> TargetType.RABBIT
                else -> MockRepository.operation(operationType).targetType
            }
            ChecklistItem(
                id = target.id,
                label = target.toDisplayLabel(targetType),
                targetType = targetType,
                targetId = when (targetType) {
                    TargetType.RABBIT -> target.targetId ?: target.rabbitId ?: target.scanIdentifier ?: target.id
                    TargetType.CAGE -> target.targetId ?: target.cageId?.toString() ?: target.id
                    TargetType.HANGAR -> target.targetId ?: target.hangarId?.toString() ?: target.id
                    TargetType.ROW -> target.targetId ?: target.id
                },
                rabbitId = target.rabbitId,
                cageId = target.cageId?.toString(),
                scanIdentifier = target.scanIdentifier?.trim()?.takeIf { it.isNotBlank() }
                    ?: target.displayCode?.trim()?.takeIf { targetType == TargetType.RABBIT && it.isNotBlank() },
                serverType = executionItem.serverType,
                status = if (target.isCompleted == true) ChecklistStatus.DONE else target.status.orEmpty().toChecklistStatus(),
                result = ExecutionResult(scannedRfid = target.scanIdentifier, completedAt = target.completedAt),
            )
        },
        requiresAcceptance = task.requiresAcceptance,
        description = task.description.orEmpty(),
        operationTypeTitle = task.title.orEmpty().ifBlank { operationType.title },
        isGeneral = false,
        sortOrder = task.sortOrder,
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

internal fun String.toTaskStatus(): TaskStatus = when (normalizedStatus()) {
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

internal fun String.normalizedStatus(): String = trim()
    .uppercase()
    .replace('-', '_')
    .replace(' ', '_')

internal fun String?.toDisplayTime(): String = this
    ?.substringAfter('T', "")
    ?.take(5)
    ?.takeIf(String::isNotBlank)
    ?: "—"
