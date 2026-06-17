package ru.profikrol.operator.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.profikrol.operator.data.nfc.AndroidNfcReader
import ru.profikrol.operator.domain.nfc.NfcReader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NfcModule {

    @Binds
    @Singleton
    abstract fun bindNfcReader(impl: AndroidNfcReader): NfcReader
}
