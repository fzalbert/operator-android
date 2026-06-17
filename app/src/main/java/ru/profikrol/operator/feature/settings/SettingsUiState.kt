package ru.profikrol.operator.feature.settings

import ru.profikrol.operator.domain.model.User

data class SettingsUiState(
    val user: User? = null,
    val selectedLanguage: AppLanguage = AppLanguage.Russian,
)
