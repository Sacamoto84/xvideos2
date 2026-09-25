package com.client.xvideos.x.screens.videoplayer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

private val PLAYER_BG_COLOR = Color(0xFF040404)
private val RESUME_PILL_BOTTOM_PADDING = 76.dp
private const val RESUME_NOTICE_AUTO_HIDE_MS = 4000L
private const val PROGRESS_SAVE_INTERVAL_MS = 3000L
private const val RESUME_NOTICE_PREFIX = "Возобновлено с "

private val ALIGN_BOTTOM_CENTER = Alignment.BottomCenter
private val ENTER_FADE_TRANSITION = fadeIn()
private val EXIT_FADE_TRANSITION = fadeOut()

private val FULL_SIZE_MODIFIER = Modifier.fillMaxSize()
private val CONTAINER_MODIFIER = Modifier.fillMaxSize().background(PLAYER_BG_COLOR)

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
        val downloadsList by sm.saved.downloads.list.collectAsStateWithLifecycle()

        val videoId = item?.id?.takeIf { it > 0L }
            ?: Uri.parse(fileUrl).lastPathSegment?.substringBefore('.')?.toLongOrNull()
            ?: 0L

        val resolvedItem = remember(item, videoId, downloadsList) {
            item
                ?: downloadsList.find { it.id == videoId }
                ?: ItemsX(id = videoId)
        }

        val historyItem = remember(videoId) { if (videoId > 0L) sm.saved.history.get(videoId) else null }
        val resumePosition = remember(historyItem) {
            historyItem?.takeIf { it.isEligibleForResume }?.let {
                (it.lastPositionMs / 1000f).takeIf { sec -> sec.isFinite() && sec >= 0f }
            }
        }

        var resumeNoticeText by remember(fileUrl) {
            mutableStateOf(
                resumePosition?.let { sec ->
                    "$RESUME_NOTICE_PREFIX${formatTime(sec.toInt())}"
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
                delay(RESUME_NOTICE_AUTO_HIDE_MS)
                resumeNoticeText = null
            }
        }

        // Периодическое сохранение прогресса во время активного воспроизведения
        LaunchedEffect(host.isPaused) {
            if (!host.isPaused) {
                saveProgress(sm, resolvedItem, host.currentTime, host.totalTime)
                while (isActive) {
                    delay(PROGRESS_SAVE_INTERVAL_MS)
                    saveProgress(sm, resolvedItem, host.currentTime, host.totalTime)
                }
            }
        }

        // Финальное сохранение при закрытии экрана
        DisposableEffect(host, sm, resolvedItem) {
            onDispose {
                saveProgress(sm, resolvedItem, host.currentTime, host.totalTime)
            }
        }

        var isZoomed by remember { mutableStateOf(false) }
        var resetZoomTrigger by remember { mutableIntStateOf(0) }

        val onResetZoom: () -> Unit = remember { { resetZoomTrigger++ } }
        val onPopScreen: () -> Unit = remember(navigator) { { navigator.pop() } }

        // Нажатие кнопки «Назад» при зуме сбрасывает масштаб, иначе выходит из плеера
        BackHandler(enabled = isZoomed, onBack = onResetZoom)
        BackHandler(enabled = !isZoomed, onBack = onPopScreen)

        val onZoomChanged: (Boolean) -> Unit = remember { { isZoomed = it } }
        val onTap: () -> Unit = remember(host) { { host.togglePlayPause() } }
        val onRestartPlayback: () -> Unit = remember(host) {
            {
                host.seekTo(0f)
                resumeNoticeText = null
            }
        }

        Box(modifier = CONTAINER_MODIFIER) {
            ComposeVideoPlayer(
                playerHost = host,
                modifier = FULL_SIZE_MODIFIER,
                resetZoomTrigger = resetZoomTrigger,
                onZoomChanged = onZoomChanged,
                onTap = onTap,
                overlay = {
                    // Плашка возобновления
                    AnimatedVisibility(
                        visible = resumeNoticeText != null,
                        enter = ENTER_FADE_TRANSITION,
                        exit = EXIT_FADE_TRANSITION,
                        modifier = Modifier
                            .align(ALIGN_BOTTOM_CENTER)
                            .padding(bottom = RESUME_PILL_BOTTOM_PADDING)
                    ) {
                        resumeNoticeText?.let { notice ->
                            ResumePlaybackPill(
                                text = notice,
                                onRestart = onRestartPlayback
                            )
                        }
                    }

                    Box(modifier = Modifier.align(ALIGN_BOTTOM_CENTER)) {
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
