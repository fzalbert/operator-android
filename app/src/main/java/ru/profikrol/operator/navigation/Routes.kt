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
    data object NestPreparation : Route

    @Serializable
    data object NestAlignment : Route

    @Serializable
    data object RfidInstallation : Route

    @Serializable
    data object RabbitCulling : Route
}

/**
 * Ключ savedStateHandle для возврата RFID-кода с экрана сканирования.
 */
const val RESULT_KEY_RFID_CODE = "rfid_code"
