package ru.profikrol.operator.feature.notifications

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor() : ViewModel() {

    val notifications = mutableStateListOf(
        NotificationUi(
            id = 1,
            title = "Критическая задача",
            description = "Требуется пальпация 24 особей",
            time = "5 мин назад",
            isUnread = true,
            type = NotificationType.CRITICAL,
        ),
        NotificationUi(
            id = 2,
            title = "Завершена задача",
            description = "Осеменение в Ангаре 2 завершено",
            time = "1 час назад",
            isUnread = true,
            type = NotificationType.SUCCESS,
        ),
        NotificationUi(
            id = 3,
            title = "Новое уведомление",
            description = "Добавлены 12 новых кроликов в базу данных",
            time = "2 часа назад",
            isUnread = false,
            type = NotificationType.SUCCESS,
        ),
        NotificationUi(
            id = 4,
            title = "Напоминание",
            description = "Плановое взвешивание в 15:00",
            time = "3 часа назад",
            isUnread = false,
            type = NotificationType.SUCCESS,
        ),
        NotificationUi(
            id = 5,
            title = "Обновление системы",
            description = "Доступна новая версия приложения",
            time = "5 часов назад",
            isUnread = false,
            type = NotificationType.DEFAULT,
        ),
    )

    fun markAsRead(id: Long) {
        val index = notifications.indexOfFirst { it.id == id }

        if (index != -1) {
            notifications[index] = notifications[index].copy(
                isUnread = false,
            )
        }
    }

    fun markAllAsRead() {
        notifications.replaceAll {
            it.copy(isUnread = false)
        }
    }
}