package com.client.xvideos.common.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/** Доступ к [ApplicationScope] из composable вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApplicationScopeEntryPoint {
    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
