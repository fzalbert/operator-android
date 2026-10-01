package com.rabbitmes.mobile.data.mapper

import com.rabbitmes.mobile.domain.AcceptanceStatus
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.profikrol.operator.data.remote.worktask.WorkSubtaskDto
import ru.profikrol.operator.data.remote.worktask.WorkTaskDto

class TaskMappersTest {
    @Test
    fun `resolves operation type by alias`() {
        assertEquals(OperationType.ANIMAL_TRANSFER, WorkTaskDto(id = 1, operationName = "Переселение").resolveOperationType())
        assertEquals(OperationType.FEMALE_DELIVERY, WorkTaskDto(id = 1, operationId = "female-arrival").resolveOperationType())
    }

    @Test
    fun `unknown operation becomes custom task`() {
        assertEquals(OperationType.CUSTOM_TASK, WorkTaskDto(id = 1, name = "Починить дверь").resolveOperationType())
    }

    @Test
    fun `maps server statuses`() {
        assertEquals(TaskStatus.IN_PROGRESS, "in-progress".toTaskStatus())
        assertEquals(TaskStatus.DONE, "AWAITING_ACCEPTANCE".toTaskStatus())
        assertEquals(TaskStatus.SKIPPED, "canceled".toTaskStatus())
        assertEquals(TaskStatus.NEW, "something else".toTaskStatus())
    }

    @Test
    fun `maps work task with subtasks to checklist`() {
        val dto = WorkTaskDto(
            id = 42,
            name = "Переселение",
            status = "IN_PROGRESS",
            operationName = "Переселение",
            subtasks = listOf(
                WorkSubtaskDto(id = 7, name = "Клетка 1", status = "DONE"),
                WorkSubtaskDto(id = 8, name = "", status = "NEW"),
            ),
        )

        val task = dto.toMobileTask(employeeId = "emp")

        assertEquals("42", task.id)
        assertEquals(TaskStatus.IN_PROGRESS, task.status)
        assertEquals(listOf("7", "8"), task.checklist.map { it.id })
        assertEquals(listOf(ChecklistStatus.DONE, ChecklistStatus.PENDING), task.checklist.map { it.status })
        assertEquals("Подзадача 8", task.checklist[1].label)
        assertEquals(AcceptanceStatus.NOT_REQUIRED, task.acceptanceStatus)
    }

    @Test
    fun `task awaiting acceptance waits for review`() {
        val task = WorkTaskDto(id = 1, status = "AWAITING_ACCEPTANCE", requiresAcceptance = true).toMobileTask("emp")

        assertEquals(AcceptanceStatus.WAITING, task.acceptanceStatus)
    }
}
