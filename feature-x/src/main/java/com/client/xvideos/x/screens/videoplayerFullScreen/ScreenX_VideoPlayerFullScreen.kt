package com.client.xvideos.x.screens.videoplayerFullScreen

import android.content.pm.ActivityInfo
import com.client.xvideos.common.util.findActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.videoplayer.rememberExoPlayerWithLifecycle

/**
 * Полноэкранный плеер X.
 *
 * Сохраняет оригинальное поведение/вид X-плеера в полном экране:
 * - альбомная ориентация + immersive (скрытие системных баров) на время экрана;
 * - НАТИВНЫЕ media3-контролы (`PlayerView`, useController=true) — тот же стиль кнопок,
 *   что был раньше (play/pause, перемотка ±, прогрессбар, кнопка выхода из fullscreen);
 * - без звука.
 *
 * ExoPlayer создаётся общей инфраструктурой `:core-player`
 * ([rememberExoPlayerWithLifecycle]) — HLS, lifecycle и release переиспользуются.
 * Позиция возвращается обычному экрану через [EventBus].
 */
@Deprecated("Используйте ScreenX_VideoPlayer с встроенным полноэкранным режимом на едином плеере")
class ScreenX_VideoPlayerFullScreen(val url: String, val position: Long = -1L) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val vm = getScreenModel<ScreenX_VideoPlayerFullScreenSM, ScreenX_VideoPlayerFullScreenSM.Factory> { factory ->
            factory.create(url, position)
        }

        // Альбомная ориентация + immersive на время полноэкранного режима.
        DisposableEffect(Unit) {
            val activity = context.findActivity()
            val window = activity?.window
            val prevOrientation = activity?.requestedOrientation
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            // Edge-to-edge включён глобально (MainActivity) и не перенастраивается.
            // Статус-бар скрыт глобально — прячем/возвращаем только навигацию.
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView)
                    .hide(WindowInsetsCompat.Type.navigationBars())
            }
            onDispose {
                activity?.requestedOrientation =
                    prevOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window?.let {
                    val controller = WindowCompat.getInsetsController(it, it.decorView)
                    controller.show(WindowInsetsCompat.Type.navigationBars())
                    // Страховка: статус-бар обязан остаться скрытым
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
        }

        fun exit(currentExoPosition: Long = position) {
            EventBus.postEvent(Event.X_FullScreenExitPosition(currentExoPosition))
            navigator.pop()
        }

        BackHandler(enabled = vm.isError || vm.isLoading) { exit() }

        if (vm.isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Не удалось загрузить видео", color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        Button(onClick = { vm.loadVideo(forceReload = true) }) {
                            Text("Повторить")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { exit() }) {
                            Text("Назад")
                        }
                    }
                }
            }
            return
        }

        if (vm.isLoading || vm.passedString.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
            return
        }

        val exo = rememberExoPlayerWithLifecycle(
            url = vm.passedString,
            context = context,
            isPause = false,
            isLiveStream = false,
            isLooping = false,
            headers = null,
            drmConfig = null,
            error = { vm.onPlaybackError() },
            selectedQuality = null,
            seekBackIncrementMs = 10_000L,    // Перемотка назад на ±10 сек
            seekForwardIncrementMs = 10_000L, // Перемотка вперёд на ±10 сек
        )

        // Без звука + старт с переданной позиции, когда медиа готово.
        DisposableEffect(exo) {
            exo.volume = 0f
            // playWhenReady у ExoPlayer по умолчанию false, и включить его здесь
            // некому: rememberExoPlayerWithLifecycle только готовит медиа, а
            // возвращает воспроизведение лишь при ON_RESUME после ухода в фон.
            // Обычные экраны спасает CMPlayer2 — он выставляет флаг в update
            // своего AndroidView, но здесь PlayerView собран напрямую.
            // Без этой строки полный экран открывался, перематывался на
            // переданную позицию и стоял на паузе.
            exo.playWhenReady = true
            val listener = object : Player.Listener {
                private var seeked = false
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY && !seeked && position > 0L) {
                        exo.seekTo(position)
                        seeked = true
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    vm.onPlaybackError()
                }
            }
            exo.addListener(listener)
            onDispose { exo.removeListener(listener) }
        }

        fun exitWithExo() {
            val pos = exo.currentPosition
            exo.pause()
            exit(pos)
        }

        BackHandler(enabled = !vm.isError && !vm.isLoading) { exitWithExo() }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = true
                    setShowRewindButton(true)
                    setShowFastForwardButton(true)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setFullscreenButtonClickListener { exitWithExo() }
                }
            },
            modifier = Modifier.fillMaxSize(),
            // PlayerView создаётся здесь и держит ссылку на exo (а плеер — на view).
            // Без отвязки view переживает уход с экрана вместе с контекстом Activity.
            onRelease = { it.player = null }
        )
    }
}
