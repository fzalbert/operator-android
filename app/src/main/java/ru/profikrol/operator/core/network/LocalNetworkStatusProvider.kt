package ru.profikrol.operator.core.network

import androidx.compose.runtime.staticCompositionLocalOf

val LocalNetworkStatusProvider = staticCompositionLocalOf<NetworkStatusProvider> {
    error("NetworkStatusProvider is not provided")
}