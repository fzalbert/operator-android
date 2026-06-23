package ru.profikrol.operator.feature.rabbitprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.profikrol.operator.domain.repository.RabbitRepository
import javax.inject.Inject

@HiltViewModel
class RabbitProfileViewModel @Inject constructor(
    private val rabbitRepository: RabbitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RabbitProfileScreenState())
    val uiState: StateFlow<RabbitProfileScreenState> = _uiState.asStateFlow()

    fun loadProfile(rfidCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }

            rabbitRepository.getRabbitProfile(rfidCode)
                .onSuccess { responseFields ->
                    _uiState.value = RabbitProfileScreenState(
                        isLoading = false,
                        profile = responseFields
                            .toRabbitProfile()
                            .toUiModel(),
                    )
                }
                .onFailure {
                    _uiState.value = RabbitProfileScreenState(
                        isLoading = false,
                        isError = true,
                    )
                }
        }
    }
}
