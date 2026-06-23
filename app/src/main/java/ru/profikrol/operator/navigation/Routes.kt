package ru.profikrol.operator.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe routes for Navigation Compose.
 */
sealed interface Route {

    @Serializable
    data object Auth : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Notifications : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object RfidScan : Route

    @Serializable
    data class RfidScanResult(val code: String) : Route

    @Serializable
    data class RabbitProfile(val code: String) : Route

    @Serializable
    data class Weighing(val code: String) : Route

    @Serializable
    data class Moving(val code: String) : Route

    @Serializable
    data object NestPreparation : Route

    @Serializable
    data object NestAlignment : Route

    @Serializable
    data object RfidInstallation : Route

    @Serializable
    data object RabbitCulling : Route
}
