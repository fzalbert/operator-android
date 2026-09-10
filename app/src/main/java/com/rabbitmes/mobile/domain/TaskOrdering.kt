package com.rabbitmes.mobile.domain

fun Iterable<MobileTask>.orderedOpenTasks(): List<MobileTask> {
    val openTasks = filter {
        it.status != TaskStatus.DONE &&
            it.status != TaskStatus.SENT &&
            it.status != TaskStatus.SKIPPED
    }
    val inProgress = openTasks
        .filter { it.status == TaskStatus.IN_PROGRESS }
        .sortedByDescending { it.startedAt.orEmpty() }
    return inProgress + openTasks.filterNot { it.status == TaskStatus.IN_PROGRESS }
}

fun Iterable<MobileTask>.nextExecutableTask(activeTaskId: String?): MobileTask? {
    val openTasks = orderedOpenTasks()
    return activeTaskId
        ?.let { activeId -> openTasks.firstOrNull { it.id == activeId } }
        ?: openTasks.firstOrNull()
}
