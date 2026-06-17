package ru.profikrol.operator.feature.rabbitculling

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RabbitCullingModule {

    @Binds
    @Singleton
    abstract fun bindRabbitCullingService(
        impl: RabbitCullingServiceImpl
    ): RabbitCullingService
}