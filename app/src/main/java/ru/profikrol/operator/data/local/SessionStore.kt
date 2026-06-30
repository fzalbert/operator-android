package ru.profikrol.operator.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.profikrol.operator.domain.model.User
import ru.profikrol.operator.domain.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранилище текущей сессии.
 * Держит пользователя в памяти и дублирует на диск через SharedPreferences,
 * чтобы сессия переживала перезапуск приложения.
 *
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
        preferences.edit {
            putString(KEY_ID, user.id)
            putString(KEY_LOGIN, user.login)
            putString(KEY_DISPLAY_NAME, user.displayName)
            putString(KEY_TOKEN, user.token)
            putString(KEY_REFRESH_TOKEN, user.refreshToken)
            putString(KEY_ROLE, user.role.name)
            putString(KEY_EMAIL, user.email)
            putString(KEY_PHONE, user.phone)
        }
        _user.value = user
    }

    fun clear() {
        preferences.edit {
            clear()
        }
        _user.value = null
    }

    private fun readUser(): User? {
        val id = preferences.getString(KEY_ID, null) ?: return null
        val login = preferences.getString(KEY_LOGIN, null) ?: return null
        val displayName = preferences.getString(KEY_DISPLAY_NAME, null) ?: return null
        val token = preferences.getString(KEY_TOKEN, null) ?: return null
        val role = preferences.getString(KEY_ROLE, null)
            ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
            ?: return null

        return User(
            id = id,
            login = login,
            displayName = displayName,
            token = token,
            refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null),
            role = role,
            email = preferences.getString(KEY_EMAIL, null),
            phone = preferences.getString(KEY_PHONE, null),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "session"
        const val KEY_ID = "id"
        const val KEY_LOGIN = "login"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_TOKEN = "token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ROLE = "role"
        const val KEY_EMAIL = "email"
        const val KEY_PHONE = "phone"
    }
}
