package com.rabbitmes.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.domain.NotificationType
import com.rabbitmes.mobile.domain.NotificationUi
import com.rabbitmes.mobile.ui.components.AppHeader
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen

@Composable
fun NotificationsScreen(
    notifications: List<NotificationUi>,
    onBack: () -> Unit,
    onRead: (Long) -> Unit,
    onReadAll: () -> Unit,
) {
    val unreadCount = notifications.count { it.isUnread }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                AppHeader(
                    title = "Уведомления",
                    subtitle = "Непрочитанные: $unreadCount",
                    onBack = onBack,
                    trailing = {
                        TextButton(onClick = onReadAll, enabled = unreadCount > 0) {
                            Text("Прочитать все")
                        }
                    },
                )
            }

            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Уведомлений пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(
                items = notifications,
                key = { it.id },
            ) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { onRead(notification.id) },
                )
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationUi,
    onClick: () -> Unit,
) {
    val accent = notificationAccent(notification.type)
    val background = if (notification.isUnread) {
        accent.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        NotificationIcon(notification.type)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (notification.isUnread) accent else MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = notification.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (notification.isUnread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape),
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun NotificationIcon(type: NotificationType) {
    val accent = notificationAccent(type)
    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.12f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun notificationAccent(type: NotificationType): Color = when (type) {
    NotificationType.CRITICAL -> MaterialTheme.colorScheme.error
    NotificationType.SUCCESS -> mobileSuccessGreen
    NotificationType.DEFAULT -> MaterialTheme.colorScheme.primary
}
