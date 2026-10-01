package com.client.xvideos.x.screens.videoplayer

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.videoplayer.molecule.LocalVideoPlayerContent
import com.client.xvideos.x.screens.videoplayer.molecule.VideoPlayerLoadingView

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

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sm: ScreenX_LocalVideoPlayerSM = getScreenModel()

        ScreenX_LocalVideoPlayerContent(
            fileUrl = fileUrl,
            item = item,
            sm = sm,
            onPopBack = { navigator.pop() },
        )
    }
}

/**
 * Корневая функция компоновки экрана локального плеера.
 * Разрешает метаданные видео, сохранённую позицию и делегирует отображение молекуле [LocalVideoPlayerContent].
 */
@Composable
fun ScreenX_LocalVideoPlayerContent(
    fileUrl: String,
    item: ItemsX?,
    sm: ScreenX_LocalVideoPlayerSM,
    onPopBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val downloadsList by sm.saved.downloads.list.collectAsStateWithLifecycle()

    val videoId = item?.id?.takeIf { it > 0L }
        ?: Uri.parse(fileUrl).lastPathSegment?.substringBefore('.')?.toLongOrNull()
        ?: 0L

    val resolvedItem = remember(item, videoId, downloadsList) {
        item
            ?: downloadsList.find { it.id == videoId }
            ?: ItemsX(id = videoId)
    }

    // Плеер берёт позицию старта только при создании, поэтому создаётся после чтения
    // истории; `null` — история ещё читается (с диска, если записи нет в памяти).
    val startSeconds by produceState<Float?>(initialValue = null, videoId) {
        value = sm.startPositionSeconds(videoId)
    }

    val onSaveProgress: (Float, Int) -> Unit = remember(sm, resolvedItem) {
        { currentTimeSeconds, totalTimeSeconds ->
            sm.saveProgress(resolvedItem, currentTimeSeconds, totalTimeSeconds)
        }
    }

    val start = startSeconds
    if (start == null) {
        VideoPlayerLoadingView(modifier = modifier)
    } else {
        LocalVideoPlayerContent(
            fileUrl = fileUrl,
            resumePosition = start.takeIf { it > 0f },
            onSaveProgress = onSaveProgress,
            onPopBack = onPopBack,
            modifier = modifier
        )
    }
}
