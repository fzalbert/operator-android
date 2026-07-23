package ru.profikrol.operator.domain.model

data class User(
    val id: String,
    val login: String,
    val displayName: String,
    val token: String,
    val refreshToken: String = "",
    val accessTokenExpiresAt: String? = null,
    val refreshTokenExpiresAt: String? = null,
    val role: UserRole,
    val email: String? = null,
    val phone: String? = null,
)
