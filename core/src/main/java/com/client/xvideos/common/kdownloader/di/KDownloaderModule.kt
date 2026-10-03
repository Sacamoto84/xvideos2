package com.client.xvideos.common.kdownloader.di

import android.content.Context
import com.client.xvideos.common.kdownloader.DownloaderConfig
import com.client.xvideos.common.kdownloader.KDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль загрузчика файлов.
 *
 * Провайдер раньше лежал в DI-модуле раздела L, хотя из графа Hilt загрузчик
 * получает только раздел R: R собирался, пока в сборке есть модуль L, а
 * настройки его загрузчика лежали в чужом модуле. Загрузчик — общий класс
 * `:core`, отсюда он и предоставляется.
 */
@Module
@InstallIn(SingletonComponent::class)
object KDownloaderModule {

    /**
     * Загрузчик без базы докачки: загрузка, прерванная вместе с процессом,
     * начинается заново.
     */
    @Singleton
    @Provides
    fun provideDownloader(@ApplicationContext context: Context): KDownloader =
        KDownloader.create(context, DownloaderConfig(false))
}
