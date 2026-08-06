package ru.profikrol.operator.data.remote.rabbit

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface RabbitApi {
    @GET("api/v1/rabbits")
    suspend fun getRabbits(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
    ): List<RabbitDto>
}

@Serializable
data class RabbitDto(
    val id: Long? = null,
    val age: Int = 0,
    val rfid: String? = null,
)
