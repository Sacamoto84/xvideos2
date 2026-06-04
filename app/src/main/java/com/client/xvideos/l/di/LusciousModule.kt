package com.client.xvideos.l.di

import android.content.Context
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.kdownloader.DownloaderConfig
import com.client.xvideos.common.kdownloader.KDownloader
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.repository.Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LusciousModule {

    @Singleton
    @Provides
    fun provideDownloader( @ApplicationContext context: Context ): KDownloader { return KDownloader.create(context, DownloaderConfig(false)) }

    @Singleton
    @Provides
    fun provideRepository(
        db: AppFileDatabase, @ApplicationScope scope: CoroutineScope, @ApplicationContext context: Context
    ): Repository {
        return Repository( db, scope, context )
    }

    @Singleton
    @Provides
    fun provideLuscious(
        repository: Repository
    ): Luscious {
        val downloadDispatcher = Executors.newFixedThreadPool(8).asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + downloadDispatcher)
        return Luscious(scope, repository)
    }

}
