package ru.profikrol.operator.feature.nestalignment

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
import ru.profikrol.operator.domain.repository.RabbitRepository
import javax.inject.Inject

@HiltViewModel
class NestAlignmentViewModel @Inject constructor(
    private val rabbitRepository: RabbitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NestAlignmentUiState())
    val uiState: StateFlow<NestAlignmentUiState> = _uiState.asStateFlow()

    private val _events = Channel<NestAlignmentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onRfidScanned(target: NestAlignmentScanTarget, rfidCode: String) {
        if (rfidCode.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadingTarget = target,
                    errorTarget = null,
                )
            }

            rabbitRepository.getRabbitByRfid(rfidCode)
                .onSuccess { rabbit ->
                    val nest = rabbit.toNestAlignmentNest()
                    _uiState.update { state ->
                        val updatedState = when (target) {
                            NestAlignmentScanTarget.Donor -> state.copy(donor = nest)
                            NestAlignmentScanTarget.Recipient -> state.copy(recipient = nest)
                        }

                        updatedState.copy(
                            transferCount = updatedState.coerceTransferCount(),
                            loadingTarget = null,
                            errorTarget = null,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            loadingTarget = null,
                            errorTarget = target,
                        )
                    }
                }
        }
    }

    fun onTransferCountChange(count: Int) {
        _uiState.update { state ->
            state.copy(transferCount = state.coerceTransferCount(count))
        }
    }

    fun onClearTarget(target: NestAlignmentScanTarget) {
        _uiState.update { state ->
            when (target) {
                NestAlignmentScanTarget.Donor -> NestAlignmentUiState()
                NestAlignmentScanTarget.Recipient -> {
                    state.copy(
                        recipient = null,
                        transferCount = state.coerceTransferCount(),
                        loadingTarget = null,
                        errorTarget = null,
                    )
                }
            }
        }
    }

    fun onConfirmClick() {
        val draft = _uiState.value.toMoveDraft() ?: return
        // TODO: send this draft through a NestAlignmentRepository once the API is ready.
        viewModelScope.launch {
            _events.send(NestAlignmentEvent.MoveDraftReady(draft))
        }
    }

    fun onRestartClick() {
        _uiState.value = NestAlignmentUiState()
    }

    private fun NestAlignmentUiState.coerceTransferCount(
        count: Int = transferCount,
    ): Int {
        val donorCount = donor?.rabbitsCount ?: return 1
        if (donorCount <= 0) return 0
        return count.coerceIn(1, donorCount)
    }
}

sealed interface NestAlignmentEvent {
    data class MoveDraftReady(val draft: NestAlignmentMoveDraft) : NestAlignmentEvent
}
