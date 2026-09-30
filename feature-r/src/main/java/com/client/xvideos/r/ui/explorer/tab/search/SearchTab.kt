package com.client.xvideos.r.ui.explorer.tab.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.ui.explorer.tab.search.molecule.SearchCreatorsList
import com.client.xvideos.r.ui.explorer.tab.search.molecule.SearchTopBar
import com.client.xvideos.r.ui.profile.ScreenRedProfile

object SearchTab : Screen {

    private fun readResolve(): Any = SearchTab

    override val key: ScreenKey = "RedSearchTab"

    @Composable
    override fun Content() {
        val vm: ScreenRedExplorerSearchSM = getScreenModel()
        val navigator = LocalNavigator.currentOrThrow

        val searchText by vm.searchText.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()

        val onSearchTextChange: (String) -> Unit = remember(vm) { { vm.updateSearchText(it) } }
        val onCreatorClick: (String) -> Unit = remember(navigator) {
            { handle -> navigator.push(ScreenRedProfile(handle)) }
        }

        SearchTabContent(
            searchText = searchText,
            isLoading = isLoading,
            onSearchTextChange = onSearchTextChange,
            creatorsList = vm.creatorsList,
            onCreatorClick = onCreatorClick
        )
    }
}

@Composable
fun SearchTabContent(
    searchText: String,
    isLoading: Boolean,
    onSearchTextChange: (String) -> Unit,
    creatorsList: List<SearchItemCreatorsResponse>,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    BackHandler(enabled = searchText.isNotEmpty()) {
        onSearchTextChange("")
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SearchTopBar(
                searchText = searchText,
                isLoading = isLoading,
                onSearchTextChange = onSearchTextChange
            )
        }
    ) { paddingValues ->
        SearchCreatorsList(
            creatorsList = creatorsList,
            searchText = searchText,
            isLoading = isLoading,
            onCreatorClick = onCreatorClick,
            listState = listState,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Preview(backgroundColor = 0xFF303030)
@Composable
private fun SearchTabPreview() {
    SearchTabContent(
        searchText = "Ana",
        isLoading = false,
        onSearchTextChange = {},
        creatorsList = listOf(
            SearchItemCreatorsResponse(
                name = "Ana",
                image = null,
                followers = 1234
            ),
            SearchItemCreatorsResponse(
                name = "Elf Sandi",
                image = "https://userpic.redgifs.com/5/3f/53f9367f4b1d523a032f5fa2475de70d.png",
                followers = 274
            )
        ),
        onCreatorClick = {}
    )
}
