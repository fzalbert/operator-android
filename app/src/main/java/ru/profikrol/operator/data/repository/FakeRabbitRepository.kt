package ru.profikrol.operator.data.repository

import kotlinx.coroutines.delay
import ru.profikrol.operator.domain.model.Rabbit
import ru.profikrol.operator.domain.repository.RabbitError
import ru.profikrol.operator.domain.repository.RabbitRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException


@Singleton
class FakeRabbitRepository @Inject constructor() : RabbitRepository {

    override suspend fun getRabbitByRfid(rfidCode: String): Result<Rabbit> {
        return try {
            delay(800)

            if (rfidCode.isBlank()) {
                return Result.failure(RabbitError.NotFound)
            }

            Result.success(
                Rabbit(
                    rfidCode = rfidCode,
                    status = "Здорова",
                    age = "8 мес",
                    cage = "A-12",
                    weight = "3.2 кг",
                    diagnosis = "Здорова",
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(RabbitError.Unknown)
        }
    }
}
