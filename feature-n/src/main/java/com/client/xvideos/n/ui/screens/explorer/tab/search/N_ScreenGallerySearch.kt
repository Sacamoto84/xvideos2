package com.client.xvideos.n.ui.screens.explorer.tab.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.atom.NSearchBar
import com.client.xvideos.n.ui.molecule.NCloudflareChallengeDialog
import com.client.xvideos.n.ui.molecule.NGalleryGrid
import com.client.xvideos.n.ui.molecule.NTagSuggestionsRow
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Вкладка поиска манги раздела nhentai с автодополнением тегов.
 */
object N_ScreenGallerySearch : Screen {

    private fun readResolve(): Any = N_ScreenGallerySearch

    override val key: ScreenKey = "N_ScreenGallerySearch"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_ScreenGallerySearchSM>()

        val query by vm.query.collectAsStateWithLifecycle()
        val tagSuggestions by vm.tagSuggestions.collectAsStateWithLifecycle()
        val isLoadingMore by vm.isLoadingMore.collectAsStateWithLifecycle()
        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()
        val challengeUrl by NCloudflareState.challengeUrl.collectAsStateWithLifecycle()

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }

        N_ScreenGallerySearchContent(
            query = query,
            tagSuggestions = tagSuggestions,
            uiState = uiState,
            columns = columns,
            challengeUrl = challengeUrl,
            isLoadingMore = isLoadingMore,
            onQueryChange = vm::setQuery,
            onSearch = vm::search,
            onTagSuggestionClick = vm::selectTagSuggestion,
            onGalleryClick = onGalleryClick,
            onLoadMore = vm::loadNextPage,
            onRetry = vm::search
        )
    }
}

@Composable
fun N_ScreenGallerySearchContent(
    query: String,
    tagSuggestions: List<NTag>,
    uiState: NGalleryListUiState,
    columns: Int,
    challengeUrl: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onTagSuggestionClick: (NTag) -> Unit,
    onGalleryClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null
) {
    val topInset = getTopInsetDp()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        NSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            topInset = topInset
        )

        NTagSuggestionsRow(
            suggestions = tagSuggestions,
            onTagClick = onTagSuggestionClick
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            when (uiState) {
                is NGalleryListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFED2553)
                    )
                }
                is NGalleryListUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.message,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        NCloudflareBanner(onVerifyClick = onRetry)
                    }
                }
                is NGalleryListUiState.CloudflareRequired -> {
                    NCloudflareBanner(
                        onVerifyClick = onRetry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is NGalleryListUiState.Success -> {
                    if (uiState.items.isEmpty()) {
                        Text(
                            text = if (query.isEmpty()) "Введите запрос для поиска" else "Ничего не найдено",
                            color = Color(0xFF888888),
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        NGalleryGrid(
                            items = uiState.items,
                            columns = columns,
                            onItemClick = onGalleryClick,
                            isLoadingMore = isLoadingMore,
                            onLoadMore = onLoadMore,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Модальный диалог проверки Cloudflare
            challengeUrl?.let { url ->
                NCloudflareChallengeDialog(
                    url = url,
                    onDismiss = { NCloudflareState.onChallengeDismissed() },
                    onSuccess = { cookies ->
                        NCloudflareState.onChallengeCompleted(cookies)
                        onRetry()
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun N_ScreenGallerySearchContentPreview() {
    N_ScreenGallerySearchContent(
        query = "",
        tagSuggestions = emptyList(),
        uiState = NGalleryListUiState.Success(emptyList()),
        columns = 2,
        challengeUrl = null,
        onQueryChange = {},
        onSearch = {},
        onTagSuggestionClick = {},
        onGalleryClick = {},
        onRetry = {}
    )
}
