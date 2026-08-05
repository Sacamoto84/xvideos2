package com.client.xvideos.x.screens.videoplayerFullScreen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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

        if (vm.passedString == "") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
            error = {},
            selectedQuality = null,
            seekBackIncrementMs = 10_000L,    // Перемотка назад на ±10 сек
            seekForwardIncrementMs = 10_000L, // Перемотка вперёд на ±10 сек
        )

        // Без звука + старт с переданной позиции, когда медиа готово.
        DisposableEffect(exo) {
            exo.volume = 0f
            val listener = object : Player.Listener {
                private var seeked = false
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY && !seeked && position > 0L) {
                        exo.seekTo(position)
                        seeked = true
                    }
                }
            }
            exo.addListener(listener)
            onDispose { exo.removeListener(listener) }
        }

        // Альбомная ориентация + immersive на время полноэкранного режима.
        DisposableEffect(Unit) {
            val activity = context.findActivityOrNull()
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

        fun exit() {
            EventBus.postEvent(Event.X_FullScreenExitPosition(exo.currentPosition))
            navigator.pop()
        }

        BackHandler { exit() }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = true
                    setShowRewindButton(true)
                    setShowFastForwardButton(true)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setFullscreenButtonClickListener { exit() }
                }
            },
            modifier = Modifier.fillMaxSize(),
            // PlayerView создаётся здесь и держит ссылку на exo (а плеер — на view).
            // Без отвязки view переживает уход с экрана вместе с контекстом Activity.
            onRelease = { it.player = null }
        )
    }
}

private fun Context.findActivityOrNull(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
