package com.rabbitmes.mobile

import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.FieldType
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.OperationType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun parseWeighingRabbitWeights(rawWeights: String?): List<Int> =
    rawWeights
        .orEmpty()
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it > 0 }

internal fun MobileTask.productionResultJson(comment: String = result.comment): String = buildJsonObject {
    val fields = MockRepository.operation(operationType).fields.associateBy { it.id }
    if (operationType == OperationType.SLAUGHTER_SHIPMENT) {
        val rawCount = result.values["animalCount"] ?: result.values["count"]
        rawCount?.toDoubleOrNull()?.toInt()?.let { put("animalCount", it) }
    } else {
        result.values.forEach { (key, value) ->
            if (value.isBlank()) return@forEach
            when (fields[key]?.type) {
                FieldType.BOOLEAN -> put(key, value.toBooleanStrictOrNull() ?: false)
                FieldType.NUMBER, FieldType.TEMPERATURE, FieldType.HOURS -> {
                    value.toLongOrNull()?.let { put(key, it) }
                        ?: value.toDoubleOrNull()?.let { put(key, it) }
                        ?: put(key, value)
                }
                FieldType.PHOTO, FieldType.VIDEO, FieldType.FILE -> Unit
                else -> put(key, value)
            }
        }
    }
    if (comment.isNotBlank()) put("comment", comment)
}.toString()
