package com.rabbitmes.mobile

import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.Priority
import com.rabbitmes.mobile.domain.TaskStatus
import com.rabbitmes.mobile.domain.withSingleInProgressTask
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskOrderingTest {
    @Test
    fun `only first ordered task remains in progress`() {
        val tasks = listOf(
            task(id = "second", status = TaskStatus.IN_PROGRESS, sortOrder = 2),
            task(id = "first", status = TaskStatus.IN_PROGRESS, sortOrder = 1),
            task(id = "third", status = TaskStatus.NEW, sortOrder = 3),
        ).withSingleInProgressTask()

        assertEquals(TaskStatus.NEW, tasks.first { it.id == "second" }.status)
        assertEquals(TaskStatus.IN_PROGRESS, tasks.first { it.id == "first" }.status)
        assertEquals(TaskStatus.NEW, tasks.first { it.id == "third" }.status)
    }

    private fun task(id: String, status: TaskStatus, sortOrder: Int) = MobileTask(
        id = id,
        title = id,
        operationType = OperationType.CUSTOM_TASK,
        workshopId = "workshop",
        hangarId = "hangar",
        assignedEmployeeId = "employee",
        dueDate = "2026-09-22",
        plannedStart = "10:00",
        plannedDurationMinutes = 15,
        priority = Priority.NORMAL,
        status = status,
        checklist = emptyList(),
        requiresAcceptance = false,
        sortOrder = sortOrder,
    )
}
