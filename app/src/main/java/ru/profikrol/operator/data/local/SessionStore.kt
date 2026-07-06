package ru.profikrol.operator.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.profikrol.operator.domain.model.User
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
    private val json: Json,
) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _user = MutableStateFlow(readUser())
    val user: StateFlow<User?> = _user.asStateFlow()

    /** Удобный синхронный геттер, если стрим не нужен. */
    val currentUser: User? get() = _user.value

    val isLoggedIn: Boolean get() = _user.value != null

    fun save(user: User) {
        preferences.edit {
            putString(KEY_USER, json.encodeToString(user))
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
        val savedUser = preferences.getString(KEY_USER, null) ?: return null

        return try {
            json.decodeFromString<User>(savedUser)
        } catch (e: SerializationException) {
            preferences.edit { clear() }
            null
        } catch (e: IllegalArgumentException) {
            preferences.edit { clear() }
            null
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "session"
        const val KEY_USER = "user"
    }
}
