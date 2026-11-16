package com.client.xvideos.n.ui.screens.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.atom.NGalleryTopBar
import com.client.xvideos.n.ui.molecule.NCloudflareChallengeDialog
import com.client.xvideos.n.ui.molecule.NGalleryHeader
import com.client.xvideos.n.ui.molecule.NGalleryPagesGrid
import com.client.xvideos.n.ui.molecule.NGalleryRelatedStrip
import com.client.xvideos.n.ui.screens.reader.N_FullScreenReader
import com.client.xvideos.n.ui.screens.tag.ScreenNTag

/**
 * Экран информации о манге / галерее nhentai (аналог ScreenLAlbum).
 */
class ScreenNGallery(val galleryId: Long) : Screen {

    override val key: ScreenKey = "NGallery:$galleryId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenNGallerySM, ScreenNGallerySM.Factory> { factory ->
            factory.create(galleryId)
        }

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }
        BackHandler(onBack = onBack)

        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val isFavorite by vm.isFavorite.collectAsStateWithLifecycle()
        val challengeUrl by NCloudflareState.challengeUrl.collectAsStateWithLifecycle()

        val onRead: (NGallery, Int) -> Unit = remember(navigator) {
            { gallery, pageIndex ->
                navigator.push(N_FullScreenReader(gallery = gallery, initialPage = pageIndex))
            }
        }

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { nextGalleryId ->
                navigator.push(ScreenNGallery(nextGalleryId))
            }
        }

        val onRandomClick: () -> Unit = remember(navigator, vm) {
            {
                vm.openRandomGallery { randomId ->
                    navigator.push(ScreenNGallery(randomId))
                }
            }
        }

        val onTagClick: (NTag) -> Unit = remember(navigator) {
            { tag ->
                navigator.push(ScreenNTag(tag))
            }
        }

        ScreenNGalleryContent(
            galleryId = galleryId,
            uiState = uiState,
            columns = vm.columns,
            challengeUrl = challengeUrl,
            isFavorite = isFavorite,
            onBack = onBack,
            onRead = onRead,
            onFavoriteClick = vm::toggleFavorite,
            onGalleryClick = onGalleryClick,
            onRandomClick = onRandomClick,
            onTagClick = onTagClick,
            onRetry = vm::loadGallery
        )
    }
}

@Composable
fun ScreenNGalleryContent(
    galleryId: Long,
    uiState: NGalleryUiState,
    columns: Int,
    challengeUrl: String?,
    onBack: () -> Unit,
    onRead: (NGallery, Int) -> Unit,
    onGalleryClick: (Long) -> Unit,
    onRandomClick: () -> Unit,
    onTagClick: (NTag) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    val topInset = getTopInsetDp()
    val title = (uiState as? NGalleryUiState.Success)?.gallery?.title ?: "Манга #$galleryId"

    Scaffold(
        topBar = {
            NGalleryTopBar(
                title = title,
                onBack = onBack,
                onRandomClick = onRandomClick,
                topInset = topInset
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Theme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is NGalleryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFED2553)
                    )
                }
                is NGalleryUiState.Error -> {
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
                is NGalleryUiState.CloudflareRequired -> {
                    NCloudflareBanner(
                        onVerifyClick = onRetry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is NGalleryUiState.Success -> {
                    val gallery = uiState.gallery
                    NGalleryPagesGrid(
                        pages = gallery.pages,
                        mediaId = gallery.mediaId,
                        columns = columns,
                        onPageClick = { pageIndex -> onRead(gallery, pageIndex) },
                        header = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                NGalleryHeader(
                                    gallery = gallery,
                                    isFavorite = isFavorite,
                                    onReadClick = { onRead(gallery, 0) },
                                    onFavoriteClick = onFavoriteClick,
                                    onTagClick = onTagClick
                                )
                                NGalleryRelatedStrip(
                                    relatedGalleries = gallery.relatedGalleries,
                                    onGalleryClick = onGalleryClick
                                )
                            }
                        },
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
    }
}

@Preview
@Composable
private fun ScreenNGalleryContentPreview() {
    ScreenNGalleryContent(
        galleryId = 123456L,
        uiState = NGalleryUiState.Loading,
        columns = 3,
        challengeUrl = null,
        onBack = {},
        onRead = { _, _ -> },
        onGalleryClick = {},
        onRandomClick = {},
        onTagClick = {},
        onRetry = {}
    )
}
