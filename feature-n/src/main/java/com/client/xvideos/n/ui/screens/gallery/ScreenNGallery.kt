package com.client.xvideos.n.ui.screens.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.molecule.NCloudflareChallengeDialog
import com.client.xvideos.n.ui.molecule.NGalleryHeader
import com.client.xvideos.n.ui.molecule.NGalleryPagesGrid
import com.client.xvideos.n.ui.screens.reader.N_FullScreenReader

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
        val challengeUrl by NCloudflareState.challengeUrl.collectAsStateWithLifecycle()

        val onRead: (NGallery, Int) -> Unit = remember(navigator) {
            { gallery, pageIndex ->
                navigator.push(N_FullScreenReader(gallery = gallery, initialPage = pageIndex))
            }
        }

        val onTagClick: (NTag) -> Unit = remember(navigator) {
            { tag ->
                // Переход к поиску по тегу
            }
        }

        ScreenNGalleryContent(
            galleryId = galleryId,
            uiState = uiState,
            columns = vm.columns,
            challengeUrl = challengeUrl,
            onBack = onBack,
            onRead = onRead,
            onTagClick = onTagClick,
            onRetry = vm::loadGallery
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenNGalleryContent(
    galleryId: Long,
    uiState: NGalleryUiState,
    columns: Int,
    challengeUrl: String?,
    onBack: () -> Unit,
    onRead: (NGallery, Int) -> Unit,
    onTagClick: (NTag) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? NGalleryUiState.Success)?.gallery?.title ?: "Манга #$galleryId",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Theme.tabLevel0
                )
            )
        },
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        NGalleryHeader(
                            gallery = gallery,
                            onReadClick = { onRead(gallery, 0) },
                            onTagClick = onTagClick
                        )
                        NGalleryPagesGrid(
                            pages = gallery.pages,
                            mediaId = gallery.mediaId,
                            columns = columns,
                            onPageClick = { pageIndex -> onRead(gallery, pageIndex) },
                            modifier = Modifier.weight(1f)
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
private fun ScreenNGalleryContentPreview() {
    ScreenNGalleryContent(
        galleryId = 123456L,
        uiState = NGalleryUiState.Loading,
        columns = 3,
        challengeUrl = null,
        onBack = {},
        onRead = { _, _ -> },
        onTagClick = {},
        onRetry = {}
    )
}
