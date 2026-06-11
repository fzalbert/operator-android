package ru.profikrol.operator.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.profikrol.operator.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хранилище текущей сессии.
 * Сейчас — в памяти. Позже легко заменить на DataStore (диск + шифрование токена),
 * сохранив API.
 *
 * Доступ из любого места — через DI: @Inject constructor(... sessionStore: SessionStore).
 */
@Singleton
class SessionStore @Inject constructor() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    /** Удобный синхронный геттер, если стрим не нужен. */
    val currentUser: User? get() = _user.value

    val isLoggedIn: Boolean get() = _user.value != null

    fun save(user: User) {
        _user.value = user
    }

    fun clear() {
        _user.value = null
    }
}
