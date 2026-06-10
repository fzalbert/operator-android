package ru.profikrol.operator.uikit.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.profikrol.operator.R

/**
 * Тулбар главного экрана: title + действия справа (уведомления, настройки).
 * Без кнопки "назад". На иконке уведомлений рисуется badge-точка,
 * если hasUnreadNotifications = true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    title: String,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    hasUnreadNotifications: Boolean = false,
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                BadgedBox(
                    badge = {
                        if (hasUnreadNotifications) {
                            // Пустой Badge = маленькая красная точка.
                            Badge()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.home_cd_notifications),
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.home_cd_settings),
                )
            }
        },
    )
}
