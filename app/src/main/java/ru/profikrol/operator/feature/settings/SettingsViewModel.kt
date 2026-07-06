package ru.profikrol.operator.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.profikrol.operator.domain.service.AuthService
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authService: AuthService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(user = authService.currentUser),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authService.user.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        // TODO: сохранить выбор в DataStore + сменить locale приложения.
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun onLogout() {
        authService.logout()
    }
}
