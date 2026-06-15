package ru.profikrol.operator.uikit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.ElementSizes

/** Горизонтальный отступ между логотипом и текстовым блоком. */
private val LogoTitleSpacing = 12.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    title: String,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    subtitle: String? = null,
    hasUnreadNotifications: Boolean = false,
) {
    TopAppBar(
        modifier = Modifier.shadow(elevation = 8.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LogoTitleSpacing),
            ) {
                // Лого-квадрат
                IconBox(
                    icon = painterResource(R.drawable.ic_home_button),
                )

                // Title + subtitle
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
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
                        painter = painterResource(R.drawable.ic_notification),
                        contentDescription = stringResource(R.string.home_cd_notifications),
                        modifier = Modifier.size(ElementSizes.IconBoxIcon)
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.home_cd_settings),
                    modifier = Modifier.size(ElementSizes.IconBoxIcon)
                )
            }
        },
    )
}

// ---------- Previews ----------

@Preview(name = "MainTopBar — без бейджа", showBackground = true)
@Composable
private fun MainTopBarNoBadgePreview() {
    ProfikrolTheme {
        MainTopBar(
            title = "ООО «Профикроль»",
            subtitle = "Главный экран",
            onNotificationsClick = {},
            onSettingsClick = {},
            hasUnreadNotifications = false,
        )
    }
}

@Preview(name = "MainTopBar — с бейджем", showBackground = true)
@Composable
private fun MainTopBarWithBadgePreview() {
    ProfikrolTheme {
        MainTopBar(
            title = "ООО «Профикроль»",
            subtitle = "Главный экран",
            onNotificationsClick = {},
            onSettingsClick = {},
            hasUnreadNotifications = true,
        )
    }
}

@Preview(name = "MainTopBar — без subtitle", showBackground = true)
@Composable
private fun MainTopBarNoSubtitlePreview() {
    ProfikrolTheme {
        MainTopBar(
            title = "ООО «Профикроль»",
            onNotificationsClick = {},
            onSettingsClick = {},
            hasUnreadNotifications = true,
        )
    }
}

@Preview(
    name = "MainTopBar — тёмная тема",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MainTopBarDarkPreview() {
    ProfikrolTheme(darkTheme = true) {
        MainTopBar(
            title = "ООО «Профикроль»",
            subtitle = "Главный экран",
            onNotificationsClick = {},
            onSettingsClick = {},
            hasUnreadNotifications = true,
        )
    }
}
