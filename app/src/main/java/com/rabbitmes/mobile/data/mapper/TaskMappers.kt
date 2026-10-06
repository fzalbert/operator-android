package com.rabbitmes.mobile.data.mapper

import com.rabbitmes.mobile.data.reference.OperationDefinitions
import com.rabbitmes.mobile.domain.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.profikrol.operator.data.remote.cell.localizeCellPositions
import ru.profikrol.operator.data.remote.production.ProductionTargetDto
import ru.profikrol.operator.data.remote.production.ProductionTaskDetailsDto
import ru.profikrol.operator.data.remote.profile.ShiftDto

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
            hangarId = hangarId,
            isOnline = previous.isOnline,
            pendingSyncEvents = previous.pendingSyncEvents,
        )
    }

internal fun ShiftState.isOpen(): Boolean = startedAt != null && finishedAt == null

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
    val fields = OperationDefinitions.of(operationType).fields.associateBy { it.id }
    if (operationType == OperationType.SLAUGHTER_SHIPPING) {
        val rawCount = result.values["animalCount"] ?: result.values["count"]
        rawCount?.toDoubleOrNull()?.toInt()?.let { put("animalCount", it) }
    } else {
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
    }
    if (comment.isNotBlank()) put("comment", comment)
}.toString()

internal fun ProductionTaskDetailsDto.toMobileTask(employeeId: String): MobileTask {
    val operationType = OperationType.fromCode(task.operationCode)
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
        // Задачу в работе без исполнителя отпустили при закрытии смены: её нужно снова взять
        // через «Приступить», тогда сервер сделает сотрудника исполнителем.
        status = task.executionStatus.orEmpty().toTaskStatus().let { status ->
            if (status == TaskStatus.IN_PROGRESS && task.assignedEmployeeId.isNullOrBlank()) TaskStatus.NEW else status
        },
        checklist = allExecutionItems().sortedBy { it.value.sortOrder }.map { executionItem ->
            val target = executionItem.value
            val targetType = when (target.targetType?.lowercase()) {
                "cage" -> TargetType.CAGE
                "hangar" -> TargetType.HANGAR
                "row" -> TargetType.ROW
                "rabbit" -> TargetType.RABBIT
                else -> OperationDefinitions.of(operationType).targetType
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
        operationTypeTitle = operationType.title,
        sortOrder = task.sortOrder,
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

internal fun String.toChecklistStatus(): ChecklistStatus = when (normalizedStatus()) {
    "DONE", "COMPLETED", "FINISHED", "ACCEPTED", "APPROVED" -> ChecklistStatus.DONE
    "PROBLEM", "FAILED", "BLOCKED", "ABORTED", "REJECTED" -> ChecklistStatus.PROBLEM
    "SKIPPED", "CANCELLED", "CANCELED" -> ChecklistStatus.SKIPPED
    else -> ChecklistStatus.PENDING
}

private fun String.normalizedStatus(): String = trim()
    .uppercase()
    .replace('-', '_')
    .replace(' ', '_')
