package ru.profikrol.operator.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Здесь будут провайды OkHttp, Retrofit, API-интерфейсов.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule
