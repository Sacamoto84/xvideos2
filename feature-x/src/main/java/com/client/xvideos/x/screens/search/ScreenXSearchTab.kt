package com.client.xvideos.x.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.channel.ScreenX_Channel
import com.client.xvideos.x.screens.search.molecule.SearchHistoryView
import com.client.xvideos.x.screens.search.molecule.SearchResultsView
import com.client.xvideos.x.screens.search.molecule.SearchSuggestionsView
import com.client.xvideos.x.screens.search.molecule.SearchTopBar
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Pornstar

/**
 * Автономный Voyager-экран вкладки поиска.
 */
class ScreenXSearchTab : Screen {
    override val key: ScreenKey = "ScreenXSearchTab"

    @Composable
    override fun Content() {
        val vm: ScreenXSearchSM = getScreenModel()
        val navigator = LocalNavigator.currentOrThrow
        ScreenXSearchTabContent(
            vm = vm,
            onOpenVideoPlayer = { item -> navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item)) },
            onOpenChannel = { slug, isModel -> navigator.push(ScreenX_Channel(slug = slug, isModel = isModel)) }
        )
    }
}

/**
 * Контент вкладки поиска раздела X.
 *
 * Отображает поле ввода, анимированные подсказки автодополнения (фразы, авторы, каналы),
 * локальную историю предыдущих запросов и пагинированную сетку найденных видеороликов.
 *
 * @param vm [ScreenXSearchSM] модели экрана поиска.
 * @param onOpenVideoPlayer Колбэк открытия видеоплеера по карточке ролика.
 * @param onOpenChannel Колбэк перехода на экран профиля автора или студии.
 */
@Composable
fun ScreenXSearchTabContent(
    vm: ScreenXSearchSM,
    onOpenVideoPlayer: (ItemsX) -> Unit,
    onOpenChannel: (slug: String, isModel: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val uiMode by vm.uiMode.collectAsStateWithLifecycle()
    val isSuggestLoading by vm.isSuggestLoading.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val isVideoLoading by vm.isVideoLoading.collectAsStateWithLifecycle()
    val videoItems by vm.videoItems.collectAsStateWithLifecycle()
    val isSearchError by vm.isSearchError.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    val onSearchKeyword: (String) -> Unit = remember(vm, focusManager) {
        { keyword ->
            focusManager.clearFocus()
            vm.searchKeyword(keyword)
        }
    }

    val onQueryChange: (String) -> Unit = remember(vm) {
        { newQuery -> vm.updateQuery(newQuery) }
    }

    val onClearQuery: () -> Unit = remember(vm) {
        { vm.clearQuery() }
    }

    val onBackClick: () -> Unit = remember(vm) {
        { vm.onBackPress() }
    }

    val onModelClick: (Pornstar) -> Unit = remember(onOpenChannel) {
        { star ->
            val slug = star.profilePath.trim().removePrefix("/").removePrefix("profiles/").removePrefix("channels/")
            if (slug.isNotBlank()) {
                onOpenChannel(slug, true)
            }
        }
    }

    val onChannelClick: (Channel) -> Unit = remember(onOpenChannel) {
        { channel ->
            val slug = channel.profilePath.trim().removePrefix("/").removePrefix("profiles/").removePrefix("channels/")
            if (slug.isNotBlank()) {
                onOpenChannel(slug, false)
            }
        }
    }

    BackHandler(enabled = uiMode == SearchUiMode.RESULTS || query.isNotEmpty()) {
        vm.onBackPress()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1B))
    ) {
        SearchTopBar(
            query = query,
            uiMode = uiMode,
            isLoading = isSuggestLoading || isVideoLoading,
            onQueryChange = onQueryChange,
            onSearch = onSearchKeyword,
            onClear = onClearQuery,
            onBack = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (uiMode) {
                SearchUiMode.HISTORY -> {
                    SearchHistoryView(
                        history = history,
                        onSelect = onSearchKeyword,
                        onDelete = { vm.deleteHistoryItem(it) },
                        onClearAll = { vm.clearAllHistory() }
                    )
                }
                SearchUiMode.SUGGESTIONS -> {
                    SearchSuggestionsView(
                        suggestions = suggestions,
                        isLoading = isSuggestLoading,
                        onSelectKeyword = onSearchKeyword,
                        onSelectModel = onModelClick,
                        onSelectChannel = onChannelClick
                    )
                }
                SearchUiMode.RESULTS -> {
                    val onRetry = remember(vm) { { vm.retrySearch() } }
                    val isFavoriteFn: (Long) -> Boolean = remember(vm) { { id -> vm.isFavorite(id) } }
                    val onFavAdd: (ItemsX) -> Unit = remember(vm) { { item -> vm.addFavorite(item) } }
                    val onFavRemove: (ItemsX) -> Unit = remember(vm) { { item -> vm.removeFavorite(item) } }
                    val onDl: (ItemsX) -> Unit = remember(vm) { { item -> vm.download(item) } }
                    val onGallery: (ItemsX) -> Unit = remember(vm) { { item -> vm.saveToGallery(item) } }
                    SearchResultsView(
                        items = videoItems,
                        isLoading = isVideoLoading,
                        isError = isSearchError,
                        onRetry = onRetry,
                        isFavorite = isFavoriteFn,
                        onFavoriteAdd = onFavAdd,
                        onFavoriteRemove = onFavRemove,
                        onDownload = onDl,
                        onSaveToGallery = onGallery,
                        openVideoPlayer = onOpenVideoPlayer
                    )
                }
            }
        }
    }
}

@Composable
fun X_SearchContent(
    vm: ScreenXSearchSM,
    onOpenVideoPlayer: (ItemsX) -> Unit,
    onOpenChannel: (slug: String, isModel: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenXSearchTabContent(
        vm = vm,
        onOpenVideoPlayer = onOpenVideoPlayer,
        onOpenChannel = onOpenChannel,
        modifier = modifier
    )
}

@Preview
@Composable
private fun ScreenXSearchTabContentPreview() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text("Search Tab Preview", color = Color.White)
    }
}
