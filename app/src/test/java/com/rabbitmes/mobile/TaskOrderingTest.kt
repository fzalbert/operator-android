package com.rabbitmes.mobile

import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.Priority
import com.rabbitmes.mobile.domain.TaskStatus
import com.rabbitmes.mobile.domain.orderedOpenTasks
import com.rabbitmes.mobile.domain.withSingleInProgressTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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

    @Test
    fun `overdue tasks go before today's regardless of sort order`() {
        val today = LocalDate.now()
        val ordered = listOf(
            task(id = "today-first", status = TaskStatus.NEW, sortOrder = 1, dueDate = today.toString()),
            task(id = "overdue-late", status = TaskStatus.NEW, sortOrder = 5, dueDate = today.minusDays(1).toString()),
            task(id = "overdue-oldest", status = TaskStatus.NEW, sortOrder = 9, dueDate = today.minusDays(3).toString()),
            task(id = "today-second", status = TaskStatus.NEW, sortOrder = 2, dueDate = today.toString()),
        ).orderedOpenTasks()

        assertEquals(listOf("overdue-oldest", "overdue-late", "today-first", "today-second"), ordered.map { it.id })
    }

    @Test
    fun `task in progress stays first even before overdue ones`() {
        val today = LocalDate.now()
        val ordered = listOf(
            task(id = "overdue", status = TaskStatus.NEW, sortOrder = 1, dueDate = today.minusDays(1).toString()),
            task(id = "today-in-progress", status = TaskStatus.IN_PROGRESS, sortOrder = 2, dueDate = today.toString()),
        ).orderedOpenTasks()

        assertEquals("today-in-progress", ordered.first().id)
    }

    @Test
    fun `overdue only when scheduled before today`() {
        val today = LocalDate.now()
        assertTrue(task("past", TaskStatus.NEW, 1, dueDate = today.minusDays(1).toString()).isOverdue)
        assertFalse(task("today", TaskStatus.NEW, 1, dueDate = today.toString()).isOverdue)
        assertFalse(task("broken", TaskStatus.NEW, 1, dueDate = "").isOverdue)
    }

    private fun task(id: String, status: TaskStatus, sortOrder: Int, dueDate: String = "2026-09-22") = MobileTask(
        id = id,
        title = id,
        operationType = OperationType.GENERAL,
        workshopId = "workshop",
        hangarId = "hangar",
        assignedEmployeeId = "employee",
        dueDate = dueDate,
        plannedStart = "10:00",
        plannedDurationMinutes = 15,
        priority = Priority.NORMAL,
        status = status,
        checklist = emptyList(),
        requiresAcceptance = false,
        sortOrder = sortOrder,
    )
}
