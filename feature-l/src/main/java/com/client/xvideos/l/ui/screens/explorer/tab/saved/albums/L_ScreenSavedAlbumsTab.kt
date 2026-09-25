package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.client.xvideos.l.model.AlbumDetails
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.ui.theme.XvideosTheme
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import timber.log.Timber
import javax.inject.Inject

private const val GRID_COLUMNS = 2
private const val EMPTY_ALBUMS_ALPHA = 0.6f
private const val TEXT_NO_SAVED_ALBUMS = "Нет сохранённых альбомов"
private const val ERROR_EMPTY_ID = "Не удалось открыть альбом: пустой id"
private const val ITEM_KEY_TOP_SPACER = "top_spacer"
private const val CONTENT_TYPE_TOP_SPACER = "top_spacer"
private const val CONTENT_TYPE_ALBUM_ITEM = "album_item"
private val ALBUM_PADDING_HORIZONTAL = 2.dp
private val ALBUM_PADDING_VERTICAL = 2.dp
private val SCROLLBAR_WIDTH = 2.dp
private val ITEM_PADDING_MODIFIER = Modifier.padding(horizontal = ALBUM_PADDING_HORIZONTAL, vertical = ALBUM_PADDING_VERTICAL)

object L_ScreenSavedAlbumsTab : Screen {

    override val key: ScreenKey = "L_ScreenSavedAlbumsTab"

    private fun readResolve(): Any = L_ScreenSavedAlbumsTab

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLSavedAlbumsSM = getScreenModel()
        val state = vm.state
        val albums by remember(vm.saved.albums.list) {
            derivedStateOf { vm.albums }
        }

        val onAlbumClick: (AlbumDetails) -> Unit = remember(navigator) {
            { item ->
                val albumId = item.id.toLongOrNull()
                if (albumId != null) {
                    navigator.push(ScreenLAlbum(albumId))
                } else {
                    SnackBar.error(ERROR_EMPTY_ID)
                }
            }
        }

        val topInset = getTopInsetDp()

        SavedAlbumsTabContent(
            albums = albums,
            state = state,
            topInset = topInset,
            onAlbumClick = onAlbumClick
        )

    }

}

@Composable
fun SavedAlbumsTabContent(
    albums: List<AlbumDetails>,
    state: LazyGridState,
    topInset: Dp,
    onAlbumClick: (AlbumDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    text = TEXT_NO_SAVED_ALBUMS,
                    color = Theme.L.textColor.copy(alpha = EMPTY_ALBUMS_ALPHA)
                )
            }
        } else {
            // itemsToIgnore = 1: нулевой item грида — full-span спейсер под вырез,
            // без него индикатор считает спейсер контентом и врёт по позиции и длине.
            val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(state, itemsToIgnore = 1)
            val scrollPercentProvider = remember(scrollPercent) { { scrollPercent.value } }

            LazyVerticalGrid(
                state = state,
                columns = GridCells.Fixed(GRID_COLUMNS),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                item(
                    key = ITEM_KEY_TOP_SPACER,
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = CONTENT_TYPE_TOP_SPACER
                ) {
                    Box(modifier = Modifier.height(topInset))
                }

                // Индекс в ключе обязателен: сохранённый список может
                // содержать один альбом дважды, а дублирующийся ключ
                // роняет LazyLayout ("Key ... was already used") — тот же
                // приём, что и в ScreenAlbumList.
                itemsIndexed(
                    items = albums,
                    key = { index, item -> "${item.id}#$index" },
                    contentType = { _, _ -> CONTENT_TYPE_ALBUM_ITEM }
                ) { _, item ->
                    SavedAlbumGridItem(
                        item = item,
                        onAlbumClick = onAlbumClick,
                        modifier = ITEM_PADDING_MODIFIER
                    )
                }
            }

            /** Вертикальный индикатор прокрутки */
            Box(modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd).width(SCROLLBAR_WIDTH)) {
                VerticalScrollbar(scrollPercentProvider)
            }
        }
    }
}

@Composable
private fun SavedAlbumGridItem(
    item: AlbumDetails,
    onAlbumClick: (AlbumDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(item, onAlbumClick) { { onAlbumClick(item) } }
    AlbumListItem(
        title = item.title,
        coverUrl = item.cover?.url.orEmpty(),
        numberOfAnimatedPictures = item.number_of_animated_pictures,
        numberOfPictures = item.number_of_pictures,
        modifier = modifier,
        onClick = onClick,
    )
}


@Stable
class ScreenLSavedAlbumsSM @Inject constructor(
    val saved: SavedL
) : ScreenModel {

    val state = LazyGridState()

    val albums: List<AlbumDetails>
        get() = saved.albums.list.filter { it.id.toLongOrNull() != null }

    init {
        Timber.d("ScreenLSavedAlbumsSM init")
        if (albums.isEmpty()) saved.albums.refresh()
    }

    override fun onDispose() {
        super.onDispose()
        Timber.d("ScreenLSavedAlbumsSM onDispose")
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedAlbums {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLSavedAlbumsSM::class)
    abstract fun bindScreenRedFulScreenSreenModel(hiltListScreenModel: ScreenLSavedAlbumsSM): ScreenModel
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SavedAlbumsTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        SavedAlbumsTabContent(
            albums = emptyList(),
            state = rememberLazyGridState(),
            topInset = 24.dp,
            onAlbumClick = {}
        )
    }
}
