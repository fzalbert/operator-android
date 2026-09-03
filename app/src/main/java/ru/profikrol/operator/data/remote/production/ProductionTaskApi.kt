package ru.profikrol.operator.data.remote.production

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductionTaskApi {
    @GET("api/v1/production/tasks/employee/{employeeId}")
    suspend fun getEmployeeTasks(@Header("X-Employee-Id") requesterEmployeeId: String, @Path("employeeId") employeeId: String, @Query("completed") completed: Boolean = false): List<ProductionTaskDto>

    @GET("api/v1/production/tasks/{id}")
    suspend fun getTask(@Header("X-Employee-Id") employeeId: String, @Path("id") id: String): ProductionTaskDetailsDto

    @POST("api/v1/production/tasks/{id}/start")
    suspend fun startTask(@Header("X-Employee-Id") employeeId: String, @Path("id") id: String): Response<ResponseBody>

    @POST("api/v1/production/tasks/{id}/targets")
    suspend fun addTarget(@Header("X-Employee-Id") employeeId: String, @Path("id") taskId: String, @Body request: AddProductionTargetRequest): ResponseBody

    @POST("api/v1/production/tasks/{id}/targets/{targetId}/complete")
    suspend fun completeTarget(@Header("X-Employee-Id") employeeId: String, @Path("id") taskId: String, @Path("targetId") targetId: String, @Body request: CompleteTargetRequest)

    @POST("api/v1/production/tasks/{id}/targets/{targetId}/problem")
    suspend fun reportTargetCommentProblem(@Header("X-Employee-Id") employeeId: String, @Path("id") taskId: String, @Path("targetId") targetId: String, @Body request: ProductionTargetCommentProblemRequest)

    @POST("api/v1/production/tasks/{id}/targets/{targetId}/problem")
    suspend fun reportMortalityCountProblem(@Header("X-Employee-Id") employeeId: String, @Path("id") taskId: String, @Path("targetId") targetId: String, @Body request: ProductionMortalityCountProblemRequest)

    @POST("api/v1/production/tasks/{id}/complete")
    suspend fun completeTask(@Header("X-Employee-Id") employeeId: String, @Path("id") taskId: String)
}

@Serializable
data class AddProductionTargetRequest(
    val targetKind: String,
    val cageId: Long? = null,
    val rabbitId: Long? = null,
)

@Serializable
data class CompleteTargetRequest(val result: JsonObject? = null, val rfid: String? = null, val deviceId: String? = null)

@Serializable
data class ProductionTargetCommentProblemRequest(val comment: String)

@Serializable
data class ProductionMortalityCountProblemRequest(val result: MortalityCountResult)

@Serializable
data class MortalityCountResult(val count: Int)

@Serializable
data class ProductionTaskDto(
    val id: String,
    val workshopId: Long = 0,
    val hangarId: Long? = null,
    val operationCode: String? = null,
    val scheduledDate: String = "",
    val title: String? = null,
    val description: String? = null,
    val assignedEmployeeId: String? = null,
    val durationMinutes: Int? = null,
    val requiresAcceptance: Boolean = false,
    val executionStatus: String? = null,
    val targets: List<ProductionTargetDto> = emptyList(),
    val checkList: List<ProductionTargetDto> = emptyList(),
)

@Serializable
data class ProductionTaskDetailsDto(
    val task: ProductionTaskDto,
    val targets: List<ProductionTargetDto> = emptyList(),
    val checklist: List<ProductionTargetDto> = emptyList(),
)

@Serializable
data class ProductionTargetDto(
    val id: String,
    val targetType: String? = null,
    val targetId: String? = null,
    val targetKind: String? = null,
    val displayCode: String? = null,
    val rabbitId: String? = null,
    val cageId: Long? = null,
    val hangarId: Long? = null,
    val status: String? = null,
    val completedAt: String? = null,
    val resultJson: String? = null,
    val scanIdentifier: String? = null,
    val sortOrder: Int = 0,
)
