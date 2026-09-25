package com.rabbitmes.mobile

import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException

internal fun Throwable.toUserMessage(fallback: String): String = when (this) {
    is HttpException -> toUserMessage(fallback)
    is IOException -> "$fallback: нет соединения с сервером"
    else -> fallback
}

private fun HttpException.toUserMessage(fallback: String): String {
    val serverMessage = peekErrorBody()
        .extractServerErrorMessage()
        ?.takeIf(String::isSuitableForUser)
    if (serverMessage != null) return serverMessage.localizeServerFieldNames()

    return when (code()) {
        400 -> "$fallback. Проверьте введённые данные"
        401 -> "Сессия истекла. Войдите в приложение снова"
        403 -> "У вас нет доступа к этому действию"
        404 -> "$fallback. Данные не найдены или уже недоступны"
        409 -> "$fallback. Действие уже выполнено или задача находится в другом состоянии"
        422 -> "$fallback. Проверьте обязательные поля"
        429 -> "Слишком много запросов. Попробуйте немного позже"
        in 500..599 -> "Сервис временно недоступен. Попробуйте позже"
        else -> fallback
    }
}

internal fun String.localizeServerFieldNames(): String = this
    .replace("\"added\"", "«положили»", ignoreCase = true)
    .replace("\"removed\"", "«забрали»", ignoreCase = true)
    .replace(Regex("\\badded\\b", RegexOption.IGNORE_CASE), "«положили»")
    .replace(Regex("\\bremoved\\b", RegexOption.IGNORE_CASE), "«забрали»")
    .replace(Regex("\\banimalCount\\b", RegexOption.IGNORE_CASE), "«количество животных»")

private fun HttpException.peekErrorBody(): String = runCatching {
    response()?.errorBody()?.source()?.let { source ->
        source.request(Long.MAX_VALUE)
        source.buffer.clone().readUtf8()
    }
}.getOrNull().orEmpty()

internal fun String.extractServerErrorMessage(): String? {
    val body = trim()
    if (body.isBlank()) return null
    val parsed = runCatching { Json.parseToJsonElement(body) }.getOrNull()
        ?: return body
    val objectBody = parsed as? JsonObject ?: return parsed.errorText()
    return listOf("detail", "message", "error", "errors", "title")
        .firstNotNullOfOrNull { key -> objectBody[key]?.errorText() }
}

private fun JsonElement.errorText(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.trim()?.takeIf(String::isNotBlank)
    is JsonArray -> mapNotNull(JsonElement::errorText).distinct().joinToString(". ").takeIf(String::isNotBlank)
    is JsonObject -> values.mapNotNull(JsonElement::errorText).distinct().joinToString(". ").takeIf(String::isNotBlank)
}

private fun String.isSuitableForUser(): Boolean {
    if (length !in 3..300) return false
    val technicalMarkers = listOf(
        "exception",
        "stack trace",
        "unable to resolve service",
        "sqlstate",
        "system.",
        "npgsql",
    )
    return technicalMarkers.none { contains(it, ignoreCase = true) }
}

internal fun Throwable.toHttpDebugMessage(): String = when (this) {
    is HttpException -> {
        val body = peekErrorBody()
        "HTTP ${code()} ${message()}${body.takeIf(String::isNotBlank)?.let { ", body=$it" }.orEmpty()}"
    }
    else -> "${this::class.java.simpleName}: ${message.orEmpty()}"
}
