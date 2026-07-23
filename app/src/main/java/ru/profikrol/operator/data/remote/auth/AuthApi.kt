package ru.profikrol.operator.data.remote.auth

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {

    @Headers(
        "accept: text/plain",
        "Content-Type: application/json",
    )
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>
}

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
)
