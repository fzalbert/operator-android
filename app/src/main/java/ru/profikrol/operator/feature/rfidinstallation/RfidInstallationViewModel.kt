package ru.profikrol.operator.feature.rfidinstallation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RfidInstallationViewModel @Inject constructor() : ViewModel() {

    private val _scannedRfid = MutableStateFlow<String?>(null)
    val scannedRfid = _scannedRfid.asStateFlow()

    fun setRfid(code: String) {
        _scannedRfid.value = code.takeIf(String::isNotBlank)
    }
}
