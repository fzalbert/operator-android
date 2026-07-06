package ru.profikrol.operator.domain.service

import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.repository.AuthError
import ru.profikrol.operator.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) {

    val isLoggedIn: Boolean get() = sessionStore.isLoggedIn
    val currentUser: User? get() = sessionStore.currentUser
    val user = sessionStore.user

    suspend fun login(login: String, password: String): Result<User> {
        val result = authRepository.login(login, password)

        result.onSuccess(sessionStore::save)

        return result
    }

    suspend fun refreshSavedSession(): Result<User> {
        val currentUser = sessionStore.currentUser ?: return Result.failure(AuthError.InvalidCredentials)
        val refreshToken = currentUser.refreshToken ?: currentUser.token

        val result = authRepository.refresh(refreshToken)

        result
            .onSuccess { token ->
                sessionStore.save(currentUser.copy(token = token))
            }
            .onFailure { error ->
                if (error == AuthError.InvalidCredentials) {
                    logout()
                }
            }

        return result.map { token -> currentUser.copy(token = token) }
    }

    fun logout() {
        sessionStore.clear()
    }
}
