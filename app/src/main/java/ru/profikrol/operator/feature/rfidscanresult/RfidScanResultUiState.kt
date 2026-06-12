package ru.profikrol.operator.feature.rfidscanresult

import ru.profikrol.operator.domain.model.Rabbit

data class RfidScanResultUiState(
    val isLoading: Boolean = false,
    val rabbit: Rabbit? = null,
    val isError: Boolean = false,
)
