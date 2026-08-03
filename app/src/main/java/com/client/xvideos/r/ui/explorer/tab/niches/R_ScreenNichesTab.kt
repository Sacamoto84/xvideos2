package com.client.xvideos.r.ui.explorer.tab.niches

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.explorer.RNavigationState

import com.client.xvideos.r.ui.niche.R_ScreenNiche
import com.client.xvideos.r.ui.profile.atom.VerticalScrollbar
import com.client.xvideos.r.ui.profile.rememberVisibleRangePercentIgnoringFirstNForLazyColumn
import com.client.xvideos.ui.theme.XvideosTheme
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class NicheScrollSnapshot(
    val itemCount: Int,
    val index: Int,
    val offset: Int
)

private fun filterAndSortNiches(
    niches: List<Niche>,
    query: String,
    order: Order
): List<Niche> {
    val filtered = if (query.isBlank()) {
        niches
    } else {
        niches.filter { it.name.contains(query, ignoreCase = true) }
    }

    return when (order) {
        Order.NICHES_SUBSCRIBERS_D -> filtered.sortedByDescending { it.subscribers }
        Order.NICHES_POST_D -> filtered.sortedByDescending { it.gifs }
        Order.NICHES_SUBSCRIBERS_A -> filtered.sortedBy { it.subscribers }
        Order.NICHES_POST_A -> filtered.sortedBy { it.gifs }
        Order.NICHES_NAME_A_Z -> filtered.sortedBy { it.name }
        Order.NICHES_NAME_Z_A -> filtered.sortedByDescending { it.name }
        else -> filtered.sortedBy { it.subscribers }
    }
}

object R_ScreenNichesTab : Screen {

    private fun readResolve(): Any = R_ScreenNichesTab

    override val key: ScreenKey = uniqueScreenKey

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

        val onSortTypeChange: (Order) -> Unit = remember { { vm.changeSortType(it) } }

        val onUpClick: () -> Unit = remember(listState, coroutineScope, navigationState) { {
            coroutineScope.launch {
                listState.scrollToItem(0)
                navigationState.resetNichesScrollPosition()
            }
        } }

        val onNicheClick: (String) -> Unit = remember(navigator) { { id -> navigator.push(R_ScreenNiche(id)) } }

        /**
         * Количество элементов в кэше
         */
        val countNichesInCache = vm.savedRed.nichesCache.list.size

        NichesTabContent(
            listState = listState,
            niches = nicheItems,
            sortType = sortType,
            onSortTypeChange = onSortTypeChange,
            isSearchFocused = isSearchFocused,
            onUpClick = onUpClick,
            onNicheClick = onNicheClick,
            savedRed = { vm.savedRed },
            searchWidget = { modifier ->
                vm.search.CustomBasicTextField( modifier = modifier )
            },
            onRefreshNichesCacheClick = {
                vm.savedRed.nichesCache.refresh()
            },
            nichesCacheProgress = vm.savedRed.nichesCache.progress,
            countNichesInCache = countNichesInCache,
            cacheHour = vm.savedRed.nichesCache.lastModifiedHour
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NichesTabContent(
    listState: LazyListState,
    niches: List<Niche>,
    sortType: Order,
    onSortTypeChange: (Order) -> Unit,
    isSearchFocused: Boolean,
    onUpClick: () -> Unit,
    onNicheClick: (String) -> Unit,
    savedRed: () -> SavedRed?,
    searchWidget: @Composable (Modifier) -> Unit,
    onRefreshNichesCacheClick: () -> Unit,
    nichesCacheProgress: Float,
    countNichesInCache : Int,
    cacheHour : Long
) {

    // Без `by`: позиция скролла меняется каждый кадр, чтение здесь
    // перекомпоновывало бы весь экран. См. VerticalScrollbar.
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyColumn(gridState = listState)

    if (countNichesInCache == 0) {
        Refresh(
            onRefreshNichesCacheClick = onRefreshNichesCacheClick,
            nichesCacheProgress = nichesCacheProgress,
        )
    } else {
        Scaffold(
            bottomBar = {
                NichesBottomBar(
                    isSearchFocused = isSearchFocused,
                    sortType = sortType,
                    onSortTypeChange = onSortTypeChange,
                    onUpClick = onUpClick,
                    searchWidget = searchWidget
                )
            },
            containerColor = Theme.tabLevel1,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()).fillMaxSize()
            )
            {
                LazyColumn( state = listState, modifier = Modifier.fillMaxSize(), contentPadding = WindowInsets.displayCutout.asPaddingValues() )
                {

                    item{
                        AnimatedVisibility(cacheHour > 72, enter = fadeIn(), exit = fadeOut() )
                        {
                            RefreshMini(
                                onRefreshNichesCacheClick = onRefreshNichesCacheClick,
                                nichesCacheProgress = nichesCacheProgress,
                                cacheHour = cacheHour
                            )
                        }
                    }

                    if (niches.isEmpty()) {
                        item {
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
                        items(items = niches, key = { it.id }, contentType = { "niche" }) { item ->
                            Box(modifier = Modifier.padding(vertical = 2.dp)) {
                                // Одно чтение вместо проверки одного вызова и
                                // `!!` на результате второго.
                                val red = savedRed()
                                if (red != null) {
                                    NichePreview2( niches = { item }, onClick = { onNicheClick(item.id) }, savedRed = { red } )
                                } else {
                                    // Placeholder for Preview
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp).fillMaxWidth().height(78.dp).background( Theme.tabLevel3, RoundedCornerShape(16.dp) ),
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
                }

                // Scrollbar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .width(2.dp)
                ) {
                    VerticalScrollbar { scrollPercent.value }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Refresh(
    onRefreshNichesCacheClick: () -> Unit,
    nichesCacheProgress: Float,
    refreshList: () -> Unit = {},
) {
    LaunchedEffect(nichesCacheProgress) {
        if (nichesCacheProgress == 1f) {
            delay(1000)
            refreshList.invoke()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.tabLevel1),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Отсутствует список Niches", style = styleTest)

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRefreshNichesCacheClick,
            colors = ButtonDefaults.buttonColors(containerColor = Theme.R.colorBlue)
        ) {
            Text("Скачать список ", style = styleTest.copy(fontSize = 18.sp))
        }
        Spacer(Modifier.height(16.dp))
        LinearWavyProgressIndicator(
            progress = { nichesCacheProgress },
            Modifier.graphicsLayer(
                alpha = if (nichesCacheProgress > 0f) 1f else 0f
            )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshMini(
    onRefreshNichesCacheClick: () -> Unit,
    nichesCacheProgress: Float= 0f,
    refreshList: () -> Unit = {},
    cacheHour : Long = 1L
) {
    LaunchedEffect(nichesCacheProgress) {
        if (nichesCacheProgress == 1f) {
            delay(1000)
            refreshList.invoke()
        }
    }

    Row(
        modifier = Modifier.padding(horizontal = 8.dp).padding(vertical = 4.dp).fillMaxSize().background(Theme.tabLevel1), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    )
    {

        Text("Старый список Niches, возраст $cacheHour часов", style = styleTest.copy(fontSize = 14.sp))

        Spacer(Modifier.height(8.dp))

        Box() {

            if (nichesCacheProgress == 0f) {
                IconButton(onClick = onRefreshNichesCacheClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp).background(
                            Theme.R.colorBlue,
                            CircleShape
                        ).padding(4.dp)
                    )
                }
            }



            CircularWavyProgressIndicator(
                progress = { nichesCacheProgress },
                Modifier.size(36.dp)

                    .graphicsLayer(
                        alpha = if (nichesCacheProgress > 0f) 1f else 0f
                    )
            )

        }

    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun R_ScreenNichesTabPreview() {
    val items = remember {
        listOf(
            Niche("1", "Amateurs", 1200, 5000, "", null),
            Niche("2", "Anal", 1500, 8000, "", null),
            Niche("3", "Babe", 800, 3000, "", null),
            Niche("4", "Blowjob", 2500, 15000, "", null),
            Niche("5", "Creampie", 1800, 9000, "", null)
        )
    }

    NichesTabContent(
        listState = rememberLazyListState(),
        niches = items,
        sortType = Order.NICHES_SUBSCRIBERS_D,
        onSortTypeChange = {},
        isSearchFocused = false,
        onUpClick = {},
        onNicheClick = {},
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
        onRefreshNichesCacheClick = {},
        nichesCacheProgress = 1f,
        countNichesInCache = 10,
        cacheHour = 1
    )
}

class ScreenRedExplorerNichesSM @Inject constructor(
    val navigationState: RNavigationState,
    val savedRed: SavedRed,
    val search: R_SearchNiches,
) : ScreenModel {

    private val _sortType = MutableStateFlow(navigationState.nichesSort)
    val sortType = _sortType.asStateFlow()

    fun changeSortType(order: Order) {
        navigationState.updateNichesSort(order)
        _sortType.value = order
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedExplorerNiches {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedExplorerNichesSM::class)
    abstract fun bindScreenRedExplorerNichesSreenModel(hiltListScreenModel: ScreenRedExplorerNichesSM): ScreenModel
}

@Preview(showBackground = true, backgroundColor = 0xFF282828)
@Composable
fun RefreshPreview() {
    XvideosTheme {
        Column(
            modifier = Modifier
                .background(Theme.tabLevel1)
                .padding(8.dp)
        ) {
            Refresh(
                onRefreshNichesCacheClick = {},
                nichesCacheProgress = 0f,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Refresh(
                onRefreshNichesCacheClick = {},
                nichesCacheProgress = 0.45f,
            )
        }
    }
}

private val styleTest = androidx.compose.ui.text.TextStyle(
    fontSize = 20.sp,
    color = Color.White,
    fontFamily = Theme.R.fontFamilyDMsanss
)
