package ru.profikrol.operator.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.profikrol.operator.uikit.components.AppTopBar

//TODO: вынести в strings.xml
//TODO: изменить название NotificationUi на NotificationModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
) {

    //TODO: вынести во ViewModel
    val notifications = remember {
        mutableStateListOf(
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
    }

    var selectedId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selectedId) {
        if (selectedId == null) return@LaunchedEffect

        delay(2000)

        selectedId = null
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Уведомления",
                onBack = onBack,
            )
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            NotificationsHeader(
                unreadCount = notifications.count { it.isUnread },
                onReadAllClick = {},
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = notifications,
                    key = { it.id },
                ) { notification ->

                    NotificationItem(
                        notification = notification,
                        isSelected = selectedId == notification.id,
                        onClick = {
                            selectedId = notification.id
                        },
                    )
                }
            }
        }
    }
}
@Composable
private fun NotificationsHeader(
    unreadCount: Int,
    onReadAllClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = "Непрочитанных: $unreadCount",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )

        TextButton(
            onClick = onReadAllClick,
        ) {
            Text("Прочитать всё")
        }
    }
}