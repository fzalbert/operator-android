package ru.profikrol.operator.feature.nestpreparation

interface NestPreparationService {

    suspend fun addSawdust(row: String, cage: String)

    suspend fun installNest(row: String, cage: String)

    suspend fun finishPreparation(row: String, cage: String)
}
