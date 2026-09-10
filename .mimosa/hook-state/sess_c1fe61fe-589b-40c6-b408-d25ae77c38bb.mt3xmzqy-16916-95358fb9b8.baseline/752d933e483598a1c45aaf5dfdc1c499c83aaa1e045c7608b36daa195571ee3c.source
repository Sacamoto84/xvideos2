package com.client.xvideos.l.ui.screens.screenAlbumList

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.bottomBar.AlbumListBottomBar
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.AlbumListFilter
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import com.client.xvideos.l.model.AlbumListFilter as LAlbumListFilter


/**
 * Пейджер, у которого число страниц приходит извне и меняется по ходу загрузки.
 *
 * `Saver` здесь был, но не использовался ни разу: состояние живёт в
 * `ScreenAlbumListSM` и переживает пересоздание экрана вместе с ним. Хуже, что
 * при восстановлении он подставлял `updatedPageCount = { it[2] as Int }` —
 * лямбду-константу, — и число страниц замирало на сохранённом. Удалён, чтобы
 * его не подключили однажды по имени.
 */
class DefaultPagerState1(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int,
) : PagerState(currentPage, currentPageOffsetFraction) {

    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int
        get() = pageCountState.value.invoke()
}


object L_ScreenAlbumList : Screen {

    private fun readResolve(): Any = L_ScreenAlbumList

    fun create(filter: LAlbumListFilter?, title: String = ""): Screen = ScreenLAlbumList(filter, title)

    @Composable
    override fun Content() {
        ScreenAlbumListContent(initialFilter = null)
    }
}

private class ScreenLAlbumList(
    private val initialFilter: LAlbumListFilter?,
    private val title: String
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        ScreenAlbumListContent(initialFilter = initialFilter, title = title)
    }
}

@OptIn(ExperimentalZoomableApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun Screen.ScreenAlbumListContent(
    initialFilter: LAlbumListFilter?,
    title: String = ""
)
{
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenLAlbumListSM, ScreenLAlbumListSM.Factory> { factory ->
            factory.create(initialFilter)
        }
        val bigList = vm.bigList
        val info = vm.info.collectAsStateWithLifecycle().value
        val currentFilter = vm.filter.collectAsStateWithLifecycle().value
        val isRequest = vm.isRequest.collectAsStateWithLifecycle().value
        val filterGCount = vm.filterGenreStateCount.collectAsStateWithLifecycle().value
        val filterTagsCount = vm.filterTaggedStateCount.collectAsStateWithLifecycle().value
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        var totalPages by remember { mutableIntStateOf(1) }

        val usePadding = Settings.useCutoutPadding.field.collectAsStateWithLifecycle().value

        // ✅ Состояние для диалога
        var showFilterDialog by remember { mutableStateOf(false) }

        val topInset = getTopInsetDp()

        LaunchedEffect(info) { totalPages = info?.totalPages ?: 1 }

        LaunchedEffect(vm.statePager.currentPage) {
            vm.statePager.pageCountState.value = { totalPages }
            vm.savedPagerPage = vm.statePager.currentPage
            val currentPage = vm.statePager.currentPage
            val pagesToLoad = setOf(
                maxOf(0, currentPage - 1),
                currentPage,
                minOf(vm.statePager.pageCount - 1, currentPage + 1),
                minOf(vm.statePager.pageCount - 1, currentPage + 2)
            )
            pagesToLoad.forEach { page -> vm.loadAlbumList(page) }
        }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (isRequest) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color(0xff0c94ff)),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                },
                bottomBar = {
                    AlbumListBottomBar(
                        onClickVisibleFilter = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            // ✅ Открываем диалог
                            showFilterDialog = true
                        },
                        currentPage = vm.statePager.currentPage,
                        totalPages = info?.totalPages ?: 1,
                        onChange = {
                            scope.launch { vm.statePager.scrollToPage(it) }
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            vm.loadAlbumList(it)
                        }
                    )
                },
                containerColor = Theme.background
            )
            { padding ->

                HorizontalPager(
                    vm.statePager,
                    Modifier
                        .padding(bottom = padding.calculateBottomPadding())
                        .fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { page -> "${key}_page_$page" }
                ) { page ->

                    val pageItems = bigList[page]?.albumListImplInfoAndList?.items.orEmpty()
                    val stateGrid = vm.stateGrid.getOrPut(page) { LazyGridState() }

                    LazyVerticalGridScrollbar(
                        state = stateGrid,
                        settings = ScrollbarSettings.Default.copy(
                            thumbUnselectedColor = Color(0xFFA3A3A3),
                            thumbSelectedColor = Color(0xFFB3B3B3),
                            thumbThickness = 3.dp,
                            scrollbarPadding = 0.dp,
                            alwaysShowScrollbar = false
                        )
                    ) {
                        LazyVerticalGrid( state = stateGrid, modifier = Modifier.fillMaxSize(),  columns = GridCells.Fixed(2) )
                        {
                            item(key = "dummy", span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    Modifier
                                        .then(
                                            if (title != "") Modifier.height(40.dp) else Modifier.height(topInset)
                                        )
                                        .background(Theme.L.red)
                                        .padding(start = 24.dp), contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(text = title, color = Color.White, fontFamily = Theme.L.fontFamilyKarla)
                                }
                            }

                            items(
                                count = pageItems.size,
                                // Индекс в ключе обязателен: выдача может повторить
                                // один альбом на странице, а дублирующийся ключ роняет
                                // LazyLayout ("Key ... was already used").
                                key = { "${pageItems[it].id}#$it" }
                            ) { index ->

                                val item = pageItems[index]

                                Box(
                                    Modifier.padding(vertical = 2.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                )
                                {
                                    AlbumListItem(
                                        title = item.title,
                                        coverUrl = item.cover?.url.orEmpty(),
                                        numberOfAnimatedPictures = item.numberOfAnimatedPictures,
                                        numberOfPictures = item.numberOfPictures,
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                        navigator.push(ScreenLAlbum(item.id.toLong()))
                                    }
                                }
                            }
                        }
                    }

                    val status = vm.bigList[page]?.status
                    if ((status == StatusAlbumList.DOWNLOADING) && (pageItems.isEmpty())){
                        Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center ) { CircularProgressIndicator() }
                    }

                } //HorizontalPager(

            } //Scaffold
        }

        // ✅ ДИАЛОГ С ФИЛЬТРОМ
        AnimatedVisibility(
            visible = showFilterDialog,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ✅ УДАЛИТЕ этот Box с затемнением:
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showFilterDialog = false }
                )

                // Только панель фильтра
                Surface(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .fillMaxWidth(0.95f)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    AlbumListFilter(
                        filter = currentFilter,
                        filterGCount = filterGCount,
                        filterTagsCount = filterTagsCount,
                        onClose = { showFilterDialog = false }
                    ) { newFilter ->
                        vm.screenModelScope.launch {
                            vm.stateGrid.clear()
                            vm.statePager.scrollToPage(0)
                            vm.filterUpdate(newFilter)
                            vm.loadInitialData()
                            showFilterDialog = false
                        }
                    }
                }
            }
        }
}

