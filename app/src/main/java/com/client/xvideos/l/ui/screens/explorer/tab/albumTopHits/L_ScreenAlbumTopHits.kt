package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.l.net.AlbumTopHitsImpl
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import timber.log.Timber
import javax.inject.Inject







object L_ScreenAlbumTopHits : Screen {

    override val key: ScreenKey = uniqueScreenKey

    private fun readResolve(): Any = L_ScreenAlbumTopHits

    @OptIn(ExperimentalZoomableApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        val vm: ScreenLAlbumTopHitsSM = getScreenModel()

        val items = vm.albumTopHits.collectAsState().value?.items

        //val haptic = LocalHapticFeedback.current

        //val vm: ScreenLRootSM = getScreenModel()

        //var dialogExpanded by remember { mutableStateOf(false) }

        //val scope = rememberCoroutineScope()

        //var selectIndexDrawer by remember { mutableStateOf(SelectIndex.Unselect) }




        Scaffold(
            containerColor = ThemeL.greyBackground,
        ) {

            LazyColumn(state = vm.state) {

                items(items?.size ?: 0) { index ->

                    val item = items?.get(index) ?: return@items

                    Text(
                        item.title,
                        color = ThemeL.textColor,
                        fontSize = 24.sp,
                        fontFamily = ThemeL.fontFamilyKarla,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp)
                    )

                    FlowRow(
                        maxItemsInEachRow = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    )
                    {

                        val itemWidth = (LocalConfiguration.current.screenWidthDp.dp - 8.dp) / 3  // учитываем padding

                        item.items.dropLast(1).forEach { item ->
                            Box(
                                modifier = Modifier.width(itemWidth).padding(vertical = 2.dp)
                            ) {
                                AlbumListItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    title = item.title,
                                    coverUrl = item.cover.url,
                                    numberOfAnimatedPictures = item.numberOfAnimatedPictures,
                                    numberOfPictures = item.numberOfPictures,
                                    onClick = { navigator.push(ScreenLAlbum(item.id.toLong())) }
                                )
                            }
                        }
                    }
                    ButtonSeeAll()
                }
            }
        }
    }
}



@Preview
@Composable
private fun ButtonSeeAll() {
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .padding(horizontal = 4.dp)
            .fillMaxWidth()
            .height(32.dp)
            .border(1.dp, ThemeL.grey2, RoundedCornerShape(8.dp))
            .background(ThemeL.grey3),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "See All >",
            color = ThemeL.textColor,
            modifier = Modifier,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontFamily = ThemeL.fontFamilyKarla,
            fontWeight = FontWeight.Medium,
        )
    }
}


class ScreenLAlbumTopHitsSM @Inject constructor(
    val luscious: Luscious
) : ScreenModel {

    val state = LazyListState()

    var albumTopHits = MutableStateFlow<AlbumTopHitsImpl?>(null)

    init {
        Timber.i("iii ScreenLAlbumTopHitsSM init")
        screenModelScope.launch {
            albumTopHits.value = luscious.getAlbumTopHits()
        }
    }

    override fun onDispose() {
        super.onDispose()
        Timber.i("iii ScreenLAlbumTopHitsSM onDispose")
    }

}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumTopHits {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLAlbumTopHitsSM::class)
    abstract fun bindHiltProfilesScreenModelFactory(hiltListScreenModel: ScreenLAlbumTopHitsSM): ScreenModel
}
