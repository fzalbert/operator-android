package ru.profikrol.operator.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.profikrol.operator.core.role.RoleFeatureProvider
import ru.profikrol.operator.data.local.SessionStore
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionStore: SessionStore,
    private val roleFeatureProvider: RoleFeatureProvider,
) : ViewModel() {

    // TODO: заменить хардкод unreadNotificationsCount на NotificationsRepository.unreadCount: Flow<Int>
    private val _uiState = MutableStateFlow(
        HomeUiState(
            unreadNotificationsCount = 3,
            actions = currentActions(),
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Если сессия сменится (логаут → новый логин) — пересчитаем список действий.
        viewModelScope.launch {
            sessionStore.user.collect {
                _uiState.update { it.copy(actions = currentActions()) }
            }
        }
    }

    private fun currentActions(): List<HomeAction> =
        roleFeatureProvider.current()
            .homeActions()
            .map(HomeAction::byId)
}
