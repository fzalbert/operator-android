package ru.profikrol.operator.data.remote.profile

import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface ProfileApi {

    @GET("api/v1/profile/me")
    suspend fun getMyProfile(): ProfileDto
}

@Serializable
data class ProfileDto(
    val employeeId: String,
    val name: String = "",
    val surname: String = "",
    val secondName: String = "",
    val phoneNumber: String? = null,
    val email: String? = null,
    val contactNumber: String? = null,
    val address: String? = null,
    val roles: List<ProfileRoleDto> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ProfileRoleDto(
    val id: Long,
    val name: String = "",
    val description: String? = null,
    val requiresUser: Boolean = false,
)