package com.rabbitmes.mobile.domain

/**
 * Единое правило «этот RFID относится к пункту чек-листа».
 *
 * Подпись пункта проверяется только на точное вхождение RFID отдельным словом:
 * частично введённый RFID не должен совпадать с чужой подписью.
 */
fun ChecklistItem.matchesRfid(rfid: String, resolvedRabbitId: String? = null): Boolean {
    val value = rfid.trim()
    if (value.isEmpty()) return false
    return targetId.equals(value, ignoreCase = true) ||
        rabbitId.equals(value, ignoreCase = true) ||
        scanIdentifier.equals(value, ignoreCase = true) ||
        result.scannedRfid.equals(value, ignoreCase = true) ||
        resolvedRabbitId?.let { targetId.equals(it, ignoreCase = true) } == true ||
        label.containsWholeToken(value)
}

private fun String.containsWholeToken(token: String): Boolean =
    Regex("(?<![\\p{L}\\p{N}])${Regex.escape(token)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        .containsMatchIn(this)
