package com.client.xvideos.common.videoplayer.util

import android.content.Context
import android.media.MediaDrm
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.drm.UnsupportedDrmException
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.client.xvideos.common.videoplayer.host.DrmConfig

@OptIn(UnstableApi::class)
fun applyQualitySelection(
    trackSelector: DefaultTrackSelector,
    selectedQuality: VideoQuality?
) {
    trackSelector.setParameters(
        trackSelector.buildUponParameters().apply {
            selectedQuality?.let {
                setMaxVideoBitrate(it.bitrate.toInt())
                setMinVideoBitrate(it.bitrate.toInt())
            } ?: setMaxVideoBitrate(Int.MAX_VALUE)
        }
    )
}

@OptIn(UnstableApi::class)
fun applyAudioTrackSelection(trackSelector: DefaultTrackSelector, audioTrack: AudioTrack?) {
    trackSelector.setParameters(
        trackSelector.buildUponParameters()
            .setPreferredAudioLanguage(audioTrack?.language)
    )
}

@OptIn(UnstableApi::class)
fun applySubTitleTrackSelection(
    trackSelector: DefaultTrackSelector,
    subtitleTrack: SubtitleTrack?
) {
    trackSelector.setParameters(
        trackSelector.buildUponParameters().apply {
            if (subtitleTrack != null) {
                setPreferredTextLanguage(subtitleTrack.language)
                setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            } else {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                setRendererDisabled(C.TRACK_TYPE_TEXT, true)
                setPreferredTextLanguage(null)
                setSelectUndeterminedTextLanguage(false)
            }
        }
    )
}


@OptIn(UnstableApi::class)
fun createHlsMediaSource(mediaItem: MediaItem, headers: Map<String, String>?): MediaSource {
    val headersMap = headers ?: emptyMap()
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setDefaultRequestProperties(headersMap)

    return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
}

@OptIn(UnstableApi::class)
fun createProgressiveMediaSource(
    mediaItem: MediaItem,
    context: Context,
    headers: Map<String, String>?
): MediaSource {
    val headersMap = headers ?: emptyMap()
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setDefaultRequestProperties(headersMap)

    return ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context, httpDataSourceFactory))
        .createMediaSource(mediaItem)
}

@OptIn(UnstableApi::class)
fun createHlsMediaSourceWithDrm(
    mediaItem: MediaItem,
    headers: Map<String, String>?,
    drmConfig: DrmConfig
): MediaSource {
    val headersMap = headers ?: emptyMap()
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setDefaultRequestProperties(headersMap)

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
