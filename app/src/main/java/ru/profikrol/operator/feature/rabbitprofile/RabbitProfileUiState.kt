package ru.profikrol.operator.feature.rabbitprofile

data class RabbitProfileScreenState(
    val isLoading: Boolean = true,
    val profile: RabbitProfileUiModel? = null,
    val isError: Boolean = false,
)
