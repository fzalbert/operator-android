package com.rabbitmes.mobile

import kotlinx.serialization.Serializable

data class AppErrorMessage(
    val id: Long,
    val message: String,
)

/** Маршруты Navigation Compose. */
@Serializable
sealed interface AppRoute {
    @Serializable data object Shift : AppRoute
    @Serializable data object Tasks : AppRoute
    @Serializable data object Sync : AppRoute
    @Serializable data object Profile : AppRoute
    @Serializable data object Notifications : AppRoute
    @Serializable data object AcceptanceQueue : AppRoute
    @Serializable data class TaskExecution(val taskId: String) : AppRoute
    @Serializable data class RabbitProfile(val rfidCode: String, val taskId: String) : AppRoute
}
