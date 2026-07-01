package ru.profikrol.operator.core.network

import kotlinx.coroutines.flow.Flow

interface NetworkStatusProvider {
    fun isOnline(): Flow<Boolean>
}