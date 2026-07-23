package ru.profikrol.operator.data.remote.auth

import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("api/v1/auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<TokenDto>>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("api/v1/auth/refresh")
    fun refresh(@Body request: RefreshTokenRequest): Call<ApiResponse<TokenDto>>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("api/v1/auth/logout")
    fun logout(@Body request: RefreshTokenRequest): Call<ApiResponse<Boolean>>
}

@Serializable
data class LoginRequest(val login: String, val password: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class TokenDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: String? = null,
    val refreshTokenExpiresAt: String? = null,
)

@Serializable
data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
    val title: String? = null,
    val errors: Map<String, List<String>>? = null,
)
