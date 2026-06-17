package ru.profikrol.operator.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.profikrol.operator.R
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.feature.settings.components.AdditionalSection
import ru.profikrol.operator.feature.settings.components.LanguageSection
import ru.profikrol.operator.feature.settings.components.ProfileSection
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Spacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        user = state.user,
        selectedLanguage = state.selectedLanguage,
        onLanguageSelected = viewModel::onLanguageSelected,
        onAboutClick = { /* TODO: открыть About */ },
        onSupportClick = { /* TODO: открыть Support */ },
        onLogoutClick = {
            viewModel.onLogout()
            onLoggedOut()
        },
        onBack = onBack,
    )
}

@Composable
private fun SettingsContent(
    user: User?,
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onAboutClick: () -> Unit,
    onSupportClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBack: () -> Unit,
) {
    val placeholder = stringResource(R.string.settings_value_placeholder)
    val roleText = user?.role?.displayString() ?: placeholder
    val name = user?.displayName ?: placeholder
    val email = user?.email ?: placeholder
    val phone = user?.phone ?: placeholder

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
            )
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ProfileSection(
                name = name,
                role = roleText,
                email = email,
                phone = phone,
            )

            LanguageSection(
                selected = selectedLanguage,
                onSelect = onLanguageSelected,
            )

            AdditionalSection(
                onAboutClick = onAboutClick,
                onSupportClick = onSupportClick,
                onLogoutClick = onLogoutClick,
            )
        }
    }
}

@Composable
private fun UserRole.displayString(): String = when (this) {
    UserRole.Operator -> stringResource(R.string.role_operator)
    UserRole.Technologist -> stringResource(R.string.role_technologist)
}

// ---------- Previews ----------

@Preview(name = "SettingsScreen — оператор", showBackground = true)
@Composable
private fun SettingsContentOperatorPreview() {
    ProfikrolTheme {
        SettingsContent(
            user = User(
                id = "1",
                login = "ivan",
                displayName = "Иван Иванов",
                token = "",
                role = UserRole.Operator,
                email = "ivan.ivanov@profikrol.ru",
                phone = "+7 (916) 123-45-67",
            ),
            selectedLanguage = AppLanguage.Russian,
            onLanguageSelected = {},
            onAboutClick = {},
            onSupportClick = {},
            onLogoutClick = {},
            onBack = {},
        )
    }
}

@Preview(name = "SettingsScreen — без сессии", showBackground = true)
@Composable
private fun SettingsContentEmptyPreview() {
    ProfikrolTheme {
        SettingsContent(
            user = null,
            selectedLanguage = AppLanguage.English,
            onLanguageSelected = {},
            onAboutClick = {},
            onSupportClick = {},
            onLogoutClick = {},
            onBack = {},
        )
    }
}
