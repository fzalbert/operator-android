package ru.profikrol.operator.data.repository

import kotlinx.coroutines.delay
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.domain.repository.AuthError
import ru.profikrol.operator.domain.repository.AuthRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Заглушка для авторизации. Имитирует сетевой запрос с задержкой.
 *
 * Правила демо:
 *  - логин начинается с "tech" → роль Technologist
 *  - всё остальное непустое → роль Operator
 *  - логин == "fail" → ошибка InvalidCredentials (для проверки UI ошибки)
 */
@Singleton
class FakeAuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> {
        return try {
            // Имитация сетевой задержки.
            delay(1200)

            if (login.equals("fail", ignoreCase = true)) {
                return Result.failure(AuthError.InvalidCredentials)
            }

            val role = if (login.startsWith("tech", ignoreCase = true)) {
                UserRole.Technologist
            } else {
                UserRole.Operator
            }

            val user = User(
                id = UUID.randomUUID().toString(),
                login = login,
                displayName = login.replaceFirstChar { it.uppercase() },
                token = "fake-token-${UUID.randomUUID()}",
                role = role,
                email = "$login@profikrol.ru",
                phone = "+7 (916) 123-45-67",
            )

            sessionStore.save(user)
            Result.success(user)
        } catch (e: CancellationException) {
            // Корутины должны пробрасывать отмену дальше.
            throw e
        } catch (e: Throwable) {
            Result.failure(AuthError.Unknown)
        }
    }
}
