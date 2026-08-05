package ru.profikrol.operator.data.remote.worktask

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface WorkTaskApi {

    @GET("api/v1/profile/me/work-tasks")
    suspend fun getMyWorkTasks(
        @Query("status") status: List<String>? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("manufactureId") manufactureId: Long? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): WorkTaskPageDto
}

@Serializable
data class WorkTaskPageDto(
    val items: List<WorkTaskDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class WorkTaskDto(
    val id: Long,
    val programScheduleId: Long? = null,
    val programOperationId: Long? = null,
    val scheduledDate: String = "",
    val status: String = "",
    val assignedRoleId: Long? = null,
    val assignedEmployeeId: String? = null,
    val requiresAcceptance: Boolean = false,
    val acceptanceRoleId: Long? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val manufactureId: Long? = null,
    val manufactureName: String = "",
    val programId: Long? = null,
    val programName: String = "",
    val operationId: String = "",
    val operationName: String = "",
    val operationCategory: String = "",
    val subtasks: List<WorkSubtaskDto> = emptyList(),
)

@Serializable
data class WorkSubtaskDto(
    val id: Long,
    val taskId: Long? = null,
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val status: String = "",
    val completedByEmployeeId: String? = null,
    val completedAt: String? = null,
    val skipReason: String? = null,
    val report: WorkSubtaskReportDto? = null,
)

@Serializable
data class WorkSubtaskReportDto(
    val id: Long = 0,
    val status: String = "",
    val reportedByEmployeeId: String? = null,
    val reportedAt: String? = null,
    val abortReason: String? = null,
    val acceptedByEmployeeId: String? = null,
    val acceptedAt: String? = null,
)
