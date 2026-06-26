package ru.profikrol.operator.feature.nestpreparation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NestPreparationViewModel @Inject constructor(
    private val service: NestPreparationService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NestPreparationUiState())
    val uiState: StateFlow<NestPreparationUiState> = _uiState.asStateFlow()

    private val _events = Channel<NestPreparationEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onRowChange(value: String) {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                row = value.filter(Char::isLetter).take(MaxRowLength),
                isSawdustAdded = false,
                isNestInstalled = false,
            )
        }
    }

    fun onCageChange(value: String) {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                cage = value.filter(Char::isDigit).take(MaxCageLength),
                isSawdustAdded = false,
                isNestInstalled = false,
            )
        }
    }

    fun onAddSawdustClick() {
        val state = _uiState.value
        if (!state.isCageSelected || state.isSawdustAdded || state.isLoading) return

        executeRequest(
            request = {
                service.addSawdust(
                    row = state.row,
                    cage = state.cage,
                )
            },
            onSuccess = {
                _uiState.update { it.copy(isSawdustAdded = true) }
            },
        )
    }

    fun onInstallNestClick() {
        val state = _uiState.value
        if (!state.isCageSelected || state.isNestInstalled || state.isLoading) return

        executeRequest(
            request = {
                service.installNest(
                    row = state.row,
                    cage = state.cage,
                )
            },
            onSuccess = {
                _uiState.update { it.copy(isNestInstalled = true) }
            },
        )
    }

    fun onFinishClick() {
        val state = _uiState.value
        if (!state.canFinish) return

        executeRequest(
            request = {
                service.finishPreparation(
                    row = state.row,
                    cage = state.cage,
                )
            },
            onSuccess = {
                _events.send(NestPreparationEvent.Finished)
            },
        )
    }

    private fun executeRequest(
        request: suspend () -> Unit,
        onSuccess: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                request()
                onSuccess()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                _events.send(NestPreparationEvent.RequestFailed)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

sealed interface NestPreparationEvent {
    data object Finished : NestPreparationEvent
    data object RequestFailed : NestPreparationEvent
}

private const val MaxRowLength = 1
private const val MaxCageLength = 3
