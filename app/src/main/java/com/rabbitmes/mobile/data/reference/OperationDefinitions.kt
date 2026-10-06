package com.rabbitmes.mobile.data.reference

import com.rabbitmes.mobile.domain.FieldType
import com.rabbitmes.mobile.domain.OperationDefinition
import com.rabbitmes.mobile.domain.OperationField
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.TargetType

/**
 * Базовые описания форм операций: поля результата, тип цели и нужен ли скан.
 * Списки клеток и рядов в поля подставляет [OperationCatalog].
 */
internal object OperationDefinitions {
    private val definitions: Map<OperationType, OperationDefinition> = listOf(
        OperationDefinition(OperationType.MORTALITY_ROUND, TargetType.HANGAR, false, "Обход завершен", listOf(
            OperationField("deadCount", "Падеж", FieldType.NUMBER, true),
            OperationField("ammonia", "NH₃", FieldType.NUMBER, false, "ppm"),
        )),
        OperationDefinition(OperationType.FEMALE_ARRIVAL, TargetType.CAGE, false, "Заселить самку", listOf(
            OperationField("age", "Возраст, дней", FieldType.NUMBER, true),
            OperationField("femaleRfid", "RFID самки", FieldType.TEXT, true),
        )),
        OperationDefinition(OperationType.INSEMINATION, TargetType.RABBIT, true, "Осеменить", listOf(
            OperationField("rfid", "RFID самки", FieldType.TEXT, true),
            OperationField("inseminated", "Самка осеменена", FieldType.BOOLEAN, true),
            OperationField("comment", "Комментарий", FieldType.TEXT),
        )),
        OperationDefinition(OperationType.PALPATION, TargetType.RABBIT, true, "Зафиксировать пальпацию", listOf(
            OperationField("rfid", "RFID самки", FieldType.TEXT, true),
            OperationField("result", "Результат", FieldType.SELECT, true, options = listOf("Сукрольная", "Не сукрольная", "Сомнительно")),
            OperationField("comment", "Комментарий", FieldType.TEXT),
        )),
        OperationDefinition(OperationType.NEST_PREPARATION, TargetType.CAGE, false, "Гнездо подготовлено", listOf(
            OperationField("nestReady", "Гнездо готово", FieldType.BOOLEAN, true),
        )),
        OperationDefinition(OperationType.NEST_EQUALIZATION, TargetType.CAGE, false, "Сохранить по клетке", listOf(
            OperationField("alive", "Живые", FieldType.NUMBER, true),
            OperationField("stillborn", "Мертворождённые", FieldType.NUMBER, true),
            OperationField("removed", "Забрали", FieldType.NUMBER, true),
            OperationField("added", "Добавили", FieldType.NUMBER, true),
        )),
        OperationDefinition(OperationType.ANIMAL_SETTLEMENT, TargetType.HANGAR, true, "Переселить", listOf(
            OperationField("rfid", "RFID", FieldType.TEXT, true),
            OperationField("cellId", "Клетка назначения", FieldType.TEXT, true),
            OperationField("comment", "Комментарий", FieldType.TEXT),
        )),
        OperationDefinition(OperationType.SLAUGHTER_SHIPPING, TargetType.CAGE, false, "Забой завершён", listOf(
            OperationField("animalCount", "Количество животных", FieldType.NUMBER, true),
        )),
        OperationDefinition(OperationType.WEIGHING_RABBIT, TargetType.CAGE, false, "Сохранить веса кроликов", listOf(
            OperationField("weightsGrams", "Вес мясных кроликов", FieldType.NUMBER, true, "г"),
            OperationField("photo", "Фото показаний весов", FieldType.PHOTO),
        )),
        OperationDefinition(OperationType.WEIGHING_CAGE, TargetType.CAGE, false, "Сохранить общий вес клетки", listOf(
            OperationField("weightGrams", "Общий вес клетки", FieldType.NUMBER, true, "г"),
            OperationField("photo", "Фото показаний весов", FieldType.PHOTO),
        )),
        OperationDefinition(OperationType.GENERAL, TargetType.HANGAR, false, "Выполнить пункт", emptyList()),
    ).associateBy { it.type }

    val all: List<OperationDefinition> get() = definitions.values.toList()

    fun of(type: OperationType): OperationDefinition = definitions.getValue(type)
}
