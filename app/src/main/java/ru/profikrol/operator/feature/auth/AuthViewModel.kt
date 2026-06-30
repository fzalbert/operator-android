package ru.profikrol.operator.feature.auth

import android.util.Log
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
import ru.profikrol.operator.domain.repository.AuthError
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
        if (!state.canSubmit) {
            Log.d(TAG, "Submit ignored: canSubmit=false")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Submit started: login=${state.login}")
            _uiState.update { it.copy(isLoading = true, errorText = null) }

            authRepository.login(state.login, state.password)
                .onSuccess {
                    Log.d(TAG, "Submit succeeded: login=${state.login}")
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.LoggedIn)
                }
                .onFailure { throwable ->
                    Log.w(TAG, "Submit failed: login=${state.login}, error=${throwable.message}")
                    val errorRes = when (throwable) {
                        AuthError.InvalidCredentials -> "Неверный логин или пароль"
                        AuthError.Network -> "Нет связи с сервером"
                        else -> "Что-то пошло не так"
                    }
                    _uiState.update { it.copy(isLoading = false, errorText = errorRes) }
                }
        }
    }

    private companion object {
        const val TAG = "AuthViewModel"
    }
}

sealed interface AuthEvent {
    data object LoggedIn : AuthEvent
}
