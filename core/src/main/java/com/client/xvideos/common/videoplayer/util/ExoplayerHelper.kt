package com.client.xvideos.common.videoplayer.util

import android.content.Context
import android.media.MediaDrm
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.drm.UnsupportedDrmException
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.client.xvideos.common.videoplayer.host.DrmConfig
import com.client.xvideos.common.videoplayer.net.VideoHttpDataSource

/**
 * Проверяет, указывает ли URL на HLS-поток (.m3u8).
 * Корректно обрабатывает URL с query-параметрами (например, ?token=...&expires=...),
 * фрагментами (#...) и использует [Util.inferContentType] из Media3.
 */
@OptIn(UnstableApi::class)
fun isHlsUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val trimmed = url.trim()
    if (trimmed.endsWith(".m3u8", ignoreCase = true)) return true
    val cleanUrl = trimmed.substringBefore('?').substringBefore('#').trim()
    if (cleanUrl.endsWith(".m3u8", ignoreCase = true)) {
        return true
    }
    return runCatching {
        val uri = android.net.Uri.parse(trimmed)
        Util.inferContentType(uri) == C.CONTENT_TYPE_HLS
    }.getOrDefault(false)
}

@OptIn(UnstableApi::class)
fun createHlsMediaSource(mediaItem: MediaItem, headers: Map<String, String>?): MediaSource {
    val dataSourceFactory = VideoHttpDataSource.factory(headers)
    return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
}

@OptIn(UnstableApi::class)
fun createProgressiveMediaSource(
    mediaItem: MediaItem,
    context: Context,
    headers: Map<String, String>?
): MediaSource {
    val httpDataSourceFactory = VideoHttpDataSource.factory(headers)
    return ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context, httpDataSourceFactory))
        .createMediaSource(mediaItem)
}

@OptIn(UnstableApi::class)
fun createHlsMediaSourceWithDrm(
    mediaItem: MediaItem,
    headers: Map<String, String>?,
    drmConfig: DrmConfig
): MediaSource {
    val dataSourceFactory = VideoHttpDataSource.factory(headers)

    val drmSessionManager = try {
        DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID) { FrameworkMediaDrm.newInstance(C.CLEARKEY_UUID) }
            .build(LocalMediaDrmCallback(VideoUtils.createDrmJson(drmConfig)))
    } catch (e: UnsupportedDrmException) {
        throw RuntimeException("Unsupported DRM scheme: ${e.message}", e)
    } catch (e: MediaDrm.MediaDrmStateException) {
        throw RuntimeException("DRM state issue: ${e.message}", e)
    } catch (e: Exception) {
        throw RuntimeException("Failed to create DRM session manager: ${e.message}", e)
    }

    return HlsMediaSource.Factory(dataSourceFactory)
        .setDrmSessionManagerProvider { drmSessionManager }
        .createMediaSource(mediaItem)
}
