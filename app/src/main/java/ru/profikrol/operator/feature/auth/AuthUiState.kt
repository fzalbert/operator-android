package ru.profikrol.operator.feature.auth

data class AuthUiState(
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = login.isNotBlank() && password.isNotBlank() && !isLoading
}
