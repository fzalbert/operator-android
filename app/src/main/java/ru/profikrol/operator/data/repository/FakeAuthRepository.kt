package ru.profikrol.operator.data.repository

import kotlinx.coroutines.delay
import ru.profikrol.operator.data.local.SessionStore
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.model.UserRole
import ru.profikrol.operator.domain.repository.AuthError
import ru.profikrol.operator.domain.repository.AuthRepository
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Заглушка для авторизации. Имитирует сетевой запрос с задержкой.
 *
 * Правила демо:
 *  - логин похож на супер-админа → роль SuperAdmin
 *  - логин похож на технолога → роль Technologist
 *  - всё остальное непустое → роль Operator
 *  - логин == "fail" → ошибка InvalidCredentials (для проверки UI ошибки)
 */
@Singleton
class FakeAuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> {
        return try {
            delay(1200)

            if (login.equals("fail", ignoreCase = true)) {
                return Result.failure(AuthError.InvalidCredentials)
            }

            val normalizedLogin = login.trim().lowercase()
            val role = when {
                normalizedLogin.startsWith("admin") ||
                    normalizedLogin.startsWith("super") ||
                    normalizedLogin.startsWith("root") ||
                    normalizedLogin.contains("админ") -> UserRole.SuperAdmin
                normalizedLogin.startsWith("c") ||
                    normalizedLogin.startsWith("technolog") ||
                    normalizedLogin.startsWith("тех") ||
                    normalizedLogin.contains("технолог") -> UserRole.Technologist
                else -> UserRole.Operator
            }

            // Local backend currently only parses the userId claim and does not validate
            // the signature. Keep this development token syntactically valid so the gRPC
            // stream can be exercised until the real authentication endpoint is connected.
            val userId = login.trim()
            val token = developmentJwt(userId)
            val user = User(
                id = userId,
                login = login,
                displayName = login.replaceFirstChar { it.uppercase() },
                token = token,
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

    private fun developmentJwt(userId: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".toByteArray())
        val escapedUserId = userId.replace("\\", "\\\\").replace("\"", "\\\"")
        val payload = encoder.encodeToString("{\"userId\":\"$escapedUserId\"}".toByteArray())
        return "$header.$payload."
    }
}
