package ru.profikrol.operator.domain.repository

import ru.profikrol.operator.domain.model.User

interface AuthRepository {

    /**
     * Авторизация по логину и паролю.
     * Возвращает Result.success(User) при успехе или Result.failure(AuthError) при ошибке.
     */
    suspend fun login(login: String, password: String): Result<User>

    suspend fun refreshSession(): Result<User>
}

/** Доменные ошибки авторизации. */
sealed class AuthError(message: String) : Throwable(message) {
    data object InvalidCredentials : AuthError("Invalid credentials")
    data object Network : AuthError("Network error")
    data object Unknown : AuthError("Unknown error")
}
