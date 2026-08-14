package com.client.xvideos.common.videoplayer.feed

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Дисковый кеш предзагрузки видео.
 *
 * `SimpleCache` держит блокировку на своей папке, поэтому экземпляр в процессе
 * ровно один и живёт до смерти процесса: закрывать его вместе с экраном нельзя —
 * следующий экран не сможет открыть ту же папку.
 */
@OptIn(UnstableApi::class)
object FeedVideoCache {

    private const val DIR_NAME = "video_precache"
    private const val MAX_BYTES = 256L * 1024 * 1024

    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context): Cache {
        instance?.let { return it }
        return synchronized(this) {
            instance ?: SimpleCache(
                File(context.applicationContext.cacheDir, DIR_NAME),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context.applicationContext),
            ).also { instance = it }
        }
    }
}
