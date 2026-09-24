package com.rabbitmes.mobile.domain

private fun TaskStatus.executionOrder(): Int = when (this) {
    TaskStatus.IN_PROGRESS -> 0
    TaskStatus.BLOCKED -> 1
    TaskStatus.NEW -> 2
    TaskStatus.DONE, TaskStatus.SENT, TaskStatus.SKIPPED -> 3
}

val mobileTaskExecutionComparator: Comparator<MobileTask> =
    compareBy<MobileTask> { it.status.executionOrder() }
        .thenBy { it.priority.weight }
        .thenBy { it.dueDate }
        .thenBy { it.plannedStart }
        .thenBy { it.id }

fun Iterable<MobileTask>.orderedOpenTasks(): List<MobileTask> =
    filter {
        it.status != TaskStatus.DONE &&
            it.status != TaskStatus.SENT &&
            it.status != TaskStatus.SKIPPED
    }.sortedWith(
        compareBy<MobileTask> { it.status.executionOrder() }
            .thenBy { it.sortOrder }
            .then(mobileTaskExecutionComparator),
    )

fun Iterable<MobileTask>.withSingleInProgressTask(): List<MobileTask> {
    val tasks = toList()
    val activeTaskId = tasks
        .orderedOpenTasks()
        .firstOrNull { it.status == TaskStatus.IN_PROGRESS }
        ?.id

    return tasks.map { task ->
        if (task.status == TaskStatus.IN_PROGRESS && task.id != activeTaskId) {
            task.copy(status = TaskStatus.NEW)
        } else {
            task
        }
    }
}
