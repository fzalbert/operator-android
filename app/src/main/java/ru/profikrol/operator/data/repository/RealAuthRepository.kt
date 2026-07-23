package ru.profikrol.operator.data.repository

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.data.remote.auth.ApiResponse
import ru.profikrol.operator.data.remote.auth.AuthApi
import ru.profikrol.operator.data.remote.auth.LoginRequest
import ru.profikrol.operator.data.remote.auth.RefreshTokenRequest
import ru.profikrol.operator.data.remote.auth.TokenDto
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.domain.repository.AuthError
import ru.profikrol.operator.domain.repository.AuthRepository
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class RealAuthRepository @Inject constructor(
    @Named("authless") private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val json: Json,
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            val trimmedLogin = login.trim()
            try {
                val response = authApi.login(LoginRequest(trimmedLogin, password)).execute()
                Log.d(TAG, "Login response. code=${response.code()} success=${response.isSuccessful}")

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        if (response.code() in listOf(400, 401, 403)) {
                            AuthError.InvalidCredentials
                        } else {
                            AuthError.Network
                        },
                    )
                }

                val envelope = response.body()
                val tokens = envelope?.data
                if (!envelope.isSuccessfulEnvelope() || !tokens.isValid()) {
                    Log.w(TAG, "Login returned 2xx without a valid token pair")
                    return@withContext Result.failure(
                        if (!envelope?.error.isNullOrBlank()) AuthError.InvalidCredentials
                        else AuthError.InvalidToken,
                    )
                }
                val validTokens = requireNotNull(tokens)

                val accessToken = validTokens.accessToken.orEmpty()
                val claims = parseJwtClaims(accessToken)
                val displayName = claims.stringClaim("unique_name")
                    .ifBlank { claims.stringClaim("name") }
                    .ifBlank { claims.stringClaim("preferred_username") }
                val roleText = claims.stringClaim("role")
                    .ifBlank { claims.stringClaim("roles") }
                    .ifBlank { claims.stringClaim("http://schemas.microsoft.com/ws/2008/06/identity/claims/role") }
                    .ifBlank { displayName }
                    .ifBlank { trimmedLogin }

                val user = User(
                    id = claims.stringClaim("userId")
                        .ifBlank { claims.stringClaim("sub") }
                        .ifBlank { UUID.randomUUID().toString() },
                    login = trimmedLogin,
                    displayName = displayName.ifBlank { trimmedLogin.replaceFirstChar { it.uppercase() } },
                    token = accessToken,
                    refreshToken = validTokens.refreshToken.orEmpty(),
                    accessTokenExpiresAt = validTokens.expiresAt,
                    refreshTokenExpiresAt = validTokens.refreshTokenExpiresAt,
                    role = roleText.toUserRole(),
                )
                sessionStore.save(user)
                Result.success(user)
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                Log.e(TAG, "Login network error", error)
                Result.failure(AuthError.Network)
            } catch (error: Throwable) {
                Log.e(TAG, "Login unexpected error", error)
                Result.failure(AuthError.Unknown)
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        val refreshToken = sessionStore.currentUser?.refreshToken.orEmpty()
        try {
            if (refreshToken.isNotBlank()) {
                authApi.logout(RefreshTokenRequest(refreshToken)).execute()
            }
            Result.success(Unit)
        } catch (error: IOException) {
            Result.failure(AuthError.Network)
        } finally {
            sessionStore.clear()
        }
    }

    private fun parseJwtClaims(accessToken: String): Map<String, JsonElement> = runCatching {
        val payload = accessToken.split(".").getOrNull(1).orEmpty()
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            .toString(Charsets.UTF_8)
        json.parseToJsonElement(decoded).jsonObject
    }.getOrElse {
        Log.w(TAG, "JWT claims parse failed: ${it.message}")
        emptyMap()
    }

    private fun Map<String, JsonElement>.stringClaim(key: String): String {
        val value = this[key] ?: return ""
        return when (value) {
            is JsonArray -> value.firstOrNull()?.jsonPrimitive?.content.orEmpty()
            else -> value.jsonPrimitive.content
        }
    }

    private fun String.toUserRole(): UserRole {
        val normalized = trim().lowercase()
        return when {
            listOf("super", "admin", "root", "админ").any(normalized::contains) -> UserRole.SuperAdmin
            listOf("technologist", "technology", "технолог").any(normalized::contains) ||
                normalized.startsWith("tech") || normalized.startsWith("тех") -> UserRole.Technologist
            else -> UserRole.Operator
        }
    }

    private fun ApiResponse<TokenDto>?.isSuccessfulEnvelope(): Boolean =
        this != null && error.isNullOrBlank() && errors.isNullOrEmpty() && data != null

    private fun TokenDto?.isValid(): Boolean =
        this != null &&
            accessToken.orEmpty().looksLikeJwt() &&
            !refreshToken.isNullOrBlank()

    private fun String.looksLikeJwt(): Boolean =
        count { it == '.' } == 2 && split('.').all { it.isNotBlank() }

    private companion object {
        const val TAG = "RabbitAuth"
    }
}
