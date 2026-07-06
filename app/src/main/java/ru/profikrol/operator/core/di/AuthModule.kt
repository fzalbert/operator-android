package ru.profikrol.operator.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.profikrol.operator.data.repository.AuthRepositoryImpl
import ru.profikrol.operator.domain.repository.AuthRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    /**
     * @Binds сообщает Hilt: когда кто-то просит AuthRepository,
     * нужно подставить реальную реализацию авторизации.
    */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
