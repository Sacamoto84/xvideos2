package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.l.model.Album
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.AlbumListTopHits
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.net.AlbumTopHitsImpl
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import java.net.URLDecoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject







object L_ScreenAlbumTopHits : Screen {

    override val key: ScreenKey = "L_ScreenAlbumTopHits"

    private fun readResolve(): Any = L_ScreenAlbumTopHits

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLAlbumTopHitsSM = getScreenModel()
        val albumTopHits by vm.albumTopHits.collectAsStateWithLifecycle()
        val items = albumTopHits?.items
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val itemWidth = remember(screenWidth) { (screenWidth - 8.dp) / 3 }

        val onAlbumClick: (Long) -> Unit = remember(navigator) {
            { albumId -> navigator.push(ScreenLAlbum(albumId)) }
        }
        val onSeeAllClick: (String, String) -> Unit = remember(navigator) {
            { url, title ->
                navigator.push(
                    L_ScreenAlbumList.create(
                        filter = albumListFilterFromTopHitsUrl(url),
                        title = title
                    )
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(Theme.background)
        ) {

            LazyColumn(state = vm.state) {
                items(
                    items = items.orEmpty(),
                    key = { it.title },
                    contentType = { "top_hits_section" }
                ) { item ->
                    TopHitsSectionItem(
                        item = item,
                        itemWidth = itemWidth,
                        onAlbumClick = onAlbumClick,
                        onSeeAllClick = onSeeAllClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TopHitsSectionItem(
    item: AlbumListTopHits,
    itemWidth: Dp,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: (String, String) -> Unit,
) {
    Text(
        item.title,
        color = Theme.L.textColor,
        fontSize = 24.sp,
        fontFamily = Theme.L.fontFamilyKarla,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp)
    )

    FlowRow(
        maxItemsInEachRow = 3,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val albums = remember(item.items) { item.items.take(9) }
        albums.forEach { album ->
            key(album.id) {
                TopHitsAlbumItem(
                    album = album,
                    itemWidth = itemWidth,
                    onAlbumClick = onAlbumClick
                )
            }
        }
    }
    ButtonSeeAll(
        onClick = remember(item.url, item.title, onSeeAllClick) {
            { onSeeAllClick(item.url, item.title) }
        }
    )
}

@Composable
private fun TopHitsAlbumItem(
    album: Album,
    itemWidth: Dp,
    onAlbumClick: (Long) -> Unit,
) {
    val onClick = remember(album.id, onAlbumClick) {
        {
            val albumId = album.id.toLongOrNull()
            if (albumId != null) {
                onAlbumClick(albumId)
            }
        }
    }
    Box(
        modifier = Modifier.width(itemWidth).padding(vertical = 2.dp)
    ) {
        AlbumListItem(
            modifier = Modifier.fillMaxWidth(),
            title = album.title,
            coverUrl = album.cover?.url.orEmpty(),
            numberOfAnimatedPictures = album.numberOfAnimatedPictures,
            numberOfPictures = album.numberOfPictures,
            onClick = onClick
        )
    }
}

/**
 * Строит [AlbumListFilter] из URL категории top-hits, например:
 * `/albums/list/?album_type=manga&audience_ids=%2B1%2B2&display=date_trending&tagged=%2Bcollared&page=1`
 *
 * Параметры, которых нет в URL, берутся из дефолтов [AlbumListFilter].
 */
internal fun albumListFilterFromTopHitsUrl(url: String): AlbumListFilter {
    val params: Map<String, String> = url
        .substringAfter('?', "")
        .split('&')
        .mapNotNull { pair ->
            val separatorIndex = pair.indexOf('=')
            if (separatorIndex <= 0) return@mapNotNull null
            val name = URLDecoder.decode(pair.substring(0, separatorIndex), "UTF-8")
            val value = URLDecoder.decode(pair.substring(separatorIndex + 1), "UTF-8")
            name to value
        }
        .toMap()

    val default = AlbumListFilter()

    val albumType = params["album_type"]
        ?.let { v -> AlbumType.entries.find { it.value == v } }
        ?: default.album_type

    // "tagged" приходит как "+tag1+tag2" → ["tag1", "tag2"]
    val tagPlus = params["tagged"].orEmpty()
        .split('+')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return AlbumListFilter(
        display = params["display"] ?: default.display,
        album_type = albumType,
        audienceIds = params["audience_ids"] ?: default.audienceIds,
        languageIds = params["language_ids"] ?: default.languageIds,
        tagPlus = tagPlus
    )
}



@Composable
private fun ButtonSeeAll(onClick: () -> Unit) {
    val buttonShape = remember { RoundedCornerShape(8.dp) }
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .padding(horizontal = 4.dp)
            .fillMaxWidth()
            .height(32.dp)
            .clip(buttonShape)
            .border(1.dp, Theme.L.grey2, buttonShape)
            .background(Theme.L.grey3)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "See All >",
            color = Theme.L.textColor,
            modifier = Modifier,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontFamily = Theme.L.fontFamilyKarla,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview
@Composable
private fun ButtonSeeAllPreview() {
    ButtonSeeAll(onClick = {})
}


@Stable
class ScreenLAlbumTopHitsSM @Inject constructor(
    val luscious: Luscious
) : ScreenModel {

    val state = LazyListState()

    private val _albumTopHits = MutableStateFlow<AlbumTopHitsImpl?>(null)
    val albumTopHits: StateFlow<AlbumTopHitsImpl?> = _albumTopHits.asStateFlow()

    init {
        Timber.d("iii ScreenLAlbumTopHitsSM init")
        screenModelScope.launch {
            _albumTopHits.value = luscious.getAlbumTopHits()
        }
    }

    override fun onDispose() {
        super.onDispose()
        Timber.d("iii ScreenLAlbumTopHitsSM onDispose")
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
