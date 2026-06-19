package ru.profikrol.operator.feature.nestpreparation

import jakarta.inject.Inject

/**
 * Заглушка сервиса подготовки гнёзд.
 * Методы будут заменены сетевыми запросами после появления API.
 */
class NestPreparationServiceImpl @Inject constructor() : NestPreparationService {

    override suspend fun addSawdust(row: String, cage: String) = Unit

    override suspend fun installNest(row: String, cage: String) = Unit

    override suspend fun finishPreparation(row: String, cage: String) = Unit
}
