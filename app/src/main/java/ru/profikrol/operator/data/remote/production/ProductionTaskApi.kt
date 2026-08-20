package ru.profikrol.operator.data.remote.production

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductionTaskApi {
    @GET("api/production/tasks/employee/{employeeId}")
    suspend fun getEmployeeTasks(@Path("employeeId") employeeId: String, @Query("completed") completed: Boolean = false): List<ProductionTaskDto>

    @GET("api/production/tasks/{id}")
    suspend fun getTask(@Path("id") id: String): ProductionTaskDetailsDto

    @POST("api/production/tasks/{id}/targets/{targetId}/complete")
    suspend fun completeTarget(@Path("id") taskId: String, @Path("targetId") targetId: String, @Body request: CompleteTargetRequest)

    @POST("api/production/tasks/{id}/result")
    suspend fun submitTaskResult(@Path("id") taskId: String, @Body request: SubmitTaskResultRequest)

    @POST("api/production/tasks/{id}/complete")
    suspend fun completeTask(@Path("id") taskId: String)
}

@Serializable
data class CompleteTargetRequest(val employeeId: String, val resultJson: String? = null, val rfid: String? = null, val deviceId: String? = null)

@Serializable
data class SubmitTaskResultRequest(val employeeId: String, val resultJson: String? = null)

@Serializable
data class ProductionTaskDto(
    val id: String,
    val workshopId: Long = 0,
    val hangarId: Long? = null,
    val operationCode: String? = null,
    val scheduledDate: String = "",
    val title: String? = null,
    val description: String? = null,
    val taskType: String? = null,
    val assignedEmployeeId: String? = null,
    val durationMinutes: Int? = null,
    val requiresAcceptance: Boolean = false,
    val executionStatus: String? = null,
)

@Serializable
data class ProductionTaskDetailsDto(val task: ProductionTaskDto, val targetRule: ProductionTargetRuleDto? = null, val targets: List<ProductionTargetDto> = emptyList())

@Serializable
data class ProductionTargetRuleDto(val targetType: String? = null, val executionScope: String? = null, val requiresRfid: Boolean = false)

@Serializable
data class ProductionTargetDto(
    val id: String,
    val targetType: String? = null,
    val targetId: String? = null,
    val displayCode: String? = null,
    val rabbitId: String? = null,
    val cageId: Long? = null,
    val hangarId: Long? = null,
    val status: String? = null,
    val completedAt: String? = null,
    val scanIdentifier: String? = null,
)
