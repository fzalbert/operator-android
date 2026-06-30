package ru.profikrol.operator.domain.model

data class User(
    val id: String,
    val login: String,
    val displayName: String,
    val token: String,
    val refreshToken: String? = null,
    val role: UserRole,
    val email: String? = null,
    val phone: String? = null,
)
