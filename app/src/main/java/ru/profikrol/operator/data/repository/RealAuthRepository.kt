package ru.profikrol.operator.data.repository

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.data.remote.auth.AuthApi
import ru.profikrol.operator.data.remote.auth.LoginRequest
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.domain.repository.AuthError
import ru.profikrol.operator.domain.repository.AuthRepository
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class RealAuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val json: Json,
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        val trimmedLogin = login.trim()
        Log.d(TAG, "Login submit. login=$trimmedLogin")

        try {
            val response = authApi.login(LoginRequest(login = trimmedLogin, password = password))
            val responseText = if (response.isSuccessful) {
                response.body()?.string().orEmpty()
            } else {
                response.errorBody()?.string().orEmpty()
            }

            Log.d(TAG, "Login response. code=${response.code()} success=${response.isSuccessful}")

            if (response.isSuccessful) {
                val authPayload = parseAuthPayload(responseText, trimmedLogin)
                val user = User(
                    id = authPayload.userId.ifBlank { UUID.randomUUID().toString() },
                    login = trimmedLogin,
                    displayName = authPayload.displayName.ifBlank { trimmedLogin.replaceFirstChar { it.uppercase() } },
                    token = authPayload.accessToken,
                    role = authPayload.role,
                )
                sessionStore.save(user)
                Log.i(TAG, "Login success. login=$trimmedLogin role=${user.role} tokenSaved=${user.token.isNotBlank()}")
                Result.success(user)
            } else {
                Log.w(TAG, "Login failed. login=$trimmedLogin code=${response.code()}")
                Result.failure(if (response.code() in listOf(400, 401, 403)) AuthError.InvalidCredentials else AuthError.Network)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            Log.e(TAG, "Login network error. login=$trimmedLogin", error)
            Result.failure(AuthError.Network)
        } catch (error: Throwable) {
            Log.e(TAG, "Login unexpected error. login=$trimmedLogin", error)
            Result.failure(AuthError.Unknown)
        }
    }

    private fun parseAuthPayload(responseText: String, login: String): AuthPayload {
        val accessToken = runCatching {
            json.parseToJsonElement(responseText)
                .jsonObject["data"]
                ?.jsonObject
                ?.get("accessToken")
                ?.jsonPrimitive
                ?.content
                .orEmpty()
        }.getOrElse { error ->
            Log.w(TAG, "Access token parse failed: ${error.message}")
            ""
        }

        val claims = parseJwtClaims(accessToken)
        val displayName = claims.stringClaim("unique_name")
            .ifBlank { claims.stringClaim("name") }
            .ifBlank { claims.stringClaim("preferred_username") }
        val roleText = claims.stringClaim("role")
            .ifBlank { claims.stringClaim("roles") }
            .ifBlank { claims.stringClaim("http://schemas.microsoft.com/ws/2008/06/identity/claims/role") }
            .ifBlank { displayName }
            .ifBlank { login }

        return AuthPayload(
            accessToken = accessToken,
            userId = claims.stringClaim("userId").ifBlank { claims.stringClaim("sub") },
            displayName = displayName,
            role = roleText.toUserRole(),
        )
    }

    private fun parseJwtClaims(accessToken: String): Map<String, JsonElement> = runCatching {
        val payload = accessToken.split(".").getOrNull(1).orEmpty()
        if (payload.isBlank()) return@runCatching emptyMap()

        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            .toString(Charsets.UTF_8)

        json.parseToJsonElement(decoded).jsonObject
    }.getOrElse { error ->
        Log.w(TAG, "JWT claims parse failed: ${error.message}")
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
            normalized.contains("super") ||
                normalized.contains("admin") ||
                normalized.contains("root") ||
                normalized.contains("админ") -> UserRole.SuperAdmin
            normalized.contains("technologist") ||
                normalized.contains("technology") ||
                normalized.contains("технолог") ||
                normalized.startsWith("tech") ||
                normalized.startsWith("тех") -> UserRole.Technologist
            else -> UserRole.Operator
        }
    }

    private data class AuthPayload(
        val accessToken: String,
        val userId: String,
        val displayName: String,
        val role: UserRole,
    )

    private companion object {
        const val TAG = "RabbitAuth"
    }
}
