package ru.profikrol.operator.feature.rabbitculling

interface RabbitCullingService {

    suspend fun getCages(): List<Cage>

    suspend fun getCullingReasons(): List<CullingReason>
}