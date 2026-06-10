package ru.profikrol.operator.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R

private val VibrantBlue = Color(0xFF2B7FFF)

@Composable
fun NotificationItem(
    notification: NotificationUi,
    onClick: () -> Unit,
) {

    val background = if (notification.isUnread) {
        VibrantBlue.copy(alpha = 0.08f)
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

        Column(
            modifier = Modifier.weight(1f),
        ) {

            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (notification.isUnread) {
                    VibrantBlue
                } else {
                    Color.Unspecified
                },
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = notification.time,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }

        if (notification.isUnread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = VibrantBlue,
                        shape = CircleShape,
                    ),
            )
        }
    }

    HorizontalDivider(
        color = Color(0xFFEAEAEA),
    )
}

@Composable
private fun NotificationIcon(
    type: NotificationType,
) {

    val backgroundColor = when (type) {
        NotificationType.CRITICAL -> Color(0xFFFFEBEE)
        NotificationType.SUCCESS -> Color(0xFFE8F5E9)
        NotificationType.DEFAULT -> Color(0xFFF5F5F5)
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_notification),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )
    }
}