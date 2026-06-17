package ru.profikrol.operator.feature.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ru.profikrol.operator.R
import ru.profikrol.operator.feature.settings.AppLanguage
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Spacing

/**
 * Секция «Язык интерфейса» — выбор одного языка из списка.
 * Stateless: текущий выбор и колбэк приходят снаружи.
 */
@Composable
fun LanguageSection(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_language),
        iconRes = R.drawable.ic_language,
        modifier = modifier,
    ) {
        AppLanguage.entries.forEachIndexed { index, language ->
            SettingsListItem(onClick = { onSelect(language) }) {
                Text(
                    text = stringResource(language.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                RadioButton(
                    selected = language == selected,
                    onClick = null,    // клик ловим на всей строке через SettingsListItem
                )
            }
            if (index < AppLanguage.entries.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

// ---------- Previews ----------

@Preview(name = "LanguageSection — RU", showBackground = true)
@Composable
private fun LanguageSectionRuPreview() {
    ProfikrolTheme {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            LanguageSection(
                selected = AppLanguage.Russian,
                onSelect = {},
            )
        }
    }
}

@Preview(name = "LanguageSection — UZ", showBackground = true)
@Composable
private fun LanguageSectionUzPreview() {
    ProfikrolTheme {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            LanguageSection(
                selected = AppLanguage.Uzbek,
                onSelect = {},
            )
        }
    }
}
