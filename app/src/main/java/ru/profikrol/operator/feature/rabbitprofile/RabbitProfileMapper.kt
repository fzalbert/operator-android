package ru.profikrol.operator.feature.rabbitprofile

import ru.profikrol.operator.domain.model.RabbitProfile

private const val EmptyValue = "—"

/**
 * Модель экрана. LinkedHashMap сохраняет порядок полей таким, как на макете.
 */
data class RabbitProfileUiModel(
    val code: String,
    val status: String?,
    val classification: String,
    val sections: List<RabbitProfileSection>,
)

data class RabbitProfileSection(
    val title: String,
    val fields: LinkedHashMap<String, String>,
)

/**
 * Преобразует плоский ответ бэкенда в доменную модель.
 *
 * Поддерживаются camelCase, snake_case и русские названия ключей. После появления
 * конкретного DTO этот метод удобно вызывать из repository/data mapper.
 */
fun Map<String, Any?>.toRabbitProfile(): RabbitProfile {
    val fields = entries.associate { (key, value) ->
        key.normalizedKey() to value.asDisplayString()
    }

    fun value(vararg aliases: String): String? =
        aliases.firstNotNullOfOrNull { alias ->
            fields[alias.normalizedKey()]?.takeIf(String::isNotBlank)
        }

    val responseFields = entries
        .mapNotNull { (key, rawValue) ->
            val value = rawValue.asDisplayString()
            if (value.isBlank() || key.isHeaderField()) {
                null
            } else {
                key to value
            }
        }
        .toMap(LinkedHashMap())

    return RabbitProfile(
        code = value("code", "rabbitCode", "rfidCode", "rfid", "номер", "код") ?: EmptyValue,
        status = value("status", "reproductiveStatus", "статус"),
        classification = value("classification", "category", "классификация"),
        responseFields = responseFields,
    )
}

fun RabbitProfile.toUiModel(): RabbitProfileUiModel = RabbitProfileUiModel(
    code = code,
    status = status,
    classification = classification.orPlaceholder(),
    sections = buildSections(),
)

private fun String?.orPlaceholder(): String = this?.takeIf(String::isNotBlank) ?: EmptyValue

private fun RabbitProfile.buildSections(): List<RabbitProfileSection> {
    val generalFields = linkedMapOf<String, String>()
    val planningFields = linkedMapOf<String, String>()

    responseFields.forEach { (key, value) ->
        val target = if (key.isPlanningField()) planningFields else generalFields
        target[key.displayLabel()] = value
    }

    return listOf(
        RabbitProfileSection("Общие данные", generalFields),
        RabbitProfileSection("Планирование", planningFields),
    ).filter { section -> section.fields.isNotEmpty() }
}

private fun String.normalizedKey(): String =
    lowercase()
        .filter(Char::isLetterOrDigit)

private fun String.isHeaderField(): Boolean = normalizedKey() in HeaderFieldKeys

private fun String.isPlanningField(): Boolean = normalizedKey() in PlanningFieldKeys

private fun String.displayLabel(): String =
    FieldLabels[normalizedKey()]
        ?: replace(Regex("([a-zа-я0-9])([A-ZА-Я])"), "$1 $2")
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .replaceFirstChar { character -> character.titlecase() }

private fun Any?.asDisplayString(): String = when (this) {
    null -> ""
    is Boolean -> if (this) "Да" else "Нет"
    else -> toString().trim()
}

private val HeaderFieldKeys = setOf(
    "code",
    "rabbitcode",
    "rfidcode",
    "rfid",
    "номер",
    "код",
    "status",
    "reproductivestatus",
    "статус",
    "classification",
    "category",
    "классификация",
)

private val PlanningFieldKeys = setOf(
    "readyforinsemination",
    "inseminationreadiness",
    "готовностькосеменению",
    "plannedslaughterdate",
    "slaughterdate",
    "планируемаядатазабоя",
)

private val FieldLabels = mapOf(
    "gender" to "Пол",
    "sex" to "Пол",
    "пол" to "Пол",
    "age" to "Возраст",
    "возраст" to "Возраст",
    "breed" to "Порода",
    "порода" to "Порода",
    "weight" to "Вес (живая масса)",
    "liveweight" to "Вес (живая масса)",
    "вес" to "Вес (живая масса)",
    "живаямасса" to "Вес (живая масса)",
    "registrationdate" to "Дата регистрации",
    "registeredat" to "Дата регистрации",
    "датегистрации" to "Дата регистрации",
    "inseminationdate" to "Дата щенения (окрола)",
    "kindlingdate" to "Дата щенения (окрола)",
    "датаосеменения" to "Дата щенения (окрола)",
    "датащенения" to "Дата щенения (окрола)",
    "датаокрола" to "Дата щенения (окрола)",
    "department" to "Отделение",
    "division" to "Отделение",
    "отделение" to "Отделение",
    "responsibleemployee" to "Ответственный сотрудник",
    "employee" to "Ответственный сотрудник",
    "ответственныйсотрудник" to "Ответственный сотрудник",
    "transferdate" to "Дата перевода",
    "movedat" to "Дата перевода",
    "датаперевода" to "Дата перевода",
    "hangarnumber" to "Номер ангара",
    "hangar" to "Номер ангара",
    "номерангара" to "Номер ангара",
    "ангар" to "Номер ангара",
    "cage" to "Клетка",
    "cagenumber" to "Клетка",
    "клетка" to "Клетка",
    "diagnosis" to "Диагноз",
    "healthstatus" to "Диагноз",
    "диагноз" to "Диагноз",
    "feedtype" to "Вид корма",
    "feed" to "Вид корма",
    "видкорма" to "Вид корма",
    "корм" to "Вид корма",
    "readyforinsemination" to "Готовность к осеменению",
    "inseminationreadiness" to "Готовность к осеменению",
    "готовностькосеменению" to "Готовность к осеменению",
    "plannedslaughterdate" to "Планируемая дата забоя",
    "slaughterdate" to "Планируемая дата забоя",
    "планируемаядатазабоя" to "Планируемая дата забоя",
)
