package ru.profikrol.operator.feature.nestpreparation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NestPreparationModule {

    @Binds
    @Singleton
    abstract fun bindNestPreparationService(
        impl: NestPreparationServiceImpl,
    ): NestPreparationService
}
