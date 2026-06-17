package ru.profikrol.operator.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Spacing

private val AvatarSize = 52.dp
private val AvatarIconSize = 24.dp

/**
 * Секция «Профиль» на экране настроек.
 * Слева — круглый аватар, справа — имя + должность. Под ними — email и телефон.
 */
@Composable
fun ProfileSection(
    name: String,
    role: String,
    email: String,
    phone: String,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_profile),
        iconRes = R.drawable.ic_profile,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(AvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(AvatarIconSize),
                    )
                }

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            ProfileField(
                label = stringResource(R.string.settings_email_label),
                value = email,
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.md),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            ProfileField(
                label = stringResource(R.string.settings_phone_label),
                value = phone,
            )
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(name = "ProfileSection — оператор", showBackground = true)
@Composable
private fun ProfileSectionOperatorPreview() {
    ProfikrolTheme {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            ProfileSection(
                name = "Иван Иванов",
                role = "Оператор фермы",
                email = "ivan.ivanov@profikrol.ru",
                phone = "+7 (916) 123-45-67",
            )
        }
    }
}

@Preview(name = "ProfileSection — технолог", showBackground = true)
@Composable
private fun ProfileSectionTechnologistPreview() {
    ProfikrolTheme {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            ProfileSection(
                name = "Мария Петрова",
                role = "Технолог",
                email = "maria.petrova@profikrol.ru",
                phone = "+7 (916) 555-66-77",
            )
        }
    }
}
