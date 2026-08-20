package com.client.xvideos.r.ui.explorer.tab.gifs

import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.settings.element.SettingElementInt
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.ui.search.RSearchTextField
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.atom.ButtonUp
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
import com.client.xvideos.ui.theme.XvideosTheme
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

private val rColumnOptions = listOf(2, 3, 4)

fun normalizeRColumnCount(value: Int): Int {
    return value.takeIf { it in rColumnOptions } ?: rColumnOptions.first()
}

fun ColumnSelect_AddRColumn(pref: SettingElementInt) {
    val currentIndex = normalizeRColumnCount(pref.field.value)
    val currentPos = rColumnOptions.indexOf(currentIndex)
    val nextPos = (currentPos + 1) % rColumnOptions.size
    pref.setValue(rColumnOptions[nextPos])
}

object R_ScreenGifsTab : Screen {

    private fun readResolve(): Any = R_ScreenGifsTab

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm: ScreenRedExplorerGifsSM = getScreenModel()
        R_ScreenGifsTabContent(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun R_ScreenGifsTabContent(vm: ScreenRedExplorerGifsSM) {
    val navigator = LocalNavigator.current

    val haptic = LocalHapticFeedback.current

    val columnSelectRaw by Settings.r_explorerGifsTab_column_current_count.field.collectAsStateWithLifecycle()
    val columnSelect = normalizeRColumnCount(columnSelectRaw)

    val search = vm.search
    // Именно searchTextDone, а не searchText: набор сортировок должен
    // соответствовать запросу, который реально выполняется. По отображаемому
    // тексту список дёргался с первого же символа, до подтверждения ввода.
    val searchQuery by search.searchTextDone.collectAsStateWithLifecycle()
    val isFocused by search.focused.collectAsStateWithLifecycle()
    val sortType by vm.lazyHost.sortType.collectAsStateWithLifecycle()

    LaunchedEffect(columnSelect) { vm.lazyHost.columns = columnSelect }


    Scaffold(
        bottomBar = {
            StatelessGifsTabBottomBar(
                searchField = { modifier -> RSearchTextField(search, modifier = modifier) },
                searchQuery = searchQuery,
                isFocused = isFocused,
                sortType = sortType,
                onSortSelect = { vm.lazyHost.changeSortType(it) },
                onUpClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    vm.lazyHost.gotoUp()
                }
            )
        },
        containerColor = Theme.background
    ) { padding ->
        Box( modifier = Modifier
            .padding(bottom = padding.calculateBottomPadding())
            .fillMaxSize() )
        {
                LazyRow123(
                    host = vm.lazyHost,
                    modifier = Modifier.fillMaxSize(),
                    onClickOpenProfile = { name ->
                        vm.lazyHost.currentIndexGoto = vm.lazyHost.currentIndex
                        navigator?.push(ScreenRedProfile(name))
                    },
                    contentPadding = PaddingValues(top = 0.dp),
                )
        }
    }
}

@Composable
private fun StatelessGifsTabBottomBar(
    searchField: @Composable (Modifier) -> Unit,
    searchQuery: String,
    isFocused: Boolean,
    sortType: Order,
    onSortSelect: (Order) -> Unit,
    onUpClick: () -> Unit
) {
    Column(Modifier.background(Theme.tabLevel1)) {
        HorizontalDivider(color = Theme.R.colorBorderGray)
        Row( modifier = Modifier
            .padding(top = 1.dp, start = 1.dp)
            .background(Theme.tabLevel1), verticalAlignment = Alignment.Bottom )
        {

            AnimatedVisibility(
                visible = !isFocused,
                enter = expandHorizontally(animationSpec = tween(250)) + fadeIn(tween(250)),
                exit = shrinkHorizontally(animationSpec = tween(250)) + fadeOut(tween(250)),
            ) {
                val orders = remember(searchQuery.isEmpty()) {
                    if (searchQuery.isEmpty()) {
                        // Order.TOP вместо прежнего TOP_ALLTIME: «топ за всё
                        // время» у RedGifs так и называется, отдельного
                        // значения нет. Теперь набор ленты отличается от набора
                        // поиска ровно на Relevant.
                        listOf(Order.TOP_WEEK, Order.TOP_MONTH, Order.TOP, Order.TRENDING, Order.LATEST)
                    } else {
                        // У поиска свой набор: Relevant есть только здесь, а
                        // Week и Month сервер понимает как top7 и top28.
                        listOf(
                            Order.RELEVANT, Order.TOP, Order.TOP_WEEK,
                            Order.TOP_MONTH, Order.TRENDING, Order.LATEST
                        )
                    }
                }
                SortByOrder(
                    containerColor = Theme.tabLevel0,
                    list = orders,
                    selected = sortType,
                    onSelect = onSortSelect
                )
            }

            searchField(
                Modifier
                    .padding(start = 4.dp)
                    .weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            AnimatedVisibility(
                visible = !isFocused,
                enter = expandHorizontally(animationSpec = tween(250), expandFrom = Alignment.Start) + fadeIn(tween(250)),
                exit = shrinkHorizontally(animationSpec = tween(250), shrinkTowards = Alignment.Start) + fadeOut(tween(250)),
            ) {
                ButtonUp(onClick = onUpClick)
            }
        }
        Spacer(modifier = Modifier.height(1.dp))
        HorizontalDivider(color = Theme.R.colorCommonBackground)
        HorizontalDivider(color = Theme.R.colorBorderGray)
    }
}

class ScreenRedExplorerGifsSM @Inject constructor(
    connectivityObserver: ConnectivityObserver,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
    val searchNiches: R_SearchNiches,
) : ScreenModel {

    val lazyHost = LazyRow123Host(
        connectivityObserver = connectivityObserver,
        scope = screenModelScope,
        extraString = "",
        typePager = TypePager.TOP,
        block = block,
        redApi = redApi,
        savedRed = savedRed,
        downloadRed = downloadRed,
        search = search,
        searchNiches = searchNiches
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedExplorerGifs {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedExplorerGifsSM::class)
    abstract fun bindScreenRedExplorerGifsSreenModel(hiltListScreenModel: ScreenRedExplorerGifsSM): ScreenModel
}

@Preview
@Composable
private fun GifsTabBottomBarPreview() {
    XvideosTheme {
        StatelessGifsTabBottomBar(
            searchField = { modifier ->
                Box(
                    modifier
                        .height(40.dp)
                        .background(Color.DarkGray, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(" Search...", color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                }
            },
            searchQuery = "",
            isFocused = false,
            sortType = Order.TRENDING,
            onSortSelect = {},
            onUpClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun R_ScreenGifsTabSkeletonPreview() {
    XvideosTheme {
        Scaffold(
            bottomBar = {
                StatelessGifsTabBottomBar(
                    searchField = { Box(it
                        .height(40.dp)
                        .background(Color.DarkGray)) },
                    searchQuery = "",
                    isFocused = false,
                    sortType = Order.TRENDING,
                    onSortSelect = {},
                    onUpClick = {}
                )
            },
            containerColor = Theme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Gifs Content Placeholder", color = Color.White)
            }
        }
    }
}
