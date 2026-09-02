package ru.profikrol.operator.data.local

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/**
 * Хранилище текущей сессии.
 * Доступ из любого места — через DI: @Inject constructor(... sessionStore: SessionStore).
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _user = MutableStateFlow(readUser())
    val user: StateFlow<User?> = _user.asStateFlow()

    /** Удобный синхронный геттер, если стрим не нужен. */
    val currentUser: User? get() = _user.value

    val isLoggedIn: Boolean get() = _user.value != null

    fun save(user: User) {
        preferences.edit()
            .putString(KEY_ID, user.id)
            .putString(KEY_LOGIN, user.login)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putString(KEY_TOKEN, user.token)
            .putString(KEY_REFRESH_TOKEN, user.refreshToken)
            .putString(KEY_ACCESS_TOKEN_EXPIRES_AT, user.accessTokenExpiresAt)
            .putString(KEY_REFRESH_TOKEN_EXPIRES_AT, user.refreshTokenExpiresAt)
            .putString(KEY_ROLE, user.role.name)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone)
            .apply()
        _user.value = user
    }

    @Synchronized
    fun updateTokens(
        accessToken: String,
        refreshToken: String,
        accessTokenExpiresAt: String?,
        refreshTokenExpiresAt: String?,
    ) {
        val current = _user.value ?: return
        save(
            current.copy(
                id = accessToken.employeeIdClaim().orEmpty().ifBlank { current.id },
                token = accessToken,
                refreshToken = refreshToken,
                accessTokenExpiresAt = accessTokenExpiresAt,
                refreshTokenExpiresAt = refreshTokenExpiresAt,
            ),
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
        _user.value = null
    }

    private fun readUser(): User? {
        val id = preferences.getString(KEY_ID, null) ?: return null
        val login = preferences.getString(KEY_LOGIN, null) ?: return null
        val displayName = preferences.getString(KEY_DISPLAY_NAME, null) ?: return null
        val token = preferences.getString(KEY_TOKEN, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        if (!token.looksLikeJwt() || refreshToken.isBlank()) return null
        val roleName = preferences.getString(KEY_ROLE, null) ?: return null
        val role = runCatching { UserRole.valueOf(roleName) }.getOrNull() ?: return null

        return User(
            id = token.employeeIdClaim().orEmpty().ifBlank { id },
            login = login,
            displayName = displayName,
            token = token,
            refreshToken = refreshToken,
            accessTokenExpiresAt = preferences.getString(KEY_ACCESS_TOKEN_EXPIRES_AT, null),
            refreshTokenExpiresAt = preferences.getString(KEY_REFRESH_TOKEN_EXPIRES_AT, null),
            role = role,
            email = preferences.getString(KEY_EMAIL, null),
            phone = preferences.getString(KEY_PHONE, null),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "operator_session"
        const val KEY_ID = "id"
        const val KEY_LOGIN = "login"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_TOKEN = "token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at"
        const val KEY_REFRESH_TOKEN_EXPIRES_AT = "refresh_token_expires_at"
        const val KEY_ROLE = "role"
        const val KEY_EMAIL = "email"
        const val KEY_PHONE = "phone"
    }
}

private fun String.looksLikeJwt(): Boolean =
    isNotBlank() && count { it == '.' } == 2 && split('.').all { it.isNotBlank() }

private fun String.employeeIdClaim(): String? = runCatching {
    val payload = split('.').getOrNull(1).orEmpty()
    val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        .toString(Charsets.UTF_8)
    JSONObject(decoded).optString("employeeId")
        .ifBlank { JSONObject(decoded).optString("employee_id") }
        .takeIf(String::isNotBlank)
}.getOrNull()
