package ru.profikrol.operator.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.profikrol.operator.domain.service.AuthService
import javax.inject.Inject

@HiltViewModel
class AppNavViewModel @Inject constructor(
    private val authService: AuthService,
) : ViewModel() {

    val startDestination: Route = if (authService.isLoggedIn) {
        Route.Home
    } else {
        Route.Auth
    }

    val isLoggedIn: StateFlow<Boolean> = authService.user
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = authService.isLoggedIn,
        )

    init {
        if (authService.isLoggedIn) {
            refreshSavedSession()
        }
    }

    private fun refreshSavedSession() {
        viewModelScope.launch {
            authService.refreshSavedSession()
        }
    }
}
