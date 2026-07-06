package ru.profikrol.operator.data.repository

import android.util.Log
import retrofit2.HttpException
import ru.profikrol.operator.data.auth.AuthApi
import ru.profikrol.operator.data.auth.LoginRequest
import ru.profikrol.operator.data.auth.RefreshRequest
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
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> {
        return runAuthRequest(
            requestName = "Login",
            failureLog = "login=$login",
        ) {
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

            Log.d(TAG, "Login request succeeded: login=$login, tokenLength=${token.length}")
            user
        }
    }

    override suspend fun refresh(refreshToken: String): Result<String> {
        return runAuthRequest(
            requestName = "Refresh",
            failureLog = null,
        ) {
            Log.d(TAG, "Refresh request started")

            val token = authApi.refresh(RefreshRequest(refreshToken)).string()

            Log.d(TAG, "Refresh request succeeded: tokenLength=${token.length}")
            token
        }
    }

    private suspend fun <T> runAuthRequest(
        requestName: String,
        failureLog: String?,
        block: suspend () -> T,
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            Log.d(TAG, "$requestName request cancelled${failureLog.suffix()}")
            throw e
        } catch (e: HttpException) {
            Log.w(TAG, "$requestName request failed with HTTP ${e.code()}${failureLog.suffix()}")
            when (e.code()) {
                401, 403 -> Result.failure(AuthError.InvalidCredentials)
                else -> Result.failure(AuthError.Unknown)
            }
        } catch (e: IOException) {
            Log.w(TAG, "$requestName request failed with network error${failureLog.suffix()}", e)
            Result.failure(AuthError.Network)
        } catch (e: Throwable) {
            Log.e(TAG, "$requestName request failed with unexpected error${failureLog.suffix()}", e)
            Result.failure(AuthError.Unknown)
        }
    }

    private fun String?.suffix(): String =
        this?.let { ": $it" }.orEmpty()

    private companion object {
        const val TAG = "AuthRepositoryImpl"
    }
}
