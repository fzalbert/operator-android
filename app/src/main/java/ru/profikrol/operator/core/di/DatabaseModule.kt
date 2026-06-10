package ru.profikrol.operator.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Здесь будут провайды Room DB и DAO.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
