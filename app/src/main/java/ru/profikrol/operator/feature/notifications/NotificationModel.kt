package ru.profikrol.operator.feature.notifications

data class NotificationUi(
    val id: Long,
    val title: String,
    val description: String,
    val time: String,
    val isUnread: Boolean,
    val type: NotificationType,
)

enum class NotificationType {
    CRITICAL,
    SUCCESS,
    DEFAULT,
}