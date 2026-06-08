package com.client.xvideos.l.ui.screens.explorer.tab.saved.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
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
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import my.nanihadesuka.compose.LazyVerticalGridScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import timber.log.Timber
import javax.inject.Inject

object L_ScreenSavedAlbumsTab : Screen {

    override val key: ScreenKey = uniqueScreenKey

    private fun readResolve(): Any = L_ScreenSavedAlbumsTab

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLSavedAlbumsSM = getScreenModel()
        val state = vm.state

        Box(modifier = Modifier.background(Theme.background)) {

            LazyVerticalGridScrollbar(state = state, settings = ScrollbarSettings.Default)
            {
                    LazyVerticalGrid(
                        state = state,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    )
                    {
                        items(vm.albums) {
                            val albumId = it.id.toLongOrNull()
                            AlbumListItem(
                                title = it.title,
                                coverUrl = it.cover.url,
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
                }
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
