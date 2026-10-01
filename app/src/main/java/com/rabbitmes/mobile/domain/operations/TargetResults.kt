package com.rabbitmes.mobile.domain.operations

import com.rabbitmes.mobile.domain.ChecklistItem
import com.rabbitmes.mobile.domain.OperationType
import com.rabbitmes.mobile.domain.PROBLEM_COMMENT_KEY
import com.rabbitmes.mobile.domain.PROBLEM_REASON_KEY
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Результат выполнения цели production-задачи, готовый к отправке на сервер. */
sealed interface TargetResult {
    data class Ready(val json: JsonObject, val rfid: String?) : TargetResult
    data class Invalid(val message: String) : TargetResult
}

/**
 * Правила выполнения цели для одного типа операции: что обязательно ввести
 * и какой JSON ждёт сервер в `result`.
 */
interface TargetResultHandler {
    val requiresRfid: Boolean get() = false

    fun validate(item: ChecklistItem, values: Map<String, String>): String? = null

    fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext)
}

/** Данные справочников, нужные для сборки результата. */
fun interface TargetResultContext {
    /** id клетки по тому, что выбрал оператор: подписи из списка или id. */
    fun cellId(selected: String): Long?
}

object TargetResults {
    private val handlers: Map<OperationType, TargetResultHandler> = mapOf(
        OperationType.NEST_SELECTION to NestSelectionHandler,
        OperationType.SLAUGHTER_SHIPMENT to SlaughterShipmentHandler,
        OperationType.WEIGHING_RABBIT to WeighingRabbitHandler,
        OperationType.WEIGHING to WeighingHandler,
        OperationType.WEIGHING_CAGE to WeighingHandler,
        OperationType.ANIMAL_TRANSFER to AnimalTransferHandler,
        OperationType.PALPATION to PalpationHandler,
        OperationType.INSEMINATION to InseminationHandler,
        OperationType.FEMALE_DELIVERY to FemaleDeliveryHandler,
    )

    fun handler(type: OperationType): TargetResultHandler = handlers[type] ?: GenericHandler

    fun build(
        type: OperationType,
        item: ChecklistItem,
        values: Map<String, String>,
        context: TargetResultContext = TargetResultContext { null },
    ): TargetResult {
        val handler = handler(type)
        val rfid = values["rfid"]?.trim()
        if (handler.requiresRfid && rfid.isNullOrBlank()) return TargetResult.Invalid("RFID обязателен")
        handler.validate(item, values)?.let { return TargetResult.Invalid(it) }
        val json = buildJsonObject { with(handler) { fill(item, values, context) } }
        return TargetResult.Ready(json, rfid)
    }
}

/** Все введённые значения как есть, кроме RFID и служебных ключей проблемы. */
private object GenericHandler : TargetResultHandler {
    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        values
            .filterKeys { it != "rfid" && it != PROBLEM_REASON_KEY && it != PROBLEM_COMMENT_KEY }
            .forEach { (key, value) -> put(key, value) }
    }
}

private object NestSelectionHandler : TargetResultHandler {
    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        put("alive", values["alive"]?.toIntOrNull() ?: 0)
        put("stillborn", values["stillborn"]?.toLongOrNull() ?: 0L)
        // Бэк принимает только одно из полей: либо забрали, либо положили.
        when {
            values.containsKey("removed") -> put("removed", values["removed"]?.toLongOrNull() ?: 0L)
            values.containsKey("added") -> put("added", values["added"]?.toLongOrNull() ?: 0L)
        }
    }
}

private object SlaughterShipmentHandler : TargetResultHandler {
    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        put("animalCount", values["animalCount"]?.toIntOrNull() ?: values["count"]?.toIntOrNull() ?: 0)
    }
}

/** Взвешивание мясных кроликов: цель — клетка, вес каждого кролика через запятую. */
private object WeighingRabbitHandler : TargetResultHandler {
    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        put("weightsGrams", JsonArray(parseWeighingRabbitWeights(values["weightsGrams"]).map(::JsonPrimitive)))
    }
}

internal fun parseWeighingRabbitWeights(rawWeights: String?): List<Int> =
    rawWeights
        .orEmpty()
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it > 0 }

private object WeighingHandler : TargetResultHandler {
    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        put("weightGrams", values["weightGrams"]?.toIntOrNull() ?: 0)
    }
}

private object PalpationHandler : TargetResultHandler {
    override val requiresRfid = true

    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        put("result", if (values["pregnant"].toBoolean()) "pregnant" else "not_pregnant")
    }
}

private object InseminationHandler : TargetResultHandler {
    override val requiresRfid = true

    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        put("inseminated", values["inseminated"]?.toBooleanStrictOrNull() ?: true)
    }
}

/** Заселение самки: клетка из цели, RFID и возраст в днях. */
private object FemaleDeliveryHandler : TargetResultHandler {
    override val requiresRfid = true

    override fun validate(item: ChecklistItem, values: Map<String, String>): String? {
        val age = values["age"]?.trim()?.toIntOrNull()
        return if (age == null || age <= 0) "Укажите возраст кролика в днях" else null
    }

    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        with(GenericHandler) { fill(item, values, context) }
        item.targetId.toLongOrNull()?.let { put("cellId", it) }
        values["rfid"]?.trim()?.let { put("femaleRfid", it) }
        values["age"]?.trim()?.toIntOrNull()?.let { put("age", it) }
    }
}

/** Переселение: клетка назначения, выбранная из списка. */
private object AnimalTransferHandler : TargetResultHandler {
    override fun JsonObjectBuilder.fill(item: ChecklistItem, values: Map<String, String>, context: TargetResultContext) {
        val selected = values["cellId"].orEmpty()
        val cellId = context.cellId(selected)
            ?: selected.trim().toLongOrNull()
            ?: Regex("(?i)\\bID\\s*(\\d+)").find(selected)?.groupValues?.getOrNull(1)?.toLongOrNull()
        cellId?.let { put("cellId", it) }
    }
}
