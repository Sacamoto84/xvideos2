package com.client.xvideos.common.videoplayer.util

import android.content.Context
import java.io.File

object VideoDiskCacheCleaner {
    private val legacyCacheDirs = arrayOf("video", "video_cache")
    private val legacyCacheDatabases = arrayOf("media3_cache.db", "exoplayer_internal.db")

    fun clearLegacyCaches(context: Context) {
        val appContext = context.applicationContext
        val cacheDir = appContext.cacheDir

        for (dirName in legacyCacheDirs) {
            File(cacheDir, dirName).deleteRecursively()
        }

        for (databaseName in legacyCacheDatabases) {
            appContext.deleteDatabase(databaseName)
        }
    }
}
