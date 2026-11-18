package com.client.xvideos.n.ui.screens.explorer.tab.tophits

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
import com.client.xvideos.n.model.NSortType
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.atom.NSortFilterRow
import com.client.xvideos.n.ui.molecule.NAlbumListBottomBar
import com.client.xvideos.n.ui.molecule.NCloudflareChallengeDialog
import com.client.xvideos.n.ui.molecule.NGalleryGrid
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Вкладка популярной манги (Топ альбомов).
 */
object N_ScreenGalleryTopHits : Screen {

    private fun readResolve(): Any = N_ScreenGalleryTopHits

    override val key: ScreenKey = "N_ScreenGalleryTopHits"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_ScreenGalleryTopHitsSM>()

        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val currentPage by vm.currentPage.collectAsStateWithLifecycle()
        val totalPages by vm.totalPages.collectAsStateWithLifecycle()
        val sortType by vm.sortType.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()
        val challengeUrl by NCloudflareState.challengeUrl.collectAsStateWithLifecycle()

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }

        N_ScreenGalleryTopHitsContent(
            uiState = uiState,
            selectedSort = sortType,
            currentPage = currentPage,
            totalPages = totalPages,
            columns = columns,
            challengeUrl = challengeUrl,
            onSortSelected = vm::setSortType,
            onGalleryClick = onGalleryClick,
            onPageChange = vm::loadPage,
            onRetry = { vm.loadPage(currentPage) }
        )
    }
}

@Composable
fun N_ScreenGalleryTopHitsContent(
    uiState: NGalleryListUiState,
    selectedSort: NSortType,
    currentPage: Int,
    totalPages: Int,
    columns: Int,
    challengeUrl: String?,
    onSortSelected: (NSortType) -> Unit,
    onGalleryClick: (Long) -> Unit,
    onPageChange: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        NSortFilterRow(
            selectedSort = selectedSort,
            onSortSelected = onSortSelected,
            topInset = topInset
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
                    NGalleryGrid(
                        items = uiState.items,
                        columns = columns,
                        onItemClick = onGalleryClick,
                        modifier = Modifier.fillMaxSize()
                    )
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

        if (totalPages > 1 || currentPage > 1) {
            NAlbumListBottomBar(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = onPageChange
            )
        }
    }
}

@Preview
@Composable
private fun N_ScreenGalleryTopHitsContentPreview() {
    N_ScreenGalleryTopHitsContent(
        uiState = NGalleryListUiState.Loading,
        selectedSort = NSortType.POPULAR_ALL_TIME,
        currentPage = 1,
        totalPages = 50,
        columns = 2,
        challengeUrl = null,
        onSortSelected = {},
        onGalleryClick = {},
        onPageChange = {},
        onRetry = {}
    )
}
