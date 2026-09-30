package com.client.xvideos.x.screens.favorites

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.favorites.molecule.FavoritesContent
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer

class ScreenFavorites : Screen {

    override val key: ScreenKey = "ScreenFavorites"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenFavoritesSM = getScreenModel()

        ScreenFavoritesContent(
            vm = vm,
            navigator = navigator
        )
    }
}

/**
 * Корневая функция компоновки экрана «Избранное».
 */
@Composable
fun ScreenFavoritesContent(
    vm: ScreenFavoritesSM,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    // Множество id скачанных видео (реактивно) — для значка и локального воспроизведения.
    val downloadedIds by vm.saved.downloads.downloadedVideoIds.collectAsStateWithLifecycle()

    FavoritesContent(
        favorites = vm.favorites,
        localUrlOf = remember(downloadedIds, vm.saved) {
            { item ->
                if (item.id in downloadedIds) vm.saved.downloads.localUrl(item.id) else null
            }
        },
        posterUrlOf = remember(downloadedIds, vm.saved) {
            { item ->
                if (item.id in downloadedIds) (vm.saved.downloads.localPosterPath(item.id) ?: item.previewImage)
                else item.previewImage
            }
        },
        onDelete = remember(vm) { { item -> vm.removeFavorite(item) } },
        onDownload = remember(vm) { { item -> vm.download(item) } },
        onSaveToGallery = remember(vm) { { item -> vm.saveToGallery(item) } },
        onPlayLocal = remember(navigator) { { url, item -> navigator.push(ScreenX_LocalVideoPlayer(url, item)) } },
        onOpenVideo = remember(navigator) { { item -> navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item)) } },
        modifier = modifier,
    )
}
