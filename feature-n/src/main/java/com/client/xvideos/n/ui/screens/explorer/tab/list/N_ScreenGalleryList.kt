package com.client.xvideos.n.ui.screens.explorer.tab.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.atom.NFeedTopBar
import com.client.xvideos.n.ui.molecule.NAlbumListBottomBar
import com.client.xvideos.n.ui.molecule.NCloudflareChallengeDialog
import com.client.xvideos.n.ui.molecule.NGalleryGrid
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Вкладка ленты свежей манги раздела nhentai.
 */
object N_ScreenGalleryList : Screen {

    private fun readResolve(): Any = N_ScreenGalleryList

    override val key: ScreenKey = "N_ScreenGalleryList"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_ScreenGalleryListSM>()

        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val currentPage by vm.currentPage.collectAsStateWithLifecycle()
        val totalPages by vm.totalPages.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()
        val challengeUrl by NCloudflareState.challengeUrl.collectAsStateWithLifecycle()

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }

        val onRandomClick: () -> Unit = remember(navigator, vm) {
            {
                vm.openRandomGallery { randomId ->
                    navigator.push(ScreenNGallery(randomId))
                }
            }
        }

        N_ScreenGalleryListContent(
            uiState = uiState,
            currentPage = currentPage,
            totalPages = totalPages,
            columns = columns,
            challengeUrl = challengeUrl,
            onGalleryClick = onGalleryClick,
            onRandomClick = onRandomClick,
            onPageChange = vm::loadPage,
            onRetry = { vm.loadPage(currentPage) }
        )
    }
}

@Composable
fun N_ScreenGalleryListContent(
    uiState: NGalleryListUiState,
    currentPage: Int,
    totalPages: Int,
    columns: Int,
    challengeUrl: String?,
    onGalleryClick: (Long) -> Unit,
    onRandomClick: () -> Unit,
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
        NFeedTopBar(
            onRandomClick = onRandomClick,
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
                        contentPadding = PaddingValues(8.dp),
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
private fun N_ScreenGalleryListContentPreview() {
    N_ScreenGalleryListContent(
        uiState = NGalleryListUiState.Loading,
        currentPage = 1,
        totalPages = 50,
        columns = 2,
        challengeUrl = null,
        onGalleryClick = {},
        onRandomClick = {},
        onPageChange = {},
        onRetry = {}
    )
}
