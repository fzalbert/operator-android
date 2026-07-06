package ru.profikrol.operator.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    Operator,
    Technologist,
}
