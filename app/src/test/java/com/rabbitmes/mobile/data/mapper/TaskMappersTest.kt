package com.rabbitmes.mobile.data.mapper

import com.rabbitmes.mobile.data.reference.OperationDefinitions
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.TargetType
import com.rabbitmes.mobile.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.profikrol.operator.data.remote.production.ProductionTargetDto
import ru.profikrol.operator.data.remote.production.ProductionTaskDetailsDto
import ru.profikrol.operator.data.remote.production.ProductionTaskDto

class TaskMappersTest {
    private fun details(
        operationCode: String? = null,
        title: String? = null,
        executionStatus: String? = null,
        assignedEmployeeId: String? = "emp",
        targets: List<ProductionTargetDto> = emptyList(),
        checklist: List<ProductionTargetDto> = emptyList(),
    ) = ProductionTaskDetailsDto(
        task = ProductionTaskDto(
            id = "task-guid",
            operationCode = operationCode,
            title = title,
            executionStatus = executionStatus,
            assignedEmployeeId = assignedEmployeeId,
        ),
        targets = targets,
        checklist = checklist,
    )

    @Test
    fun `resolves operation type only by backend code`() {
        assertEquals(OperationType.FEMALE_ARRIVAL, details(operationCode = "female_arrival").toMobileTask("emp").operationType)
        assertEquals(OperationType.NEST_EQUALIZATION, details(operationCode = "nest_equalization").toMobileTask("emp").operationType)
        // Заголовок на тип не влияет: «Переселение» с кодом осеменения остаётся осеменением.
        assertEquals(OperationType.INSEMINATION, details(operationCode = "insemination", title = "Переселение").toMobileTask("emp").operationType)
    }

    @Test
    fun `unknown, missing or misspelled code is a general task`() {
        assertEquals(OperationType.GENERAL, details(operationCode = "cleaning").toMobileTask("emp").operationType)
        assertEquals(OperationType.GENERAL, details(operationCode = "female-arrival").toMobileTask("emp").operationType)
        assertEquals(OperationType.GENERAL, details(title = "Переселение").toMobileTask("emp").operationType)
    }

    @Test
    fun `every app operation type has a form definition`() {
        OperationType.entries.forEach { type -> OperationDefinitions.of(type) }
    }

    @Test
    fun `maps server statuses`() {
        assertEquals(TaskStatus.IN_PROGRESS, details(executionStatus = "in-progress").toMobileTask("emp").status)
        assertEquals(TaskStatus.DONE, details(executionStatus = "COMPLETED").toMobileTask("emp").status)
        assertEquals(TaskStatus.SKIPPED, details(executionStatus = "canceled").toMobileTask("emp").status)
        assertEquals(TaskStatus.NEW, details(executionStatus = "something else").toMobileTask("emp").status)
    }

    @Test
    fun `task in progress without executor is shown as new to be taken again`() {
        assertEquals(TaskStatus.NEW, details(executionStatus = "in_progress", assignedEmployeeId = null).toMobileTask("emp").status)
        assertEquals(TaskStatus.IN_PROGRESS, details(executionStatus = "in_progress", assignedEmployeeId = "emp").toMobileTask("emp").status)
    }

    @Test
    fun `maps targets and checklist to execution items`() {
        val task = details(
            targets = listOf(
                ProductionTargetDto(id = "t1", targetType = "rabbit", rabbitId = "55", scanIdentifier = " E200 ", sortOrder = 2),
                ProductionTargetDto(id = "t2", targetType = "cage", cageId = 7, isCompleted = true, sortOrder = 1),
            ),
            checklist = listOf(ProductionTargetDto(id = "c1", title = "Проверить поилки", status = "DONE", sortOrder = 3)),
        ).toMobileTask("emp")

        assertEquals(listOf("t2", "t1", "c1"), task.checklist.map { it.id })
        assertEquals(listOf("production-target", "production-target", "production-checklist"), task.checklist.map { it.serverType })

        val cage = task.checklist[0]
        assertEquals(TargetType.CAGE, cage.targetType)
        assertEquals("7", cage.targetId)
        assertEquals(ChecklistStatus.DONE, cage.status)

        val rabbit = task.checklist[1]
        assertEquals(TargetType.RABBIT, rabbit.targetType)
        assertEquals("55", rabbit.targetId)
        assertEquals("E200", rabbit.scanIdentifier)
        assertEquals(ChecklistStatus.PENDING, rabbit.status)

        assertEquals("Проверить поилки", task.checklist[2].label)
        assertEquals(ChecklistStatus.DONE, task.checklist[2].status)
    }
}
