package com.client.xvideos.n.ui.screens.explorer.tab.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.ui.molecule.NGalleryPager
import com.client.xvideos.n.ui.molecule.NSortMenuButton
import com.client.xvideos.n.ui.screens.explorer.tab.list.atom.NFeedSortIcon
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery
import kotlinx.collections.immutable.toImmutableList

/**
 * Вкладка ленты свежей манги раздела nhentai.
 */
object N_ScreenGalleryList : Screen {

    private fun readResolve(): Any = N_ScreenGalleryList

    override val key: ScreenKey = "N_ScreenGalleryList"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_ScreenGalleryListSM>()

        val columns by vm.columns.collectAsStateWithLifecycle()
        val sort by vm.sortType.collectAsStateWithLifecycle()

        NGalleryPager(
            pages = vm.pages,
            columns = columns,
            onGalleryClick = { id -> navigator.push(ScreenNGallery(id)) },
            gridTopInset = getTopInsetDp(),
            leadingContent = {
                NSortMenuButton(
                    options = remember { NSortType.entries.map { it.title }.toImmutableList() },
                    selectedIndex = sort.ordinal,
                    onSelectIndex = { vm.setSortType(NSortType.entries[it]) },
                    contentDescription = "Сортировка",
                    modeIcon = { index -> NFeedSortIcon(NSortType.entries[index]) }
                )
            }
        )
    }
}
