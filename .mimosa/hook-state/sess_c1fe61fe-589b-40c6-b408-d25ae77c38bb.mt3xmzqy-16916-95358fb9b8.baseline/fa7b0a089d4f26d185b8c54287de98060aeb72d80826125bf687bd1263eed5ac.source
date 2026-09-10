package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

import android.annotation.SuppressLint
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
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
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import timber.log.Timber
import javax.inject.Inject

object L_ScreenSavedAlbumsTab : Screen {

    override val key: ScreenKey = uniqueScreenKey

    private fun readResolve(): Any = L_ScreenSavedAlbumsTab

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLSavedAlbumsSM = getScreenModel()
        val state = vm.state

        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(state)


        val topInset = getTopInsetDp()

        Box(
            modifier = Modifier
                .fillMaxSize()
                //.padding(top = topInset)
                .background(Theme.background)
        )
        {
                    LazyVerticalGrid(
                        state = state,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    )
                    {
                        item ( span = { GridItemSpan(maxLineSpan) }){
                            Box(modifier = Modifier.height(topInset))
                        }

                        items(vm.albums, key = { it.id }) {
                            val albumId = it.id.toLongOrNull()
                            AlbumListItem(
                                title = it.title,
                                coverUrl = it.cover?.url.orEmpty(),
                                numberOfAnimatedPictures = it.number_of_animated_pictures,
                                numberOfPictures = it.number_of_pictures,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                if (albumId != null) {
                                    navigator.push(ScreenLAlbum(albumId))
                                } else {
                                    SnackBar.error("Не удалось открыть альбом: пустой id")
                                }
                            }
                        }
                    }

            /** Вертикальный индикатор прокрутки */
            Box( modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd).width(2.dp) ) { VerticalScrollbar { scrollPercent.value } }
        }


    }

}


class ScreenLSavedAlbumsSM @Inject constructor(
    val saved: SavedL
) : ScreenModel {

    val state = LazyGridState()

    val albums: List<com.client.xvideos.l.model.AlbumDetails>
        get() = saved.albums.list.filter { it.id.toLongOrNull() != null }

    init {
        Timber.i("iii ScreenLSavedAlbumsSM init")
        if (albums.isEmpty()) saved.albums.refresh()
    }

    override fun onDispose() {
        super.onDispose()
        Timber.i("iii ScreenLSavedAlbumsSM onDispose")
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
