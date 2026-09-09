package ru.profikrol.operator.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.profikrol.operator.data.local.offline.OfflineDao
import ru.profikrol.operator.data.local.offline.OfflineDatabase
import javax.inject.Singleton

/**
 * Здесь будут провайды Room DB и DAO.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideOfflineDatabase(@ApplicationContext context: Context): OfflineDatabase =
        Room.databaseBuilder(context, OfflineDatabase::class.java, "operator-offline.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideOfflineDao(database: OfflineDatabase): OfflineDao = database.offlineDao()
}
