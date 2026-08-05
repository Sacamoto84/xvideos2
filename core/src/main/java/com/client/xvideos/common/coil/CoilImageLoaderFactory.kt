package com.client.xvideos.common.coil

import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowHardware
import com.client.xvideos.common.settings.Settings
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File


data class CoilProgressItem(
    val url : String,
    val bytes: Long,
    val total : Long,
    val done : Boolean
)


object CoilImageLoaderFactory {

    const val MIN_RAM_CACHE_PERCENT = 1
    const val MAX_RAM_CACHE_PERCENT = 50
    const val MIN_DISK_CACHE_SIZE_MB = 50
    const val MAX_DISK_CACHE_SIZE_MB = 2_000

    private const val DEFAULT_RAM_CACHE_PERCENT = 10
    private const val DEFAULT_DISK_CACHE_SIZE_MB = 500
    private const val BYTES_IN_MB = 1024L * 1024L
    private const val HTTP_CACHE_DIR_NAME = "http_cache"
    private const val IMAGE_CACHE_DIR_NAME = "image_cache"

    @Volatile
    private var instance: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: createImageLoader(context).also { instance = it }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun createImageLoader(context: Context): ImageLoader {
        val appContext = context.applicationContext
        val diskCacheEnabled = Settings.image_cache_disk_enabled.field.value
        val diskCacheMaxBytes = normalizedDiskCacheSizeMb().toLong() * BYTES_IN_MB
        val ramCachePercent = normalizedRamCachePercent() / 100.0

        val okHttpBuilder = OkHttpClient.Builder()
            .apply {
                if (diskCacheEnabled) {
                    cache(
                        okhttp3.Cache(
                            directory = httpCacheDir(appContext),
                            maxSize = diskCacheMaxBytes
                        )
                    )
                }
            }
            .addNetworkInterceptor(
                ProgressInterceptor { requestUrl, bytes, total, done ->
                    CoilProgressManager.updateProgress(
                        url = requestUrl,
                        bytes = bytes,
                        total = total.coerceAtLeast(0L),
                        done = done
                    )
                }
            )

        // Совместимость со старыми корневыми сертификатами обеспечивается через
        // res/xml/network_security_config.xml (ISRG Root X1). Глобальное trust-all
        // отключение проверки TLS удалено как небезопасное.

        // Try to build OkHttpClient safely. In LayoutLib (Compose Preview), building OkHttpClient
        // can fail with NoClassDefFoundError for Android-specific classes like conscrypt.
        val okHttpClient = try {
            okHttpBuilder.build()
        } catch (e: Throwable) {
            Timber.e(e, "Failed to build custom OkHttpClient (likely in Preview)")
            null
        }

        return ImageLoader.Builder(appContext)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                // Use custom OkHttpClient only if it was built successfully.
                // If not, Coil will fallback to its default network fetcher.
                okHttpClient?.let {
                    add(OkHttpNetworkFetcherFactory(callFactory = { it }))
                }
            }
            .apply {
                if (diskCacheEnabled) {
                    diskCache {
                        DiskCache.Builder()
                            .directory(imageCacheDir(appContext))
                            .maxSizeBytes(diskCacheMaxBytes)
                            .build()
                    }
                } else {
                    diskCache(null)
                }
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(appContext, ramCachePercent)
                    .strongReferencesEnabled(false)
                    .build()
            }
            .allowHardware(true)
            // .logger(DebugLogger()) // включи при отладке
            .build()
    }

    fun normalizedRamCachePercent(value: Int = Settings.image_cache_ram_percent.field.value): Int {
        return value.coerceIn(MIN_RAM_CACHE_PERCENT, MAX_RAM_CACHE_PERCENT)
            .takeIf { it > 0 }
            ?: DEFAULT_RAM_CACHE_PERCENT
    }

    fun normalizedDiskCacheSizeMb(value: Int = Settings.image_cache_disk_size_mb.field.value): Int {
        return value.coerceIn(MIN_DISK_CACHE_SIZE_MB, MAX_DISK_CACHE_SIZE_MB)
            .takeIf { it > 0 }
            ?: DEFAULT_DISK_CACHE_SIZE_MB
    }

    fun imageDiskCacheSizeBytes(context: Context): Long {
        val appContext = context.applicationContext
        return directorySizeBytes(imageCacheDir(appContext)) + directorySizeBytes(httpCacheDir(appContext))
    }

    fun recreate(context: Context) {
        synchronized(this) {
            instance?.memoryCache?.clear()
            instance = createImageLoader(context.applicationContext)
        }
    }

    fun clearCache(context: Context) {
        val appContext = context.applicationContext
        getImageLoader(appContext).apply {
            memoryCache?.clear()
            diskCache?.clear()
        }
        clearDirectory(imageCacheDir(appContext))
        clearDirectory(httpCacheDir(appContext))
        recreate(appContext)
    }

    private fun imageCacheDir(context: Context): File = File(context.cacheDir, IMAGE_CACHE_DIR_NAME)

    private fun httpCacheDir(context: Context): File = File(context.cacheDir, HTTP_CACHE_DIR_NAME)

    private fun directorySizeBytes(dir: File): Long {
        return dir.listFiles()?.sumOf { file ->
            if (file.isFile) file.length() else directorySizeBytes(file)
        } ?: 0L
    }

    private fun clearDirectory(dir: File) {
        if (!dir.exists()) return
        runCatching { dir.deleteRecursively() }
            .onFailure { Timber.w(it, "Failed to delete image cache dir: ${dir.absolutePath}") }
    }
}
