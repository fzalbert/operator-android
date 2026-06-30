package ru.profikrol.operator.data.auth

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {

    @Headers("Accept: text/plain")
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ResponseBody

    @Headers("Accept: text/plain")
    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ResponseBody
}
