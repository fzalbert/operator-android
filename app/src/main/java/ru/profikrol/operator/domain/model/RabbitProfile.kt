package ru.profikrol.operator.domain.model

/** Динамическая карточка, не зависящая от заранее известного набора полей. */
data class RabbitProfile(
    val code: String,
    val status: String?,
    val classification: String?,
    val responseFields: LinkedHashMap<String, String> = linkedMapOf(),
)
