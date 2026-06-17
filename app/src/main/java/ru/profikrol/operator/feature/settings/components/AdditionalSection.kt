package ru.profikrol.operator.feature.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Spacing

private val ArrowIconSize = 20.dp

/**
 * Секция «Дополнительно» — список действий со стрелкой справа.
 * Третий пункт «Выйти из аккаунта» подсвечивается цветом ошибки.
 *
 * TODO: подобрать постоянную иконку для заголовка секции (сейчас ic_note_logo).
 */
@Composable
fun AdditionalSection(
    onAboutClick: () -> Unit,
    onSupportClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_additional),
        iconRes = R.drawable.ic_note_logo,
        modifier = modifier,
    ) {
        AdditionalRow(
            label = stringResource(R.string.settings_about),
            onClick = onAboutClick,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        AdditionalRow(
            label = stringResource(R.string.settings_support),
            onClick = onSupportClick,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        AdditionalRow(
            label = stringResource(R.string.settings_logout),
            onClick = onLogoutClick,
            labelColor = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun AdditionalRow(
    label: String,
    onClick: () -> Unit,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    SettingsListItem(onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_next_arrow),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ArrowIconSize),
        )
    }
}

// ---------- Previews ----------

@Preview(name = "AdditionalSection", showBackground = true)
@Composable
private fun AdditionalSectionPreview() {
    ProfikrolTheme {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            AdditionalSection(
                onAboutClick = {},
                onSupportClick = {},
                onLogoutClick = {},
            )
        }
    }
}
