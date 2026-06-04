package com.client.xvideos.common

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource

/**
 * Создаёт Media3 `MediaSource` по URI и типу медиаконтента.
 *
 * ExoPlayer работает с разными источниками через разные фабрики: DASH, HLS,
 * SmoothStreaming и обычный progressive-файл. Метод определяет тип через
 * `Util.inferContentType()` и возвращает подходящий `MediaSource`, чтобы
 * вызывающий код мог не знать деталей конкретного протокола.
 *
 * @param uri адрес видео или плейлиста.
 * @param defaultHttpDataSourceFactory общая HTTP-фабрика с нужными headers/cache.
 * @param overrideExtension ручная подсказка расширения, если URI не содержит
 * явного `.m3u8`, `.mpd` или другого расширения.
 */
@OptIn(UnstableApi::class)
fun buildMediaSource(
    uri: Uri,
    defaultHttpDataSourceFactory: DefaultHttpDataSource.Factory,
    overrideExtension: String?,
): MediaSource {
    return when (val type = Util.inferContentType(uri, overrideExtension)) {
        C.CONTENT_TYPE_DASH -> DashMediaSource.Factory(defaultHttpDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))

        C.CONTENT_TYPE_SS -> SsMediaSource.Factory(defaultHttpDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))

        C.CONTENT_TYPE_HLS -> HlsMediaSource.Factory(defaultHttpDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))

        C.CONTENT_TYPE_OTHER -> ProgressiveMediaSource.Factory(defaultHttpDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))

        else -> {
            throw IllegalStateException("Unsupported type: $type")
        }
    }
}
