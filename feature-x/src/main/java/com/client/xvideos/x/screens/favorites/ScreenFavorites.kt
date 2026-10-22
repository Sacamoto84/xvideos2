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
import com.client.xvideos.x.screens.favorites.molecule.FavoritesContent
import com.client.xvideos.x.screens.common.rememberLocalVideoOf
import com.client.xvideos.x.screens.common.playerScreenFor

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
    // Множество id скачанных видео (реактивно) — для обложки в диалоге удаления.
    val downloadedIds by vm.saved.downloads.downloadedVideoIds.collectAsStateWithLifecycle()

    FavoritesContent(
        favorites = vm.favorites,
        // Скачанные: превью и плеер — из файла.
        localVideoOf = rememberLocalVideoOf(vm.saved.downloads),
        posterUrlOf = remember(downloadedIds, vm.saved) {
            { item ->
                if (item.id in downloadedIds) (vm.saved.downloads.localPosterPath(item.id) ?: item.previewImage)
                else item.previewImage
            }
        },
        onDelete = remember(vm) { { item -> vm.removeFavorite(item) } },
        onDownload = remember(vm) { { item -> vm.download(item) } },
        onSaveToGallery = remember(vm) { { item -> vm.saveToGallery(item) } },
        onOpenVideo = remember(navigator, vm) {
            { item -> navigator.push(playerScreenFor(item, vm.saved.downloads.localVideo(item.id))) }
        },
        gridState = vm.gridPosition.rememberState(),
        modifier = modifier,
    )
}
