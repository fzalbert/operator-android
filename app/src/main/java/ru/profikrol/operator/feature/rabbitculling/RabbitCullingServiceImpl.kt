package ru.profikrol.operator.feature.rabbitculling

import jakarta.inject.Inject

class RabbitCullingServiceImpl @Inject constructor() : RabbitCullingService {

    override suspend fun getCages(): List<Cage> =
        listOf(
            Cage(1, "A-12"),
            Cage(2, "A-13"),
            Cage(3, "B-01"),
        )

    override suspend fun getCullingReasons(): List<CullingReason> =
        listOf(
            CullingReason(1, "Болезнь"),
            CullingReason(2, "Травма"),
            CullingReason(3, "Возраст"),
            CullingReason(4, "Сифилис")
        )
}