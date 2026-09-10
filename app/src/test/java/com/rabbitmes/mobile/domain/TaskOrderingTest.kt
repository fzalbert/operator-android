package com.rabbitmes.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskOrderingTest {
    @Test
    fun `in progress tasks come first while server order is preserved`() {
        val tasks = listOf(
            task("new-first", TaskStatus.NEW),
            task("active-first", TaskStatus.IN_PROGRESS),
            task("blocked", TaskStatus.BLOCKED),
            task("active-second", TaskStatus.IN_PROGRESS),
            task("done", TaskStatus.DONE),
            task("new-second", TaskStatus.NEW),
        )

        assertEquals(
            listOf("active-first", "active-second", "new-first", "blocked", "new-second"),
            tasks.orderedOpenTasks().map(MobileTask::id),
        )
    }

    @Test
    fun `locally active task wins when server returns several in progress tasks`() {
        val tasks = listOf(
            task("stale-active", TaskStatus.IN_PROGRESS),
            task("current-active", TaskStatus.IN_PROGRESS),
            task("next", TaskStatus.NEW),
        )

        assertEquals("current-active", tasks.nextExecutableTask("current-active")?.id)
    }

    @Test
    fun `missing active task falls back to first open task`() {
        val tasks = listOf(
            task("done", TaskStatus.DONE),
            task("next", TaskStatus.NEW),
        )

        assertEquals("next", tasks.nextExecutableTask("missing")?.id)
    }

    @Test
    fun `latest server start is used when local active task is unknown`() {
        val tasks = listOf(
            task("stale-active", TaskStatus.IN_PROGRESS, "2026-09-02T21:37:14Z"),
            task("current-active", TaskStatus.IN_PROGRESS, "2026-09-09T18:35:53Z"),
        )

        assertEquals("current-active", tasks.nextExecutableTask(null)?.id)
    }

    private fun task(id: String, status: TaskStatus, startedAt: String? = null) = MobileTask(
        id = id,
        title = id,
        operationType = OperationType.CUSTOM_TASK,
        workshopId = "workshop",
        hangarId = "hangar",
        assignedEmployeeId = "employee",
        dueDate = "2026-09-09",
        plannedStart = "09:00",
        plannedDurationMinutes = 5,
        priority = Priority.NORMAL,
        status = status,
        checklist = emptyList(),
        requiresAcceptance = false,
        startedAt = startedAt,
    )
}
