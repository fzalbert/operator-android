package ru.profikrol.operator.feature.weighing

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
class WeighingViewModel @Inject constructor(
    private val rabbitRepository: RabbitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeighingUiState())
    val uiState: StateFlow<WeighingUiState> = _uiState.asStateFlow()

    fun saveWeight(rfidCode: String, weight: String) {
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            rabbitRepository.saveRabbitWeight(
                rfidCode = rfidCode,
                weightKg = weight,
            )

            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
