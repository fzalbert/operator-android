package ru.profikrol.operator.feature.nestpreparation

data class NestPreparationUiState(
    val row: String = "",
    val cage: String = "",
    val isSawdustAdded: Boolean = false,
    val isNestInstalled: Boolean = false,
    val isLoading: Boolean = false,
) {
    val isCageSelected: Boolean
        get() = row.isNotBlank() && cage.isNotBlank()

    val selectedCageName: String
        get() = "${row.uppercase()}–${cage.padStart(2, '0')}"

    val canFinish: Boolean
        get() = isCageSelected &&
            isSawdustAdded &&
            isNestInstalled &&
            !isLoading
}
