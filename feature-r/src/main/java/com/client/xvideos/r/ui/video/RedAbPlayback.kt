package com.client.xvideos.r.ui.video

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource

@OptIn(UnstableApi::class, ExperimentalApi::class)
internal class RedAbPlayback(
    private val player: ExoPlayer,
    private val mediaItem: MediaItem,
    private val originalSource: () -> MediaSource,
) {
    private data class Range(val startMs: Long, val endMs: Long)

    private val localSourceFactory = mediaItem.localConfiguration?.uri
        ?.takeIf { it.scheme == "file" || (it.scheme == null && it.path?.startsWith('/') == true) }
        ?.let { ProgressiveMediaSource.Factory(FileDataSource.Factory()) }
    private var requestedRange: Range? = null
    private var playingClip: Range? = null
    private var fullDurationMs = C.TIME_UNSET

    val positionMs: Long get() = player.currentPosition + (playingClip?.startMs ?: 0L)
    val durationMs: Long
        get() {
            if (playingClip == null && player.duration > 0L) fullDurationMs = player.duration
            return fullDurationMs
        }

    fun setRange(enabled: Boolean, timeA: Float, timeB: Float) {
        val range = if (isValidABRange(enabled, timeA, timeB)) {
            Range((timeA * 1000).toLong(), (timeB * 1000).toLong())
        } else {
            null
        }
        if (range == requestedRange) return
        requestedRange = range

        when {
            range == null -> {
                if (playingClip != null) restoreFullVideo(positionMs)
            }
            !startClip(range) -> {
                if (playingClip != null) restoreFullVideo(range.startMs) else seekTo(range.startMs)
            }
        }
    }

    fun repeatIfNeeded(positionMs: Long) {
        val range = requestedRange ?: return
        if (playingClip != null || positionMs < range.endMs) return
        if (!startClip(range)) player.seekTo(range.startMs)
    }

    fun seekTo(positionMs: Long) {
        val targetMs = clampSeekPositionMs(positionMs, durationMs)
        val clip = playingClip
        when {
            clip == null -> player.seekTo(targetMs)
            targetMs >= clip.startMs && targetMs < clip.endMs -> player.seekTo(targetMs - clip.startMs)
            // Полоса по-прежнему показывает весь ролик. Если пользователь вышел за границы
            // петли, проигрываем исходник до B, затем снова включаем повтор отрезка.
            else -> restoreFullVideo(targetMs)
        }
    }

    private fun startClip(range: Range): Boolean {
        val factory = localSourceFactory ?: return false
        if (range.endMs <= range.startMs || range.endMs > durationMs) return false
        if (playingClip == null && !player.isCurrentMediaItemSeekable) return false

        // REPEAT_MODE_ONE заранее ставит следующий проход в очередь. На стыке B → A
        // не посылаем seekTo из UI и не ждём следующего тика полосы времени.
        val source = ClippingMediaSource.Builder(factory.createMediaSource(mediaItem))
            .setStartPositionMs(range.startMs)
            .setEndPositionMs(range.endMs)
            .setEnableClippingInMediaPeriod(true)
            .build()
        playingClip = range
        replaceSource(source, 0L)
        return true
    }

    private fun restoreFullVideo(positionMs: Long) {
        playingClip = null
        replaceSource(originalSource(), positionMs)
    }

    private fun replaceSource(source: MediaSource, positionMs: Long) {
        player.setMediaSource(source, positionMs)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
    }
}
