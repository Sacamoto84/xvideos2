package com.client.xvideos.n.ui.screens.explorer.tab.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.client.xvideos.n.ui.molecule.NGalleryPager
import com.client.xvideos.n.ui.screens.explorer.tab.search.atom.NSearchBar
import com.client.xvideos.n.ui.screens.explorer.tab.search.molecule.NAlbumFilterDialog
import com.client.xvideos.n.ui.screens.explorer.tab.search.molecule.NTagConditionChips
import com.client.xvideos.n.ui.screens.explorer.tab.search.molecule.NTagSuggestionsRow
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Вкладка поиска манги раздела nhentai с автодополнением тегов.
 */
object N_ScreenGallerySearch : Screen {

    private fun readResolve(): Any = N_ScreenGallerySearch

    override val key: ScreenKey = "N_ScreenGallerySearch"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_ScreenGallerySearchSM>()

        val tagSuggestions by vm.tagSuggestions.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()
        val filter by vm.filter.collectAsStateWithLifecycle()
        var showFilter by remember { mutableStateOf(false) }

        Column(Modifier.fillMaxSize().background(Theme.background)) {
            NSearchBar(vm.query, vm::updateQuery, vm::search, topInset = getTopInsetDp())
            NTagSuggestionsRow(suggestions = tagSuggestions, onTagClick = vm::selectTagSuggestion)
            NTagConditionChips(filter.tags, onRemove = vm::removeTagCondition, modifier = Modifier.padding(4.dp))
            NGalleryPager(
                pages = vm.pages,
                columns = columns,
                onGalleryClick = { id -> navigator.push(ScreenNGallery(id)) },
                emptyMessage = if (vm.hasQuery) "Ничего не найдено" else "Введите запрос для поиска",
                onFilterClick = { showFilter = true },
                hasActiveFilter = filter.isFiltered,
                allowChallenge = !showFilter
            )
        }
        if (showFilter) {
            NAlbumFilterDialog(
                initialFilter = filter,
                onDismiss = { showFilter = false },
                loadTagPage = vm::loadTagPage,
                findTags = vm::findTags,
                onApply = { filter ->
                    vm.setFilter(filter)
                    showFilter = false
                }
            )
        }
    }
}
