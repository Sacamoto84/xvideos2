package com.client.xvideos.l.ui.screens.albumLandingTag

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.navigation.NavigationDepthState
import com.client.xvideos.l.model.Album
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Cover
import com.client.xvideos.l.model.Genre
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.model.Language
import com.client.xvideos.l.model.Tag
import com.client.xvideos.l.model.User
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber





class ScreenLAlbumLandingTag(val tag: String) : Screen {

    override val key: ScreenKey = "ScreenLAlbumLandingTag:$tag"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenLAlbumLandingTagSM, ScreenLAlbumLandingTagSM.Factory> { factory -> factory.create(tag) }
        BackHandler { navigator.pop() }
        val albumTopHits = vm.albumTopHits.collectAsStateWithLifecycle().value
        val items = albumTopHits?.sections
        val title = albumTopHits?.title
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Theme.background,
            topBar = {
                LandingTagTopBar(
                    title = "Tag: ${title ?: tag}",
                    onBack = { navigator.pop() }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Theme.background)
            ) {
                LazyColumn(state = vm.state, modifier = Modifier.fillMaxSize()) {
                    items(items?.size ?: 0, key = { index -> "${index}_${items?.get(index)?.title.orEmpty()}" }) { index ->
                        val item = items?.get(index) ?: return@items
                        LandingTagSectionItem(
                            item = item,
                            screenWidth = screenWidth,
                            onAlbumClick = { albumId -> navigator.push(ScreenLAlbum(albumId)) },
                            onSeeAllClick = {
                                val filter = vm.createFilter(item)
                                navigator.push(
                                    L_ScreenAlbumList.create(
                                        filter = filter,
                                        title = "Tag: ${title ?: tag}"
                                    )
                                )
                            }
                        )
                    }

                    item {
                        Spacer(Modifier.height(64.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LandingTagTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.background)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Theme.L.textColor
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            color = Theme.L.textColor,
            fontSize = 24.sp,
            fontFamily = Theme.L.fontFamilyKarla,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LandingTagSectionItem(
    item: Landing_page_albumSection,
    screenWidth: androidx.compose.ui.unit.Dp,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit
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
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val itemWidth = (screenWidth - 8.dp) / 3
        item.items.take(9).forEach { album ->
            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .padding(vertical = 2.dp)
            ) {
                AlbumListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = album.title,
                    coverUrl = album.cover?.url.orEmpty(),
                    numberOfAnimatedPictures = album.numberOfAnimatedPictures,
                    numberOfPictures = album.numberOfPictures,
                    onClick = { album.id.toLongOrNull()?.let { onAlbumClick(it) } }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .padding(horizontal = 4.dp)
            .fillMaxWidth()
            .height(40.dp)
            .border(2.dp, Theme.L.grey3, RoundedCornerShape(8.dp))
            .clickable(onClick = onSeeAllClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "See All >",
            color = Theme.L.textColor,
            textAlign = TextAlign.Center,
            fontSize = 22.sp,
            fontFamily = Theme.L.fontFamilyKarla,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C, widthDp = 390, heightDp = 900)
@Composable
private fun ScreenLAlbumLandingTagPreview() {
    val data = lAlbumLandingTagPreviewData()

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(
        modifier = Modifier.fillMaxSize().background(Theme.background)
    ) {
        LazyColumn(state = rememberLazyListState()) {
            item {
                Text(
                    "Tag: ${data.title}",
                    color = Theme.L.textColor,
                    fontSize = 32.sp,
                    fontFamily = Theme.L.fontFamilyKarla,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 16.dp)
                )
            }

            items(data.sections.size, key = { index -> "${index}_${data.sections.getOrNull(index)?.title.orEmpty()}" }) { index ->
                val section = data.sections[index]

                Text(
                    section.title,
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
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val itemWidth = (screenWidth - 8.dp) / 3
                    section.items.take(9).forEach { album ->
                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .padding(vertical = 2.dp)
                        ) {
                            AlbumListItem(
                                modifier = Modifier.fillMaxWidth(),
                                title = album.title,
                                coverUrl = album.cover?.url.orEmpty(),
                                numberOfAnimatedPictures = album.numberOfAnimatedPictures,
                                numberOfPictures = album.numberOfPictures,
                                onClick = {}
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(2.dp, Theme.L.grey3, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "See All >",
                        color = Theme.L.textColor,
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        fontFamily = Theme.L.fontFamilyKarla,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            item {
                Spacer(Modifier.height(64.dp))
            }
        }
    }
}

private fun lAlbumLandingTagPreviewData(): Landing_page_albumType {
    return Landing_page_albumType(
        title = "monster girl",
        sections = listOf(
            lAlbumLandingTagPreviewSection("Hentai Manga", "manga", 1),
            lAlbumLandingTagPreviewSection("Hentai Pictures", "hentai", 10),
            lAlbumLandingTagPreviewSection("Porn Pictures", "real", 20)
        )
    )
}

private fun lAlbumLandingTagPreviewSection(
    title: String,
    slugPrefix: String,
    idOffset: Int
): Landing_page_albumSection {
    val albums = listOf(
        lAlbumLandingTagPreviewAlbum(
            id = idOffset,
            title = "Moonlit Atelier Collection",
            pictures = 82,
            gifs = 6,
            coverUrl = "https://picsum.photos/seed/luscious-preview-$slugPrefix-1/420/620"
        ),
        lAlbumLandingTagPreviewAlbum(
            id = idOffset + 1,
            title = "Velvet Dungeon Sketchbook",
            pictures = 124,
            gifs = 0,
            coverUrl = "https://picsum.photos/seed/luscious-preview-$slugPrefix-2/420/620"
        ),
        lAlbumLandingTagPreviewAlbum(
            id = idOffset + 2,
            title = "Crimson Night Frames",
            pictures = 37,
            gifs = 12,
            coverUrl = "https://picsum.photos/seed/luscious-preview-$slugPrefix-3/420/620"
        ),
        lAlbumLandingTagPreviewAlbum(
            id = idOffset + 3,
            title = "Hidden Orchard",
            pictures = 58,
            gifs = 2,
            coverUrl = "https://picsum.photos/seed/luscious-preview-$slugPrefix-4/420/620"
        )
    )
    return Landing_page_albumSection(
        title = title,
        count = albums.size,
        itemType = "album",
        url = "/albums/list/?tagged=$slugPrefix&page=1",
        items = albums
    )
}

private fun lAlbumLandingTagPreviewAlbum(
    id: Int,
    title: String,
    pictures: Int,
    gifs: Int,
    coverUrl: String
): Album {
    return Album(
        typeName = "Album",
        id = id.toString(),
        title = title,
        description = "Preview album",
        likeStatus = "none",
        moderationStatus = "NOT_MODERATED",
        numberOfFavorites = 120 + id,
        numberOfDislikes = id,
        numberOfPictures = pictures,
        numberOfAnimatedPictures = gifs,
        numberOfDuplicates = 0,
        slug = title.lowercase().replace(" ", "-"),
        isManga = false,
        url = "/albums/${title.lowercase().replace(" ", "-")}_$id/",
        downloadUrl = "/download/preview/$id/",
        labels = emptyList(),
        permissions = listOf("can_add"),
        cover = Cover(
            width = 420,
            height = 620,
            size = "preview",
            url = coverUrl
        ),
        language = Language(
            id = "1",
            title = "English",
            url = "/languages/english_1/"
        ),
        createdBy = User(
            id = "preview",
            name = "preview_user",
            displayName = "Preview User",
            url = "/users/preview/"
        ),
        tags = listOf(
            Tag(
                id = "tag-preview",
                category = null,
                text = "preview",
                url = "/tags/preview/?type=album",
                count = 1
            )
        ),
        genres = listOf(
            Genre(
                id = "genre-preview",
                title = "Preview",
                actsAsWarning = false,
                url = "/genres/preview/?type=album"
            )
        )
    )
}

@Stable
class ScreenLAlbumLandingTagSM @AssistedInject constructor(
    @Assisted val tag: String,
    val luscious: Luscious,
    depthState: NavigationDepthState
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(tag: String): ScreenLAlbumLandingTagSM
    }

    val state = LazyListState()

    val albumTopHits = MutableStateFlow<Landing_page_albumType?>(null)

    init {
        Timber.d("ScreenLAlbumLandingTagSM init")
        screenModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    luscious.getLandingPageAlbumTag(tag)
                }
                albumTopHits.value = res.getOrNull()
                if (res.isFailure) {
                    Timber.w(res.exceptionOrNull(), "ScreenLAlbumLandingTagSM: failed to load tag $tag")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "ScreenLAlbumLandingTagSM: exception loading tag $tag")
            }
        }

        depthState.depth = 100
    }

    override fun onDispose() {
        super.onDispose()
        Timber.d("ScreenLAlbumLandingTagSM onDispose")
    }

    //section title

    //Hentai Manga
    //Hentai Pictures
    //Porn Pictures

    fun createFilter (item: Landing_page_albumSection): AlbumListFilter {

        val title = item.title

        val albumType = when (title) {
            "Hentai Manga" -> AlbumType.Manga
            "Hentai Pictures" -> AlbumType.Pictures
            "Porn Pictures" -> AlbumType.Pictures
            else -> AlbumType.Pictures
        }

        val contentId = when (title) {
            "Hentai Manga" -> ContentId.All
            "Hentai Pictures" -> ContentId.Hentai
            "Porn Pictures" -> ContentId.RealPeople
            else -> ContentId.All
        }

        return AlbumListFilter(
            display = "date_trending",
            album_type = albumType,
            content_id = contentId,
            tagPlus = listOf(tag)
        )

    }

}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumLandingTag {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenLAlbumLandingTagSM.Factory::class)
    abstract fun bindHiltLandingTagScreenModelFactory(
        hiltDetailsScreenModelFactory: ScreenLAlbumLandingTagSM.Factory
    ): ScreenModelFactory

}
