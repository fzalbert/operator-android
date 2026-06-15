package ru.profikrol.operator.feature.rfidscanresult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.profikrol.operator.core.role.RoleFeatureProvider
import ru.profikrol.operator.domain.repository.RabbitRepository
import javax.inject.Inject

@HiltViewModel
class RfidScanResultViewModel @Inject constructor(
    private val rabbitRepository: RabbitRepository,
    roleFeatureProvider: RoleFeatureProvider,
) : ViewModel() {

    // Список действий определяется ролью текущего пользователя.
    private val actions: List<RabbitAction> =
        roleFeatureProvider.current()
            .rabbitScanActions()
            .map(RabbitAction::byId)

    private val _uiState = MutableStateFlow(
        RfidScanResultUiState(isLoading = true, actions = actions),
    )
    val uiState: StateFlow<RfidScanResultUiState> = _uiState.asStateFlow()

    fun loadRabbit(rfidCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }

            rabbitRepository.getRabbitByRfid(rfidCode)
                .onSuccess { rabbit ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rabbit = rabbit,
                            isError = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rabbit = null,
                            isError = true,
                        )
                    }
                }
        }
    }
}
