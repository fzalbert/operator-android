package ru.profikrol.operator.feature.home

data class HomeUiState(
    val isLoading: Boolean = false,
    val unreadNotificationsCount: Int = 0,
) {
    val hasUnreadNotifications: Boolean
        get() = unreadNotificationsCount > 0
}
