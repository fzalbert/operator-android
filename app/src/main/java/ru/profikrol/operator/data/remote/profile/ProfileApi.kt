package ru.profikrol.operator.data.remote.profile

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ProfileApi {

    @GET("api/v1/profile/me")
    suspend fun getMyProfile(): ProfileDto

}

/** Смены сотрудника в ProductionProgram. Сотрудник берётся из токена. */
interface ShiftApi {
    /** Начать смену в ангаре. Возвращает id смены; 409, если смена уже открыта. */
    @POST("api/v1/production/shifts/start")
    suspend fun startShift(@Body request: StartShiftRequest): Long

    /** Закрыть активную смену; 404, если открытой смены нет. */
    @POST("api/v1/production/shifts/close")
    suspend fun closeShift(): Response<Unit>
}

@Serializable
data class StartShiftRequest(
    val hangarId: Long,
    val deviceId: String?,
)

@Serializable
data class ProfileDto(
    val employeeId: String,
    val name: String = "",
    val surname: String = "",
    val secondName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val contactNumber: String? = null,
    val address: String? = null,
    val roles: List<ProfileRoleDto> = emptyList(),
    val shift: ShiftDto? = null,
    /** Цеха, за которыми закреплён сотрудник, с их ангарами. */
    val manufactures: List<ProfileManufactureDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ShiftDto(
    val id: Long = 0,
    /** Ангар, в котором сотрудник работает в эту смену. */
    val hangarId: Long? = null,
    val openedAt: String? = null,
    val closedAt: String? = null,
    val isOpen: Boolean = false,
)

@Serializable
data class ProfileManufactureDto(
    val id: Long,
    val name: String = "",
    val hangars: List<ProfileHangarDto> = emptyList(),
)

@Serializable
data class ProfileHangarDto(
    val id: Long,
    val name: String = "",
)

@Serializable
data class ProfileRoleDto(
    val id: Long,
    val name: String = "",
    val description: String? = null,
    val requiresUser: Boolean = false,
)
