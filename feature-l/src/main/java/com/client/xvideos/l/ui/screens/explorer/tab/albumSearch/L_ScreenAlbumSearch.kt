package com.client.xvideos.l.ui.screens.explorer.tab.albumSearch

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import com.client.xvideos.common.ui.IncognitoKeyboard
import androidx.activity.compose.BackHandler
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject


object L_ScreenAlbumSearch : Screen {

    override val key: ScreenKey = "L_ScreenAlbumSearch"

    private fun readResolve(): Any = L_ScreenAlbumSearch

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLAlbumSearchSM = getScreenModel()

        val searchText = vm.searchText.collectAsStateWithLifecycle().value
        BackHandler(enabled = searchText.isNotEmpty()) { vm.searchText.value = "" }
        val result = vm.result.collectAsStateWithLifecycle().value
        val isLoading = vm.isLoading.collectAsStateWithLifecycle().value
        val sections = result?.sections
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.background)
        ) {

            LazyColumn(state = vm.state, modifier = Modifier.fillMaxSize()) {

                item {
                    AlbumSearchInputField(
                        searchText = searchText,
                        onSearchTextChange = { vm.searchText.value = it },
                        onSearch = { vm.search() }
                    )
                }

                item {
                    if (result?.title != null) {
                        Text(
                            result.title,
                            color = Theme.L.textColor,
                            fontSize = 32.sp,
                            fontFamily = Theme.L.fontFamilyKarla,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }

                if (!isLoading && hasNoSearchResults(result)) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Ничего не найдено",
                                color = Theme.L.textColor,
                                fontSize = 18.sp,
                                fontFamily = Theme.L.fontFamilyKarla
                            )
                        }
                    }
                }

                items(
                    count = sections?.size ?: 0,
                    key = { sections?.get(it)?.title ?: it }
                ) { index ->
                    val section = sections?.get(index) ?: return@items
                    AlbumSearchSectionBlock(
                        section = section,
                        screenWidth = screenWidth,
                        onAlbumClick = { albumId -> navigator.push(ScreenLAlbum(albumId)) },
                        onSeeAllClick = {
                            navigator.push(
                                L_ScreenAlbumList.create(
                                    filter = vm.createFilter(section),
                                    title = "Search: ${vm.searchText.value}"
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

@Composable
private fun AlbumSearchInputField(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = Modifier
            .displayCutoutPadding()
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        singleLine = true,
        label = { Text("Search") },
        textStyle = Theme.L.Type.body.copy(color = Theme.L.textColor),
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onSearchTextChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить поле поиска", tint = Theme.L.textColor)
                }
            } else {
                IconButton(onClick = { onSearch(); keyboard?.hide() }) {
                    Icon(Icons.Default.Search, contentDescription = "Искать", tint = Theme.L.textColor)
                }
            }
        },
        keyboardOptions = IncognitoKeyboard.options(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(); keyboard?.hide() })
    )
}

@Composable
private fun AlbumSearchSectionBlock(
    section: Landing_page_albumSection,
    screenWidth: androidx.compose.ui.unit.Dp,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit
) {
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
                    onClick = { onAlbumClick(album.id.toLong()) }
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


@Stable
class ScreenLAlbumSearchSM @Inject constructor(
    val luscious: Luscious
) : ScreenModel {

    val state = LazyListState()

    val searchText = MutableStateFlow("")
    val result = MutableStateFlow<Landing_page_albumType?>(null)
    val isLoading = MutableStateFlow(false)

    private var searchJob: Job? = null

    fun search() {
        val query = searchText.value.trim()
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = screenModelScope.launch {
            isLoading.value = true
            try {
                result.value = withContext(Dispatchers.IO) {
                    luscious.getLandingPageAlbumSearch(query).getOrElse {
                        Timber.e(it, "ScreenLAlbumSearchSM search")
                        null
                    }
                }
            } finally {
                if (searchJob === coroutineContext[Job]) {
                    isLoading.value = false
                }
            }
        }
    }

    fun createFilter(section: Landing_page_albumSection): AlbumListFilter =
        createAlbumSearchFilter(section, searchText.value)

    override fun onDispose() {
        super.onDispose()
        searchJob?.cancel()
        isLoading.value = false
        Timber.d("ScreenLAlbumSearchSM onDispose")
    }
}


internal fun createAlbumSearchFilter(section: Landing_page_albumSection, query: String): AlbumListFilter {
    val albumType = when (section.title) {
        "Manga" -> AlbumType.Manga
        "Picture Sets" -> AlbumType.Pictures
        else -> AlbumType.Pictures
    }

    return AlbumListFilter(
        display = "search_score",
        album_type = albumType,
        content_id = ContentId.All,
        searchQuery = query.trim()
    )
}

internal fun hasNoSearchResults(result: Landing_page_albumType?): Boolean {
    if (result == null) return false
    val sections = result.sections
    return sections.isNullOrEmpty() || sections.all { it.items.isEmpty() }
}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumSearch {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLAlbumSearchSM::class)
    abstract fun bindHiltSearchScreenModelFactory(hiltListScreenModel: ScreenLAlbumSearchSM): ScreenModel
}
