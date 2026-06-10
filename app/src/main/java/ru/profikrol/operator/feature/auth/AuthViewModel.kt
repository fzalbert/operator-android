package ru.profikrol.operator.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.profikrol.operator.domain.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onLoginChange(value: String) {
        _uiState.update { it.copy(login = value, errorText = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorText = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorText = null) }

            authRepository.login(state.login, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.LoggedIn)
                }
                .onFailure { throwable ->
                    val errorRes = "Неверный логин или пароль"
                    _uiState.update { it.copy(isLoading = false, errorText = errorRes) }
                }
        }
    }
}

sealed interface AuthEvent {
    data object LoggedIn : AuthEvent
}
