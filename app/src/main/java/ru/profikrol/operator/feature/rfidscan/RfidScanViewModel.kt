package ru.profikrol.operator.feature.rfidscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.profikrol.operator.domain.nfc.NfcReader
import ru.profikrol.operator.domain.nfc.ScannedPayload
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class RfidScanViewModel @Inject constructor(
    private val nfcReader: NfcReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RfidScanUiState())
    val uiState: StateFlow<RfidScanUiState> = _uiState.asStateFlow()

    private val _events = Channel<RfidScanEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Демо-сканирование: генерим случайный код вида RF-00247. */
    fun onDemoScanClick() {
        if (_uiState.value.isDemoScanInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDemoScanInProgress = true) }
            delay(RfidScanDemoDurationMillis)
            val code = generateDemoRfidCode()
            _uiState.update { it.copy(isDemoScanInProgress = false) }
            _events.send(RfidScanEvent.Scanned(code))
        }
    }

    fun startNfcScanning() {
        if (!nfcReader.isAvailable) return
        nfcReader.start(::onNfcScanned)
    }

    fun stopNfcScanning() {
        nfcReader.stop()
    }

    private fun onNfcScanned(payload: ScannedPayload) {
        if (payload.value.isBlank()) return
        viewModelScope.launch {
            _events.send(RfidScanEvent.Scanned(payload.value))
        }
    }

    private fun generateDemoRfidCode(): String {
        val number = Random.nextInt(0, 100_000).toString().padStart(5, '0')
        return "RF-$number"
    }

    override fun onCleared() {
        nfcReader.stop()
        super.onCleared()
    }
}

sealed interface RfidScanEvent {
    data class Scanned(val code: String) : RfidScanEvent
}

private const val RfidScanDemoDurationMillis = 1_200L
