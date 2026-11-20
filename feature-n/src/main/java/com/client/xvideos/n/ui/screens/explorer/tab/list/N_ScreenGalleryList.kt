package com.client.xvideos.n.ui.screens.explorer.tab.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.ui.molecule.NGalleryPager
import com.client.xvideos.n.ui.screens.explorer.tab.list.atom.NFeedTopBar
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

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

        Column(Modifier.fillMaxSize().background(Theme.background)) {
            NFeedTopBar(
                selectedSort = sort,
                onSortSelected = vm::setSortType,
                topInset = getTopInsetDp()
            )
            NGalleryPager(vm.pages, columns, onGalleryClick = { id -> navigator.push(ScreenNGallery(id)) })
        }
    }
}
