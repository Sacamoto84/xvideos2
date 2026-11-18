package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.ui.molecule.NGalleryPager
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.molecule.NServerFavoriteActionDialog
import com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.molecule.NServerFavoritesTopBar
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Вкладка избранного пользователя с сервера nhentai (/api/v2/favorites).
 */
object N_ScreenServerFavoritesTab : Screen {

    private fun readResolve(): Any = N_ScreenServerFavoritesTab

    override val key: ScreenKey = "N_ScreenServerFavoritesTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenNServerFavoritesSM>()

        val isImporting by vm.isImporting.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()

        var selectedItemForAction by remember { mutableStateOf<NGalleryPreview?>(null) }

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }
        val onItemLongClick = remember { { item: NGalleryPreview -> selectedItemForAction = item } }
        val onDismissAction = remember { { selectedItemForAction = null } }

        val pages = vm.pages
        val current = pages[pages.currentIndex] as? NGalleryListUiState.Success
        Column(Modifier.fillMaxSize().background(Theme.background)) {
            NServerFavoritesTopBar(
                itemsCount = current?.items?.size ?: 0,
                onImportAll = vm::importCurrentPage,
                isImporting = isImporting,
                onRefresh = pages::refresh,
                topInset = getTopInsetDp()
            )
            NGalleryPager(
                pages = pages,
                columns = columns,
                onGalleryClick = onGalleryClick,
                onItemLongClick = onItemLongClick,
                contentPadding = PaddingValues(4.dp),
                emptyMessage = "На сервере пока нет добавленных закладок"
            )
        }

        selectedItemForAction?.let { item ->
            val isLocallySaved = remember(item.id) { vm.isLocallySaved(item.id) }
            NServerFavoriteActionDialog(
                item = item,
                isLocallySaved = isLocallySaved,
                onDismiss = onDismissAction,
                onOpen = onGalleryClick,
                onImport = vm::importSingle,
                onRemoveFromServer = vm::removeServerFavorite
            )
        }
    }
}
