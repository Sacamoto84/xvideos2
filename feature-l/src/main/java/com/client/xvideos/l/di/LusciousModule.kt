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
import javax.inject.Singleton

/**
 * Hilt-модуль внедрения зависимостей для модуля `:feature-l`.
 *
 * Предоставляет синглтоны:
 * - [KDownloader]: настроенный менеджер параллельных загрузок файлов.
 * - [Repository]: репозиторий запросов к GraphQL API Luscious и кэширования.
 * - [Luscious]: корневой фасад API раздела L.
 */
@Module
@InstallIn(SingletonComponent::class)
object LusciousModule {

    /**
     * Создает экземпляр [KDownloader] для загрузки медиа-контента альбомов L.
     */
    @Singleton
    @Provides
    fun provideDownloader( @ApplicationContext context: Context ): KDownloader { return KDownloader.create(context, DownloaderConfig(false)) }

    /**
     * Предоставляет репозиторий сетевых запросов и кэша [Repository].
     */
    @Singleton
    @Provides
    fun provideRepository(
        db: AppFileDatabase
    ): Repository {
        return Repository(db)
    }

    /**
     * Предоставляет фасад доступа к API Luscious [Luscious].
     */
    @Singleton
    @Provides
    fun provideLuscious(
        repository: Repository,
        @ApplicationScope scope: CoroutineScope,
    ): Luscious {
        return Luscious(scope, repository)
    }

}
