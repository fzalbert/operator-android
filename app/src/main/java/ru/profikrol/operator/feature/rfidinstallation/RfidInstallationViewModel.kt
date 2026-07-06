package ru.profikrol.operator.feature.rfidinstallation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RfidInstallationViewModel : ViewModel() {

    private val _scannedRfid = MutableStateFlow<String?>(null)
    val scannedRfid = _scannedRfid.asStateFlow()

    fun setRfid(code: String) {
        _scannedRfid.value = code
    }
}