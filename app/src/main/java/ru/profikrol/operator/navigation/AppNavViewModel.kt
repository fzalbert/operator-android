package ru.profikrol.operator.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.repository.AuthError
import ru.profikrol.operator.domain.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AppNavViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val startDestination: Route = if (sessionStore.isLoggedIn) {
        Route.Home
    } else {
        Route.Auth
    }

    val isLoggedIn: StateFlow<Boolean> = sessionStore.user
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = sessionStore.isLoggedIn,
        )

    init {
        if (sessionStore.isLoggedIn) {
            refreshSavedSession()
        }
    }

    private fun refreshSavedSession() {
        viewModelScope.launch {
            authRepository.refreshSession()
                .onFailure { error ->
                    if (error == AuthError.InvalidCredentials) {
                        sessionStore.clear()
                    }
                }
        }
    }
}
