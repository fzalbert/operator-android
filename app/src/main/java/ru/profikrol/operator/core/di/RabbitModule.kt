package ru.profikrol.operator.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.profikrol.operator.data.repository.InMemoryRabbitRepository
import ru.profikrol.operator.domain.repository.RabbitRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RabbitModule {

    @Binds
    @Singleton
    abstract fun bindRabbitRepository(impl: InMemoryRabbitRepository): RabbitRepository
}
