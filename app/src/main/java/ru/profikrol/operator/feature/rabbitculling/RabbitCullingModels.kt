package ru.profikrol.operator.feature.rabbitculling

data class Cage(
    val id: Long,
    val name: String,
)

data class CullingReason(
    val id: Long,
    val title: String,
)