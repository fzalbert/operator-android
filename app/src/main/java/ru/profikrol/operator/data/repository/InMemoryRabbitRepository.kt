package ru.profikrol.operator.data.repository

import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.profikrol.operator.domain.model.Rabbit
import ru.profikrol.operator.domain.repository.RabbitError
import ru.profikrol.operator.domain.repository.RabbitRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException


@Singleton
class InMemoryRabbitRepository @Inject constructor() : RabbitRepository {

    override suspend fun getRabbitByRfid(rfidCode: String): Result<Rabbit> {
        return try {
            delay(800)

            if (rfidCode.isBlank()) {
                return Result.failure(RabbitError.NotFound)
            }

            parseRabbitFromTagPayload(rfidCode)?.let { rabbit ->
                return Result.success(rabbit)
            }

            Result.success(
                Rabbit(
                    rfidCode = rfidCode,
                    status = "Здорова",
                    age = "8 мес",
                    cage = "A-12",
                    weight = "3.2 кг",
                    diagnosis = "Здорова",
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(RabbitError.Unknown)
        }
    }

    private fun parseRabbitFromTagPayload(payload: String): Rabbit? {
        val trimmedPayload = payload.trim()
        return when {
            trimmedPayload.startsWith("{") -> parseRabbitFromJson(trimmedPayload)
            trimmedPayload.contains("=") || trimmedPayload.contains(":") -> {
                parseRabbitFromKeyValue(trimmedPayload)
            }
            else -> null
        }
    }

    private fun parseRabbitFromJson(payload: String): Rabbit? =
        runCatching {
            val fields = Json.parseToJsonElement(payload).jsonObject
            fields.toRabbit()
        }.getOrNull()

    private fun parseRabbitFromKeyValue(payload: String): Rabbit? {
        val fields = payload
            .lineSequence()
            .flatMap { line -> line.splitToSequence(';') }
            .mapNotNull { row ->
                val parts = row.splitKeyValue()
                val key = parts.getOrNull(0)?.trim().orEmpty()
                val value = parts.getOrNull(1)?.trim().orEmpty()
                if (key.isBlank() || value.isBlank()) null else key to value
            }
            .toMap()

        return fields.toRabbit()
    }

    private fun Map<String, String>.toRabbit(): Rabbit {
        val rfidCode = firstValue(
            "rfidCode",
            "rfid",
            "rfid код",
            "rfid-код",
            "code",
            "код",
            "метка",
        ) ?: DefaultRabbitRfidCode

        return Rabbit(
            rfidCode = rfidCode,
            status = firstValue("status", "статус") ?: DefaultRabbitStatus,
            age = firstValue("age", "возраст") ?: DefaultRabbitAge,
            cage = firstValue("cage", "клетка") ?: DefaultRabbitCage,
            weight = firstValue("weight", "вес") ?: DefaultRabbitWeight,
            diagnosis = firstValue("diagnosis", "диагноз") ?: DefaultRabbitDiagnosis,
        )
    }

    private fun JsonObject.toRabbit(): Rabbit =
        mapValues { (_, value) -> (value as? JsonPrimitive)?.jsonPrimitive?.content.orEmpty() }
            .toRabbit()

    private fun Map<String, String>.firstValue(vararg keys: String): String? {
        val normalizedFields = entries.associate { (key, value) ->
            key.lowercase() to value
        }

        keys.forEach { key ->
            normalizedFields[key.lowercase()]?.takeIf(String::isNotBlank)?.let { return it }
        }
        return null
    }

    private fun String.splitKeyValue(): List<String> {
        val equalsIndex = indexOf('=')
        val colonIndex = indexOf(':')
        val separatorIndex = listOf(equalsIndex, colonIndex)
            .filter { it >= 0 }
            .minOrNull()
            ?: return emptyList()

        return listOf(
            substring(startIndex = 0, endIndex = separatorIndex),
            substring(startIndex = separatorIndex + 1),
        )
    }

    override suspend fun saveRabbitWeight(rfidCode: String, weightKg: String): Result<Unit> {
        return try {
            delay(600)

            if (rfidCode.isBlank() || weightKg.isBlank()) {
                return Result.failure(RabbitError.InvalidWeight)
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(RabbitError.Unknown)
        }
    }
}

private const val DefaultRabbitRfidCode = "NFC-метка"
private const val DefaultRabbitStatus = "Здорова"
private const val DefaultRabbitAge = "8 мес"
private const val DefaultRabbitCage = "A-12"
private const val DefaultRabbitWeight = "3.2 кг"
private const val DefaultRabbitDiagnosis = "Здорова"
