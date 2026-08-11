package ru.profikrol.operator.data.remote.profile

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST

interface ProfileApi {

    @GET("api/v1/profile/me")
    suspend fun getMyProfile(): ProfileDto

    @POST("api/v1/profile/me/shift/open")
    suspend fun openShift(): ShiftDto

    @POST("api/v1/profile/me/shift/close")
    suspend fun closeShift(): ShiftDto
}

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
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ShiftDto(
    val id: Long = 0,
    val openedAt: String? = null,
    val closedAt: String? = null,
    val isOpen: Boolean = false,
)

@Serializable
data class ProfileRoleDto(
    val id: Long,
    val name: String = "",
    val description: String? = null,
    val requiresUser: Boolean = false,
)
