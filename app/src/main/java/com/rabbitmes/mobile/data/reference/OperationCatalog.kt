package com.rabbitmes.mobile.data.reference

import com.rabbitmes.mobile.domain.FieldType
import com.rabbitmes.mobile.domain.OperationDefinition
import com.rabbitmes.mobile.domain.OperationField
import com.rabbitmes.mobile.domain.OperationType
import ru.profikrol.operator.data.remote.cell.CellDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Описания операций (поля форм, тип цели). Базовые описания статичны,
 * списки клеток и рядов подставляются из справочников фермы.
 */
@Singleton
class OperationCatalog @Inject constructor(
    private val references: FarmReferenceRepository,
) {
    val all: List<OperationDefinition> get() = OperationDefinitions.all

    fun definition(type: OperationType): OperationDefinition {
        val definition = OperationDefinitions.of(type)
        val cells = references.cells
        if (cells.isEmpty()) return definition
        val cellOptions = cells.map(CellDto::displayName)
        if (type == OperationType.MORTALITY_ROUND) {
            return definition.copy(
                fields = definition.fields + listOf(
                    OperationField(
                        id = "rowId",
                        title = "Ряд",
                        type = FieldType.SELECT,
                        options = listOf("Выберите ряд") + references.rows
                            .map { "Ряд ${it.number}" }
                            .distinct()
                            .sortedBy { it.substringAfterLast(' ').toIntOrNull() },
                    ),
                    OperationField(
                        id = "cageId",
                        title = "ID клетки",
                        type = FieldType.SELECT,
                        options = listOf("Выберите клетку") + cellOptions,
                    ),
                ),
            )
        }
        return definition.copy(
            fields = definition.fields.map { field ->
                if (field.id == "cellId") {
                    field.copy(options = listOfNotNull(field.options.firstOrNull()) + cellOptions)
                } else {
                    field
                }
            },
        )
    }
}
