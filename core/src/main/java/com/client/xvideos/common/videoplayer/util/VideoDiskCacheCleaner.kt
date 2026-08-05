package com.client.xvideos.common.videoplayer.util

import android.content.Context
import java.io.File

object VideoDiskCacheCleaner {
    private val legacyCacheDirs = listOf("video", "video_cache")
    private val legacyCacheDatabases = listOf("media3_cache.db", "exoplayer_internal.db")

    fun clearLegacyCaches(context: Context) {
        val appContext = context.applicationContext

        legacyCacheDirs.forEach { dirName ->
            File(appContext.cacheDir, dirName).deleteRecursively()
        }

        legacyCacheDatabases.forEach { databaseName ->
            appContext.deleteDatabase(databaseName)
        }
    }
}
