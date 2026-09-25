package com.rabbitmes.mobile

import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.ChecklistItem
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.domain.ExecutionResult
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.Priority
import com.rabbitmes.mobile.domain.TargetType
import ru.profikrol.operator.data.remote.cell.localizeCellPositions
import ru.profikrol.operator.data.remote.production.ProductionTargetDto
import ru.profikrol.operator.data.remote.production.ProductionTaskDetailsDto

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

internal fun ProductionTaskDetailsDto.toMobileTask(employeeId: String): MobileTask {
    val operationCandidates = listOfNotNull(task.operationCode, task.title, task.description)
    val operationKeys = operationCandidates.map { it.operationLookupKey() }
    val operationType = if (
        OPERATION_ALIASES[task.title.orEmpty().operationLookupKey()] == OperationType.ANIMAL_TRANSFER
    ) {
        OperationType.ANIMAL_TRANSFER
    } else operationKeys.firstNotNullOfOrNull(OPERATION_ALIASES::get)
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
