package ru.profikrol.operator.domain.repository

import ru.profikrol.operator.domain.model.Rabbit

interface RabbitRepository {

    suspend fun getRabbitByRfid(rfidCode: String): Result<Rabbit>

    suspend fun saveRabbitWeight(rfidCode: String, weightKg: String): Result<Unit>
}

sealed class RabbitError(message: String) : Throwable(message) {
    data object NotFound : RabbitError("Rabbit not found")
    data object InvalidWeight : RabbitError("Invalid weight")
    data object Network : RabbitError("Network error")
    data object Unknown : RabbitError("Unknown error")
}
