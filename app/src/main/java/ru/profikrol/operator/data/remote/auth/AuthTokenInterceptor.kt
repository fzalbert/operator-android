package ru.profikrol.operator.data.remote.auth

import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.profikrol.operator.data.local.SessionStore
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthTokenInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(AUTHORIZATION) != null || request.isAuthEndpoint()) {
            return chain.proceed(request)
        }

        val token = sessionStore.currentUser?.token.orEmpty()
        val authorized = if (token.isBlank()) request else {
            request.newBuilder().header(AUTHORIZATION, "Bearer $token").build()
        }
        return chain.proceed(authorized)
    }
}

@Singleton
class AccessTokenAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
    @Named("authless") private val authApi: AuthApi,
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.retryCount() >= MAX_RETRIES || response.request.isAuthEndpoint()) return null

        synchronized(refreshLock) {
            val user = sessionStore.currentUser ?: return null
            val failedToken = response.request.bearerToken()
            if (user.token.isNotBlank() && failedToken != null && user.token != failedToken) {
                return response.request.withBearer(user.token)
            }

            val refreshToken = user.refreshToken.takeIf { it.isNotBlank() } ?: return null
            val refreshResponse = runCatching {
                authApi.refresh(RefreshTokenRequest(refreshToken)).execute()
            }.getOrNull() ?: return null

            val envelope = refreshResponse.body()
            val tokens = envelope?.data
            val newAccessToken = tokens?.accessToken.orEmpty()
            val newRefreshToken = tokens?.refreshToken.orEmpty()
            val valid = refreshResponse.isSuccessful &&
                envelope?.error.isNullOrBlank() &&
                envelope?.errors.isNullOrEmpty() &&
                newAccessToken.looksLikeJwt() &&
                newRefreshToken.isNotBlank()

            if (!valid) {
                sessionStore.clear()
                return null
            }

            sessionStore.updateTokens(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                accessTokenExpiresAt = tokens?.expiresAt,
                refreshTokenExpiresAt = tokens?.refreshTokenExpiresAt,
            )
            return response.request.withBearer(newAccessToken)
        }
    }
}

private fun Request.isAuthEndpoint(): Boolean =
    url.encodedPath.startsWith("/api/v1/auth/")

private fun Request.bearerToken(): String? =
    header(AUTHORIZATION)?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }

private fun Request.withBearer(token: String): Request =
    newBuilder().header(AUTHORIZATION, "Bearer $token").build()

private fun Response.retryCount(): Int {
    var count = 1
    var current = priorResponse
    while (current != null) {
        count++
        current = current.priorResponse
    }
    return count
}

private fun String.looksLikeJwt(): Boolean =
    count { it == '.' } == 2 && split('.').all { it.isNotBlank() }

private const val AUTHORIZATION = "Authorization"
private const val MAX_RETRIES = 2
