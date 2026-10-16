package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.ui.screens.explorer.tab.saved.albums.atom.SavedAlbumGridItem
import com.client.xvideos.l.ui.screens.explorer.tab.saved.molecule.LSortMenuButton
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

object L_ScreenSavedAlbumsTab : Screen {

    override val key: ScreenKey = "L_ScreenSavedAlbumsTab"

    private fun readResolve(): Any = L_ScreenSavedAlbumsTab

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLSavedAlbumsSM = getScreenModel()
        val state = vm.state
        val orderOrdinal by Settings.l_savedAlbums_order.field.collectAsStateWithLifecycle()
        val order = lSavedAlbumsOrderOf(orderOrdinal)
        // Список хранилища читается внутри derivedStateOf: новое или удалённое
        // сохранение пересчитывает порядок без ключа-списка.
        val albums by remember(order) {
            derivedStateOf { lSortedSavedAlbums(vm.albums, order) }
        }
        val scope = rememberCoroutineScope()
        // Новый порядок — с начала списка.
        val onOrderChange: (LSavedAlbumsOrder) -> Unit = remember(state, scope) {
            { value ->
                Settings.l_savedAlbums_order.setValue(value.ordinal)
                scope.launch { state.scrollToItem(0) }
            }
        }

        val onAlbumClick: (AlbumDetails) -> Unit = remember(navigator) {
            { item ->
                val albumId = item.id.toLongOrNull()
                if (albumId != null) {
                    navigator.push(ScreenLAlbum(albumId))
                } else {
                    SnackBar.error("Не удалось открыть альбом: пустой id")
                }
            }
        }

        val topInset = getTopInsetDp()

        SavedAlbumsTabContent(
            albums = albums,
            state = state,
            topInset = topInset,
            order = order,
            onOrderChange = onOrderChange,
            onAlbumClick = onAlbumClick
        )

    }

}

private val ORDER_OPTIONS: ImmutableList<String> = LSavedAlbumsOrder.entries.map { it.title }.toPersistentList()

@Composable
fun SavedAlbumsTabContent(
    albums: List<AlbumDetails>,
    state: LazyGridState,
    topInset: Dp,
    order: LSavedAlbumsOrder,
    onOrderChange: (LSavedAlbumsOrder) -> Unit,
    onAlbumClick: (AlbumDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSelectOrder = remember(onOrderChange) {
        { index: Int -> onOrderChange(LSavedAlbumsOrder.entries[index]) }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        if (albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нет сохранённых альбомов",
                    color = Theme.L.textColor.copy(alpha = 0.6f)
                )
            }
        } else {
            // itemsToIgnore = 1: нулевой item грида — full-span шапка (вырез и
            // сортировка), без него индикатор считает её контентом и врёт по позиции и длине.
            val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(state, itemsToIgnore = 1)
            val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

            LazyVerticalGrid(
                state = state,
                columns = GridCells.Fixed(adaptiveGridColumns()),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                item(
                    key = "top_spacer",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "top_spacer"
                ) {
                    // Сортировка — та же кнопка, что в коллекциях; уезжает со списком.
                    Box(
                        modifier = Modifier
                            .padding(top = topInset)
                            .adaptiveContentWidth()
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                    ) {
                        LSortMenuButton(
                            options = ORDER_OPTIONS,
                            selectedIndex = order.ordinal,
                            onSelectIndex = onSelectOrder,
                            contentDescription = "Порядок альбомов"
                        )
                    }
                }

                // Индекс в ключе обязателен: сохранённый список может
                // содержать один альбом дважды, а дублирующийся ключ
                // роняет LazyLayout ("Key ... was already used") — тот же
                // приём, что и в ScreenAlbumList.
                itemsIndexed(
                    items = albums,
                    key = { index, item -> "${item.id}#$index" },
                    contentType = { _, _ -> "album_item" }
                ) { _, item ->
                    SavedAlbumGridItem(
                        item = item,
                        onAlbumClick = onAlbumClick,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                    )
                }
            }

            /** Вертикальный индикатор прокрутки */
            Box(modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd).width(2.dp)) {
                VerticalScrollbar(scrollPercentProvider)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SavedAlbumsTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        SavedAlbumsTabContent(
            albums = emptyList(),
            state = rememberLazyGridState(),
            topInset = 24.dp,
            order = LSavedAlbumsOrder.NEWEST,
            onOrderChange = {},
            onAlbumClick = {}
        )
    }
}
