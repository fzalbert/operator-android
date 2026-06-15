package ru.profikrol.operator.feature.rabbitculling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RabbitCullingViewModel @Inject constructor(
    private val service: RabbitCullingService,
) : ViewModel() {

    private val _cages = MutableStateFlow<List<Cage>>(emptyList())
    val cages = _cages.asStateFlow()

    private val _reasons = MutableStateFlow<List<CullingReason>>(emptyList())
    val reasons = _reasons.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _cages.value = service.getCages()
            _reasons.value = service.getCullingReasons()
        }
    }
}