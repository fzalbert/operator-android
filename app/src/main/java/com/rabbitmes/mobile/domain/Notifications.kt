package com.rabbitmes.mobile.domain

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
