package com.rabbitmes.mobile

import com.rabbitmes.mobile.domain.*

data class AppErrorMessage(
    val id: Long,
    val message: String,
)

sealed class AppScreen {
    data object Login : AppScreen()
    data object Shift : AppScreen()
    data object Tasks : AppScreen()
    data object Sync : AppScreen()
    data object Profile : AppScreen()
    data object Notifications : AppScreen()
    data object AcceptanceQueue : AppScreen()
    data class TaskExecution(val taskId: String) : AppScreen()
    data class RabbitProfile(val rfidCode: String, val taskId: String) : AppScreen()
}
