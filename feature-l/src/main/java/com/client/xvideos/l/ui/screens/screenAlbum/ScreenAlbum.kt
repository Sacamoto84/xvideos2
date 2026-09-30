package com.client.xvideos.l.ui.screens.screenAlbum

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.navigation.rememberNavigationDepth
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.net.AlbumInfo
import com.client.xvideos.l.ui.screens.screenAlbum.dialog.AlbumDialogDeleteAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.molecule.ScreenLAlbumBody
import com.client.xvideos.l.ui.screens.screenAlbum.molecule.ScreenLAlbumErrorHeader

class ScreenLAlbum(val idAlbum: Long) : Screen {

    override val key: ScreenKey = "LAlbum:$idAlbum"

    @Composable
    override fun Content() {

        rememberNavigationDepth().depth = 100

        val navigator = LocalNavigator.currentOrThrow

        val vm = getScreenModel<ScreenLAlbumSM, ScreenLAlbumSM.Factory> { factory -> factory.create(idAlbum) }

        /**  ➜ сюда запоминаем элемент, который пользователь хочет удалить  */
        var itemPendingDelete by remember { mutableStateOf<AlbumDetails?>(null) }

        val onDismissDelete: () -> Unit = remember { { itemPendingDelete = null } }
        val onDismissAnimatedFilter: () -> Unit = remember(vm) { { vm.showOnlyAnimated = false } }
        val onPopScreen: () -> Unit = remember(navigator) { { navigator.pop() } }
        val onRequestDelete: (AlbumDetails) -> Unit = remember { { itemPendingDelete = it } }

        // Активен только когда НЕ открыта полноэкранная картинка — в этом случае
        // back перехватывает L_FullScreenImage (закрывает картинку), и выход из альбома не происходит.
        BackHandler(enabled = vm.host.selectedImage == null && itemPendingDelete != null, onBack = onDismissDelete)
        BackHandler(enabled = vm.host.selectedImage == null && itemPendingDelete == null && vm.showOnlyAnimated, onBack = onDismissAnimatedFilter)
        BackHandler(enabled = vm.host.selectedImage == null && itemPendingDelete == null && !vm.showOnlyAnimated, onBack = onPopScreen)

        val topInset = getTopInsetDp()
        val album by vm.albumInfo.collectAsStateWithLifecycle()

        /* ---------- Диалог подтверждения ---------- */
        itemPendingDelete?.let { pending ->
            val onConfirmDelete: () -> Unit = remember(pending, vm) {
                {
                    vm.saved.albums.remove(pending)
                    itemPendingDelete = null
                }
            }
            AlbumDialogDeleteAlbum(
                pending = pending,
                onDismiss = onDismissDelete,
                onClick = onConfirmDelete
            )
        }
        /* ---------- /Диалог ---------- */

        vm.p2pAlbumSource?.let { source ->
            // Навигация — side effect, нельзя звать прямо из композиции.
            LaunchedEffect(source) {
                navigator.push(ScreenP2pSend(source))
                vm.dismissP2pAlbum()
            }
        }

        ScreenLAlbumContent(
            album = album,
            vm = vm,
            navigator = navigator,
            topInset = topInset,
            idAlbum = idAlbum,
            onRequestDelete = onRequestDelete
        )
    }
}

@Composable
fun ScreenLAlbumContent(
    album: AlbumInfo?,
    vm: ScreenLAlbumSM,
    navigator: Navigator,
    topInset: Dp,
    idAlbum: Long,
    onRequestDelete: (AlbumDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    if (album != null) {
        ScreenLAlbumBody(
            album = album,
            vm = vm,
            navigator = navigator,
            topInset = topInset,
            idAlbum = idAlbum,
            onRequestDelete = onRequestDelete
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = topInset),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Theme.L.red)
        }
    }
}

@Preview
@Composable
private fun ScreenLAlbumPreview() {
    ScreenLAlbumErrorHeader(
        loadError = "Example Album Preview",
        onRetry = {},
        onBack = {}
    )
}
