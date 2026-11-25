package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.ui.molecule.NGalleryPager
import com.client.xvideos.n.ui.molecule.NRequestChallengeDialog
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.molecule.NServerFavoritesTopBar
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Вкладка избранного пользователя с сервера nhentai (/api/v2/favorites).
 */
object N_ScreenServerFavoritesTab : Screen {

    private fun readResolve(): Any = N_ScreenServerFavoritesTab

    override val key: ScreenKey = "N_ScreenServerFavoritesTab"

    @Composable
    override fun Content() = Content(active = true)

    @Composable
    internal fun Content(active: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenNServerFavoritesSM>()

        val isImporting by vm.isImporting.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()
        val access by vm.access.collectAsStateWithLifecycle()
        val apiKey = access.apiKey
        val localFavorites by vm.localFavorites.collectAsStateWithLifecycle()
        val removalChallenge by vm.removalChallenge.collectAsStateWithLifecycle()

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }

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
                itemMenuActions = { item, dismiss ->
                    if (localFavorites?.any { it.id == item.id } == true) {
                        ExpandMenuActionItem(
                            Icons.Filled.Bookmark,
                            "Уже сохранено локально",
                            enabled = false,
                            onClick = {}
                        )
                    } else {
                        ExpandMenuActionItem(Icons.Outlined.BookmarkBorder, "Сохранить локально") {
                            dismiss()
                            vm.importSingle(item)
                        }
                    }
                    ExpandMenuActionItem(
                        Icons.Outlined.Delete,
                        if (apiKey.isNotBlank()) "Удалить с сервера" else "Для удаления нужен API-ключ",
                        enabled = apiKey.isNotBlank()
                    ) {
                        dismiss()
                        vm.removeServerFavorite(item.id, apiKey)
                    }
                },
                allowChallenge = active && removalChallenge == null,
                showVerticalScrollbar = true,
                emptyMessage = "На сервере пока нет добавленных закладок"
            )
        }
        removalChallenge?.takeIf { active }?.let {
            NRequestChallengeDialog(
                challenge = it,
                onDismiss = { vm.finishRemovalChallenge(verified = false) },
                onVerified = { vm.finishRemovalChallenge(verified = true) }
            )
        }
    }
}
