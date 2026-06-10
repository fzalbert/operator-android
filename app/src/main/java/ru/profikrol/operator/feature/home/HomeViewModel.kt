package ru.profikrol.operator.feature.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    // TODO: заменить хардкод на NotificationsRepository.unreadCount: Flow<Int>
    private val _uiState = MutableStateFlow(
        HomeUiState(unreadNotificationsCount = 3),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
