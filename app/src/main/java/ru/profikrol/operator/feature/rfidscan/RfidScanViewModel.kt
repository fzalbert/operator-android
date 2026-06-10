package ru.profikrol.operator.feature.rfidscan

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
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class RfidScanViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RfidScanUiState())
    val uiState: StateFlow<RfidScanUiState> = _uiState.asStateFlow()

    private val _events = Channel<RfidScanEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Демо-сканирование: генерим случайный код вида RF-00247. */
    fun onDemoScanClick() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val code = generateDemoRfidCode()
            _uiState.update { it.copy(isScanning = false) }
            _events.send(RfidScanEvent.Scanned(code))
        }
    }

    private fun generateDemoRfidCode(): String {
        val number = Random.nextInt(0, 100_000).toString().padStart(5, '0')
        return "RF-$number"
    }
}

sealed interface RfidScanEvent {
    data class Scanned(val code: String) : RfidScanEvent
}
