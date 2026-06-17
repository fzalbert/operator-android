package ru.profikrol.operator.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.profikrol.operator.data.local.SessionStore
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(user = sessionStore.currentUser),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.user.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        // TODO: сохранить выбор в DataStore + сменить locale приложения.
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun onLogout() {
        sessionStore.clear()
    }
}
