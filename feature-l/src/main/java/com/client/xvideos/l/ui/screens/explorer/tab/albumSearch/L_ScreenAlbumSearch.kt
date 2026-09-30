package com.client.xvideos.l.ui.screens.explorer.tab.albumSearch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.molecule.AlbumSearchInputField
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.molecule.AlbumSearchSectionBlock
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList

object L_ScreenAlbumSearch : Screen {

    override val key: ScreenKey = "L_ScreenAlbumSearch"

    private fun readResolve(): Any = L_ScreenAlbumSearch

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLAlbumSearchSM = getScreenModel()

        val searchText by vm.searchText.collectAsStateWithLifecycle()
        val onClearSearch: () -> Unit = remember(vm) { { vm.clearSearchText() } }
        BackHandler(enabled = searchText.isNotEmpty(), onBack = onClearSearch)
        val result by vm.result.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp

        val onSearchTextChange: (String) -> Unit = remember(vm) { { vm.updateSearchText(it) } }
        val onSearch: () -> Unit = remember(vm) { { vm.search() } }
        val onAlbumClick: (Long) -> Unit = remember(navigator) {
            { albumId -> navigator.push(ScreenLAlbum(albumId)) }
        }
        val onSeeAllClick: (Landing_page_albumSection) -> Unit = remember(vm, navigator) {
            { section ->
                navigator.push(
                    L_ScreenAlbumList.create(
                        filter = vm.createFilter(section),
                        title = "Search: ${vm.searchText.value}"
                    )
                )
            }
        }

        L_ScreenAlbumSearchContent(
            searchText = searchText,
            isLoading = isLoading,
            result = result,
            state = vm.state,
            screenWidth = screenWidth,
            onSearchTextChange = onSearchTextChange,
            onSearch = onSearch,
            onAlbumClick = onAlbumClick,
            onSeeAllClick = onSeeAllClick
        )
    }
}

@Composable
fun L_ScreenAlbumSearchContent(
    searchText: String,
    isLoading: Boolean,
    result: Landing_page_albumType?,
    state: LazyListState,
    screenWidth: Dp,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: (Landing_page_albumSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = result?.sections
    val title = result?.title

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            item(key = "search_input", contentType = "search_input") {
                AlbumSearchInputField(
                    searchText = searchText,
                    onSearchTextChange = onSearchTextChange,
                    onSearch = onSearch
                )
            }

            if (title != null) {
                item(key = "search_title", contentType = "search_title") {
                    Text(
                        title,
                        color = Theme.L.textColor,
                        fontSize = 32.sp,
                        fontFamily = Theme.L.fontFamilyKarla,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            if (isLoading) {
                item(key = "search_loading", contentType = "search_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }

            if (!isLoading && hasNoSearchResults(result)) {
                item(key = "search_empty", contentType = "search_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ничего не найдено",
                            color = Theme.L.textColor,
                            fontSize = 18.sp,
                            fontFamily = Theme.L.fontFamilyKarla
                        )
                    }
                }
            }

            items(
                items = sections.orEmpty(),
                key = { section -> section.title },
                contentType = { "search_section" }
            ) { section ->
                AlbumSearchSectionBlock(
                    section = section,
                    screenWidth = screenWidth,
                    onAlbumClick = onAlbumClick,
                    onSeeAllClick = onSeeAllClick
                )
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(Modifier.height(64.dp))
            }
        }
    }
}

@Preview
@Composable
private fun L_ScreenAlbumSearchContentPreview() {
    L_ScreenAlbumSearchContent(
        searchText = "",
        isLoading = false,
        result = null,
        state = rememberLazyListState(),
        screenWidth = 360.dp,
        onSearchTextChange = {},
        onSearch = {},
        onAlbumClick = {},
        onSeeAllClick = {}
    )
}
