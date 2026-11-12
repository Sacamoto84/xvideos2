package com.client.xvideos.n.di

import com.client.xvideos.n.repository.NRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль внедрения зависимостей для модуля `:feature-n`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NModule {

    @Singleton
    @Provides
    fun provideNRepository(): NRepository {
        return NRepository()
    }
}
