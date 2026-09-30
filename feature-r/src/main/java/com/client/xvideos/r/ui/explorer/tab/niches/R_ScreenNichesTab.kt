package com.client.xvideos.r.ui.explorer.tab.niches

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.explorer.tab.niches.atom.RefreshMini
import com.client.xvideos.r.ui.explorer.tab.niches.molecule.NicheItemRow
import com.client.xvideos.r.ui.explorer.tab.niches.molecule.NichesBottomBar
import com.client.xvideos.r.ui.explorer.tab.niches.molecule.Refresh
import com.client.xvideos.r.ui.niche.R_ScreenNiche
import com.client.xvideos.r.ui.search.RSearchTextField
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private data class NicheScrollSnapshot(
    val itemCount: Int,
    val index: Int,
    val offset: Int
)

object R_ScreenNichesTab : Screen {

    private fun readResolve(): Any = R_ScreenNichesTab

    override val key: ScreenKey = "R_ScreenNichesTab"

    @Composable
    override fun Content() {
        val vm: ScreenRedExplorerNichesSM = getScreenModel()
        val navigator = LocalNavigator.currentOrThrow
        val navigationState = vm.navigationState
        val coroutineScope = rememberCoroutineScope()
        val sortType by vm.sortType.collectAsStateWithLifecycle()
        val searchTextDone by vm.search.searchTextDone.collectAsStateWithLifecycle()
        val cacheVersion = vm.savedRed.nichesCache.version
        val nicheItems = remember(cacheVersion, searchTextDone, sortType) {
            filterAndSortNiches(
                niches = vm.savedRed.nichesCache.list.toList(),
                query = searchTextDone,
                order = sortType
            )
        }
        val savedIndex = navigationState.nichesFirstVisibleItemIndex
        val initialIndex = if (nicheItems.isNotEmpty()) {
            savedIndex.coerceIn(0, nicheItems.lastIndex)
        } else {
            0
        }
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = initialIndex,
            initialFirstVisibleItemScrollOffset = if (initialIndex == savedIndex) {
                navigationState.nichesFirstVisibleItemScrollOffset
            } else {
                0
            }
        )
        var lastListParams by remember { mutableStateOf(searchTextDone to sortType) }

        LaunchedEffect(searchTextDone, sortType) {
            val params = searchTextDone to sortType
            if (params != lastListParams) {
                navigationState.resetNichesScrollPosition()
                listState.scrollToItem(0)
                lastListParams = params
            }
        }

        LaunchedEffect(listState, nicheItems.size, navigationState) {
            snapshotFlow {
                NicheScrollSnapshot(
                    itemCount = nicheItems.size,
                    index = listState.firstVisibleItemIndex,
                    offset = listState.firstVisibleItemScrollOffset
                )
            }
                .distinctUntilChanged()
                .collect { snapshot ->
                    if (snapshot.itemCount > 0) {
                        navigationState.updateNichesScrollPosition(snapshot.index, snapshot.offset)
                    }
                }
        }

        val isSearchFocused by vm.search.focused.collectAsStateWithLifecycle()

        val onSortTypeChange: (Order) -> Unit = remember(vm) { { vm.changeSortType(it) } }

        val onUpClick: () -> Unit = remember(listState, coroutineScope, navigationState) {
            {
                coroutineScope.launch {
                    listState.scrollToItem(0)
                    navigationState.resetNichesScrollPosition()
                }
            }
        }

        val onNicheClick: (String) -> Unit = remember(navigator) { { id -> navigator.push(R_ScreenNiche(id)) } }
        val onRefreshNichesCacheClick: () -> Unit = remember(vm.savedRed) { { vm.savedRed.nichesCache.refresh() } }
        val getSavedRed: () -> SavedRed = remember(vm.savedRed) { { vm.savedRed } }

        val countNichesInCache = vm.savedRed.nichesCache.list.size

        val searchWidget: @Composable (Modifier) -> Unit = remember(vm.search) {
            { modifier -> RSearchTextField(vm.search, modifier = modifier) }
        }

        val actions = remember(onSortTypeChange, onUpClick, onNicheClick, onRefreshNichesCacheClick) {
            com.client.xvideos.r.ui.explorer.tab.niches.model.NichesTabActions(
                onSortTypeChange = onSortTypeChange,
                onUpClick = onUpClick,
                onNicheClick = onNicheClick,
                onRefreshNichesCacheClick = onRefreshNichesCacheClick
            )
        }

        val cacheState = com.client.xvideos.r.ui.explorer.tab.niches.model.NichesCacheState(
            count = countNichesInCache,
            progress = vm.savedRed.nichesCache.progress,
            lastModifiedHour = vm.savedRed.nichesCache.lastModifiedHour
        )

        R_ScreenNichesTabContent(
            listState = listState,
            niches = nicheItems,
            sortType = sortType,
            isSearchFocused = isSearchFocused,
            savedRed = getSavedRed,
            searchWidget = searchWidget,
            cacheState = cacheState,
            actions = actions
        )
    }
}

@Composable
fun R_ScreenNichesTabContent(
    listState: LazyListState,
    niches: List<Niche>,
    sortType: Order,
    isSearchFocused: Boolean,
    savedRed: () -> SavedRed?,
    searchWidget: @Composable (Modifier) -> Unit,
    cacheState: com.client.xvideos.r.ui.explorer.tab.niches.model.NichesCacheState,
    actions: com.client.xvideos.r.ui.explorer.tab.niches.model.NichesTabActions,
    modifier: Modifier = Modifier,
) {
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyColumn(gridState = listState)
    val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

    if (cacheState.count == 0) {
        Refresh(
            onRefreshNichesCacheClick = actions.onRefreshNichesCacheClick,
            nichesCacheProgress = cacheState.progress,
            modifier = modifier,
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NichesBottomBar(
                    isSearchFocused = isSearchFocused,
                    sortType = sortType,
                    onSortTypeChange = actions.onSortTypeChange,
                    onUpClick = actions.onUpClick,
                    searchWidget = searchWidget
                )
            },
            containerColor = Theme.tabLevel1,
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = getTopInsetDp())
                ) {
                    item(key = "refresh_mini", contentType = "refresh_mini") {
                        AnimatedVisibility(cacheState.lastModifiedHour > 72, enter = fadeIn(), exit = fadeOut()) {
                            RefreshMini(
                                onRefreshNichesCacheClick = actions.onRefreshNichesCacheClick,
                                nichesCacheProgress = cacheState.progress,
                                cacheHour = cacheState.lastModifiedHour
                            )
                        }
                    }

                    if (niches.isEmpty()) {
                        item(key = "empty_placeholder", contentType = "empty_placeholder") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No results",
                                    color = Color.Gray,
                                    fontFamily = Theme.R.fontFamilyDMsanss
                                )
                            }
                        }
                    } else {
                        val currentRed = savedRed()
                        items(items = niches, key = { it.id }, contentType = { "niche" }) { item ->
                            if (currentRed != null) {
                                NicheItemRow(
                                    item = item,
                                    savedRed = currentRed,
                                    onNicheClick = actions.onNicheClick,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .padding(horizontal = 8.dp)
                                        .fillMaxWidth()
                                        .height(78.dp)
                                        .background(Theme.tabLevel3, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        modifier = Modifier.padding(start = 16.dp),
                                        fontFamily = Theme.R.fontFamilyDMsanss
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .width(2.dp)
                ) {
                    VerticalScrollbar(scrollPercentProvider)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun R_ScreenNichesTabPreview() {
    val items = remember {
        listOf(
            Niche("1", "Amateurs", 1200, 5000, "", null),
            Niche("2", "Anal", 1500, 8000, "", null),
            Niche("3", "Babe", 800, 3000, "", null)
        )
    }

    R_ScreenNichesTabContent(
        listState = rememberLazyListState(),
        niches = items,
        sortType = Order.NICHES_SUBSCRIBERS_D,
        isSearchFocused = false,
        savedRed = { null },
        searchWidget = { modifier ->
            Box(
                modifier.height(44.dp).background(Theme.tabLevel0, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Search niches...",
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 14.sp
                )
            }
        },
        cacheState = com.client.xvideos.r.ui.explorer.tab.niches.model.NichesCacheState(
            count = 10,
            progress = 1f,
            lastModifiedHour = 1L
        ),
        actions = com.client.xvideos.r.ui.explorer.tab.niches.model.NichesTabActions(
            onSortTypeChange = {},
            onUpClick = {},
            onNicheClick = {},
            onRefreshNichesCacheClick = {}
        )
    )
}
