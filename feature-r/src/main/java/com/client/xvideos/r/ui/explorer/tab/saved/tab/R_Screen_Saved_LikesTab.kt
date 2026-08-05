package com.client.xvideos.r.ui.explorer.tab.saved.tab

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.common.search.R_SearchNiches
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.model.TypePager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

object R_Screen_Saved_LikesTab : Screen {

    private fun readResolve(): Any = R_Screen_Saved_LikesTab

    override val key: ScreenKey = uniqueScreenKey

    //@Transient
    //val columnSelect = ColumnSelect(Settings.l_likesTab_column_current_count)

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenSavedLikesSM = getScreenModel()

        val columnSelect = normalizeRColumnCount(
            Settings.r_likesTab_column_current_count.field.collectAsStateWithLifecycle().value
        )

        val pager = vm.likedHost.pager.collectAsLazyPagingItems()

        //Изменение количества отображаемых элементов
        LaunchedEffect(columnSelect) { vm.likedHost.columns = columnSelect }

        // Ключ-ссылка на list никогда не меняется — подписываемся на размер:
        // добавление (в т.ч. приём по P2P) и удаление лайка перезагружают pager.
        LaunchedEffect(Unit) {
            androidx.compose.runtime.snapshotFlow { vm.savedRed.likes.list.size }
                .collect { pager.refresh() }
        }

        Box(modifier = Modifier.fillMaxSize().background(Theme.background)) {

            LazyRow123(
                host = vm.likedHost,
                modifier = Modifier.fillMaxSize(),
                onClickOpenProfile = {
                    //vm.likedHost.currentIndexGoto = vm.likedHost.currentIndex
                    navigator.push(ScreenRedProfile(it))
                },
                //gotoPosition = vm.likedHost.currentIndexGoto,
                contentPadding = PaddingValues(0.dp),
                contentBeforeList = {
                    Box(modifier = Modifier.displayCutoutPadding()){}
                },
                isRunLike = true
            )

        }


    }



}

class ScreenSavedLikesSM @Inject constructor(
    connectivityObserver: ConnectivityObserver,
    val block: BlockRed,
    val redApi: RedApi,
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val search: R_SearchExplorer,
    val searchNiches: R_SearchNiches,

) : ScreenModel {

    val likedHost = LazyRow123Host(
        connectivityObserver = connectivityObserver,
        scope = screenModelScope,
        typePager = TypePager.R_SAVED_LIKES,
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
abstract class ScreenModuleRedSavedLikes {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedLikesSM::class)
    abstract fun bindScreenRedSavedLikesScreenModel(hiltListScreenModel: ScreenSavedLikesSM): ScreenModel
}
