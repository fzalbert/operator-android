package ru.profikrol.operator.feature.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.profikrol.operator.R
import ru.profikrol.operator.domain.repository.AuthError
import javax.inject.Inject

class AuthErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun messageFor(error: Throwable): String =
        when (error) {
            AuthError.InvalidCredentials -> context.getString(R.string.auth_error_invalid_credentials)
            AuthError.Network -> context.getString(R.string.auth_error_network)
            else -> context.getString(R.string.auth_error_unknown)
        }
}
