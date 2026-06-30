package ru.profikrol.operator.data.repository

import android.util.Log
import retrofit2.HttpException
import ru.profikrol.operator.data.auth.AuthApi
import ru.profikrol.operator.data.auth.LoginRequest
import ru.profikrol.operator.data.auth.RefreshRequest
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.domain.repository.AuthError
import ru.profikrol.operator.domain.repository.AuthRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "Login request started: login=$login")

            val token = authApi.login(
                LoginRequest(
                    login = login,
                    password = password,
                ),
            ).string()

            val user = User(
                id = login,
                login = login,
                displayName = login.replaceFirstChar { it.uppercase() },
                token = token,
                refreshToken = token,
                role = UserRole.Operator,
            )

            sessionStore.save(user)
            Log.d(TAG, "Login request succeeded: login=$login, tokenLength=${token.length}")
            Result.success(user)
        } catch (e: CancellationException) {
            Log.d(TAG, "Login request cancelled: login=$login")
            throw e
        } catch (e: HttpException) {
            Log.w(TAG, "Login request failed with HTTP ${e.code()}: login=$login")
            when (e.code()) {
                401, 403 -> Result.failure(AuthError.InvalidCredentials)
                else -> Result.failure(AuthError.Unknown)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Login request failed with network error: login=$login", e)
            Result.failure(AuthError.Network)
        } catch (e: Throwable) {
            Log.e(TAG, "Login request failed with unexpected error: login=$login", e)
            Result.failure(AuthError.Unknown)
        }
    }

    override suspend fun refreshSession(): Result<User> {
        val currentUser = sessionStore.currentUser ?: return Result.failure(AuthError.InvalidCredentials)
        val refreshToken = currentUser.refreshToken ?: currentUser.token

        return try {
            Log.d(TAG, "Refresh request started: login=${currentUser.login}")

            val token = authApi.refresh(RefreshRequest(refreshToken)).string()
            val refreshedUser = currentUser.copy(token = token)

            sessionStore.save(refreshedUser)
            Log.d(
                TAG,
                "Refresh request succeeded: login=${currentUser.login}, tokenLength=${token.length}",
            )
            Result.success(refreshedUser)
        } catch (e: CancellationException) {
            Log.d(TAG, "Refresh request cancelled: login=${currentUser.login}")
            throw e
        } catch (e: HttpException) {
            Log.w(TAG, "Refresh request failed with HTTP ${e.code()}: login=${currentUser.login}")
            when (e.code()) {
                401, 403 -> Result.failure(AuthError.InvalidCredentials)
                else -> Result.failure(AuthError.Unknown)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Refresh request failed with network error: login=${currentUser.login}", e)
            Result.failure(AuthError.Network)
        } catch (e: Throwable) {
            Log.e(TAG, "Refresh request failed with unexpected error: login=${currentUser.login}", e)
            Result.failure(AuthError.Unknown)
        }
    }

    private companion object {
        const val TAG = "AuthRepositoryImpl"
    }
}
