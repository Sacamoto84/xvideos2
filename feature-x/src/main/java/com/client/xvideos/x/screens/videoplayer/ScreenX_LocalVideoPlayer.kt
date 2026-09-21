package com.client.xvideos.x.screens.videoplayer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.client.xvideos.x.screens.videoplayer.atom.formatTime
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.videoplayer.atom.ResumePlaybackPill
import com.client.xvideos.x.screens.videoplayer.atom.X_PlayerBottomBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Плеер локального (скачанного) файла X.
 *
 * Играет напрямую `file://`-URI скачанного mp4 — без резолва HTML/HLS, без сети.
 * Видео X всегда без звука (как и стриминговый X-плеер).
 * Сохраняет прогресс и поддерживает возобновление («Продолжить просмотр»).
 *
 * @param fileUrl `file://`-URI локального mp4 (см. `SavedX_Downloads.localUrl`).
 * @param item опциональные метаданные ролика (если известны из вызывающего экрана).
 */
class ScreenX_LocalVideoPlayer(
    val fileUrl: String,
    val item: ItemsX? = null,
) : Screen {

    override val key: ScreenKey = "ScreenX_LocalVideoPlayer:$fileUrl"

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm: ScreenX_LocalVideoPlayerSM = getScreenModel()

        val videoId = item?.id?.takeIf { it > 0L }
            ?: Uri.parse(fileUrl).lastPathSegment?.substringBefore('.')?.toLongOrNull()
            ?: 0L

        val resolvedItem = item
            ?: sm.saved.downloads.list.value.find { it.id == videoId }
            ?: ItemsX(id = videoId)

        val historyItem = if (videoId > 0L) sm.saved.history.get(videoId) else null
        val resumePosition = historyItem?.takeIf { it.isEligibleForResume }?.let {
            (it.lastPositionMs / 1000f).takeIf { sec -> sec.isFinite() && sec >= 0f }
        }

        var resumeNoticeText by remember(fileUrl) {
            mutableStateOf(
                resumePosition?.let { sec ->
                    "Возобновлено с ${formatTime(sec.toInt())}"
                }
            )
        }

        val host = remember(fileUrl) {
            MediaPlayerHost(
                mediaUrl = fileUrl,
                isMuted = true, // видео X всегда без звука
                isLooping = false,
                startTimeInSeconds = resumePosition,
            )
        }

        // Авто-скрытие плашки о возобновлении через 4 секунды
        LaunchedEffect(resumeNoticeText) {
            if (resumeNoticeText != null) {
                delay(4000)
                resumeNoticeText = null
            }
        }

        // Периодическое сохранение прогресса раз в 5 секунд во время воспроизведения
        LaunchedEffect(host.isPaused) {
            if (!host.isPaused) {
                while (isActive) {
                    delay(5000)
                    saveProgress(sm, resolvedItem, host.currentTime, host.totalTime)
                }
            }
        }

        // Финальное сохранение при закрытии экрана
        DisposableEffect(Unit) {
            onDispose {
                saveProgress(sm, resolvedItem, host.currentTime, host.totalTime)
            }
        }

        var isZoomed by remember { mutableStateOf(false) }
        var resetZoomTrigger by remember { mutableIntStateOf(0) }

        // Нажатие кнопки «Назад» при зуме сбрасывает масштаб, иначе выходит из плеера
        BackHandler(enabled = isZoomed) {
            resetZoomTrigger++
        }
        BackHandler(enabled = !isZoomed) {
            navigator.pop()
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040404))) {
            ComposeVideoPlayer(
                playerHost = host,
                modifier = Modifier.fillMaxSize(),
                resetZoomTrigger = resetZoomTrigger,
                onZoomChanged = { isZoomed = it },
                onTap = { host.togglePlayPause() },
                overlay = {
                    IconButton(
                        onClick = {
                            if (isZoomed) {
                                resetZoomTrigger++
                            } else {
                                navigator.pop()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.only(
                                    WindowInsetsSides.Top + WindowInsetsSides.Start
                                )
                            )
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                        )
                    }

                    // Плашка возобновления
                    AnimatedVisibility(
                        visible = resumeNoticeText != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 76.dp)
                    ) {
                        resumeNoticeText?.let { notice ->
                            ResumePlaybackPill(
                                text = notice,
                                onRestart = {
                                    host.seekTo(0f)
                                    resumeNoticeText = null
                                }
                            )
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        // Локальный файл — отдельный полноэкранный режим не требуется
                        X_PlayerBottomBar(host = host)
                    }
                }
            )
        }
    }

    private fun saveProgress(
        sm: ScreenX_LocalVideoPlayerSM,
        item: ItemsX,
        currentTimeSeconds: Float,
        totalTimeSeconds: Int,
    ) {
        val playerDurationMs = totalTimeSeconds.coerceAtLeast(0) * 1000L
        val parsedDurationMs = com.client.xvideos.x.parseDurationToMs(item.duration)
        val durationMs = if (playerDurationMs > 0L) playerDurationMs else parsedDurationMs
        val safeSeconds = currentTimeSeconds.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val maxPos = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        val positionMs = (safeSeconds * 1000f).toLong().coerceIn(0L, maxPos)
        if (item.id > 0L) {
            sm.saved.history.updateProgress(item, positionMs, durationMs)
        }
    }
}
