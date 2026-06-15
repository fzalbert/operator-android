package ru.profikrol.operator.feature.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.theme.onPrimaryLight

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val notifications = viewModel.notifications
    val unreadCount = notifications.count { it.isUnread }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.notification),
                subtitle = stringResource(R.string.unread, unreadCount),
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = { viewModel.markAllAsRead() }
                    ) {
                        Text(
                            text = stringResource(R.string.read_all),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = onPrimaryLight,
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = notifications,
                    key = { it.id },
                ) { notification ->

                    NotificationItem(
                        notification = notification,
                        onClick = {
                            viewModel.markAsRead(notification.id)
                        },
                    )
                }
            }
        }
    }
}