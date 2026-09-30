package com.client.xvideos.x.screens.videoplayer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.screens.videoplayer.atom.OrientationAndSystemBarsEffect
import com.client.xvideos.x.screens.videoplayer.molecule.VideoPlayerContentView
import com.client.xvideos.x.screens.videoplayer.molecule.VideoPlayerErrorView
import com.client.xvideos.x.screens.videoplayer.molecule.VideoPlayerLoadingView

/**
 * Экран видеоплеера (Voyager Screen).
 * Управляет жизненным циклом ScreenModel, системной ориентацией экрана
 * и переключением между состояниями: ошибка, загрузка и контент плеера.
 *
 * @param url URL страницы или видеопотока.
 * @param item Опциональные метаданные видео (ItemsX).
 */
class ScreenX_VideoPlayer(
    val url: String,
    val item: ItemsX? = null,
) : Screen {

    override val key: ScreenKey = "ScreenX_VideoPlayer:$url"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Инициализация ScreenModel с фабрикой параметров
        val vm = getScreenModel<ScreenX_VideoPlayerSM, ScreenX_VideoPlayerSM.Factory> { factory ->
            factory.create(url, item)
        }

        // Управление ориентацией устройства и системными панелями (status bar, navigation bar)
        OrientationAndSystemBarsEffect(vm.isFullScreen)

        val onRetryLoad: () -> Unit = remember(vm) { { vm.loadVideo(forceReload = true) } }
        val onPopBack: () -> Unit = remember(navigator) { { navigator.pop() } }

        // Нажатие кнопки «Назад» при ошибке или загрузке закрывает экран
        BackHandler(enabled = vm.isError || vm.isLoading || vm.passedHLS.isBlank(), onBack = onPopBack)

        ScreenX_VideoPlayerContent(
            isError = vm.isError,
            isLoading = vm.isLoading || vm.passedHLS.isBlank(),
            passedHLS = vm.passedHLS,
            videoTitle = vm.currentItem.title,
            isFavorite = vm.isFavorite,
            onFavoriteAdd = { vm.addFavorite() },
            onFavoriteRemove = { vm.removeFavorite() },
            onDownload = { vm.download() },
            onSaveToGallery = { vm.saveToGallery() },
            resumePositionSeconds = vm.resumePositionSeconds,
            isFullScreen = vm.isFullScreen,
            resumeNoticeText = vm.resumeNoticeText,
            tags = vm.tags,
            onPlaybackError = { vm.onPlaybackError() },
            onExitFullScreen = { vm.exitFullScreen() },
            onPopBack = onPopBack,
            onDismissResumeNotice = { vm.dismissResumeNotice() },
            onTagClick = { tag -> vm.openTag(tag, navigator) },
            onChannelClick = { channel -> vm.openChannel(channel, navigator) },
            onPornstarClick = { pornstar -> vm.openPornstar(pornstar, navigator) },
            onRestartFromBeginning = { vm.restartFromBeginning() },
            onToggleFullScreen = { vm.toggleFullScreen() },
            onSaveProgress = { positionSeconds, durationSeconds ->
                vm.saveProgress(positionSeconds, durationSeconds)
            },
            onRetryLoad = onRetryLoad,
        )
    }
}

/**
 * Корневая функция компоновки экрана видеоплеера.
 * Собирает состояния ошибки, загрузки и основного контента.
 */
@Suppress("LongParameterList")
@Composable
fun ScreenX_VideoPlayerContent(
    isError: Boolean,
    isLoading: Boolean,
    passedHLS: String,
    resumePositionSeconds: Float?,
    isFullScreen: Boolean,
    resumeNoticeText: String?,
    tags: TagsModel,
    onPlaybackError: () -> Unit,
    onExitFullScreen: () -> Unit,
    onPopBack: () -> Unit,
    onDismissResumeNotice: () -> Unit,
    onTagClick: (String) -> Unit,
    onRestartFromBeginning: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onSaveProgress: (positionSeconds: Float, durationSeconds: Int) -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
    videoTitle: String = "",
    isFavorite: Boolean = false,
    onFavoriteAdd: () -> Unit = {},
    onFavoriteRemove: () -> Unit = {},
    onDownload: () -> Unit = {},
    onSaveToGallery: () -> Unit = {},
    onChannelClick: (TagsMainUploaderPornstar) -> Unit = {},
    onPornstarClick: (TagsMainUploaderPornstar) -> Unit = {},
) {
    when {
        isError -> {
            VideoPlayerErrorView(
                onRetry = onRetryLoad,
                onBack = onPopBack,
                modifier = modifier
            )
        }
        isLoading -> {
            VideoPlayerLoadingView(
                onBack = onPopBack,
                modifier = modifier
            )
        }
        else -> {
            VideoPlayerContentView(
                passedHLS = passedHLS,
                videoTitle = videoTitle,
                isFavorite = isFavorite,
                onFavoriteAdd = onFavoriteAdd,
                onFavoriteRemove = onFavoriteRemove,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
                resumePositionSeconds = resumePositionSeconds,
                isFullScreen = isFullScreen,
                resumeNoticeText = resumeNoticeText,
                tags = tags,
                onPlaybackError = onPlaybackError,
                onExitFullScreen = onExitFullScreen,
                onPopBack = onPopBack,
                onDismissResumeNotice = onDismissResumeNotice,
                onTagClick = onTagClick,
                onChannelClick = onChannelClick,
                onPornstarClick = onPornstarClick,
                onRestartFromBeginning = onRestartFromBeginning,
                onToggleFullScreen = onToggleFullScreen,
                onSaveProgress = onSaveProgress,
                modifier = modifier
            )
        }
    }
}
