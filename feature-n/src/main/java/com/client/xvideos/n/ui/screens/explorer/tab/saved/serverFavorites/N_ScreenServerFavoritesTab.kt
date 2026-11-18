package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.molecule.NAlbumListBottomBar
import com.client.xvideos.n.ui.molecule.NCloudflareChallengeDialog
import com.client.xvideos.n.ui.molecule.NGalleryGrid
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.molecule.NServerFavoritesEmptyState
import com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.molecule.NServerFavoritesTopBar
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Вкладка избранного пользователя с сервера nhentai (/api/v2/favorites).
 */
object N_ScreenServerFavoritesTab : Screen {

    private fun readResolve(): Any = N_ScreenServerFavoritesTab

    override val key: ScreenKey = "N_ScreenServerFavoritesTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenNServerFavoritesSM>()

        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val currentPage by vm.currentPage.collectAsStateWithLifecycle()
        val totalPages by vm.totalPages.collectAsStateWithLifecycle()
        val isImporting by vm.isImporting.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()
        val challengeUrl by NCloudflareState.challengeUrl.collectAsStateWithLifecycle()

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }

        N_ScreenServerFavoritesTabContent(
            uiState = uiState,
            currentPage = currentPage,
            totalPages = totalPages,
            columns = columns,
            isImporting = isImporting,
            challengeUrl = challengeUrl,
            onGalleryClick = onGalleryClick,
            onPageChange = vm::loadPage,
            onImportAll = vm::importCurrentPage,
            onRetry = { vm.loadPage(currentPage) }
        )
    }
}

@Composable
fun N_ScreenServerFavoritesTabContent(
    uiState: NGalleryListUiState,
    currentPage: Int,
    totalPages: Int,
    columns: Int,
    isImporting: Boolean,
    challengeUrl: String?,
    onGalleryClick: (Long) -> Unit,
    onPageChange: (Int) -> Unit,
    onImportAll: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        val itemsCount = (uiState as? NGalleryListUiState.Success)?.items?.size ?: 0
        NServerFavoritesTopBar(
            itemsCount = itemsCount,
            onImportAll = onImportAll,
            isImporting = isImporting,
            topInset = topInset
        )

        challengeUrl?.let { url ->
            NCloudflareBanner(onVerifyClick = { NCloudflareState.triggerChallenge(url) })
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            when (uiState) {
                is NGalleryListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFED2553))
                    }
                }
                is NGalleryListUiState.Success -> {
                    if (uiState.items.isEmpty()) {
                        NServerFavoritesEmptyState()
                    } else {
                        NGalleryGrid(
                            items = uiState.items,
                            columns = columns,
                            onItemClick = onGalleryClick,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is NGalleryListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.message,
                            color = Color(0xFFED2553),
                            fontSize = 15.sp
                        )
                    }
                }
                is NGalleryListUiState.CloudflareRequired -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Требуется проверка Cloudflare",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }

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
private fun N_ScreenServerFavoritesTabContentPreview() {
    N_ScreenServerFavoritesTabContent(
        uiState = NGalleryListUiState.Success(emptyList()),
        currentPage = 1,
        totalPages = 5,
        columns = 2,
        isImporting = false,
        challengeUrl = null,
        onGalleryClick = {},
        onPageChange = {},
        onImportAll = {},
        onRetry = {}
    )
}
