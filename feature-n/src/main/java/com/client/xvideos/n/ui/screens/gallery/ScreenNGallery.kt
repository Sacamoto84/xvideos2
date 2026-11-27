package com.client.xvideos.n.ui.screens.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.share.useCaseShareText
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.atom.NGalleryTopBar
import com.client.xvideos.n.ui.molecule.NCollectionDialogs
import com.client.xvideos.n.ui.molecule.NPictureItemExpandMenu
import com.client.xvideos.n.ui.molecule.NRequestChallengeDialog
import com.client.xvideos.n.ui.screens.gallery.model.NGallerySuccessActions
import com.client.xvideos.n.ui.screens.gallery.molecule.NGallerySuccessContent
import com.client.xvideos.n.ui.screens.reader.N_FullScreenReader
import com.client.xvideos.n.ui.screens.tag.ScreenNTag
import java.util.UUID

/**
 * Экран информации о манге / галерее nhentai (аналог ScreenLAlbum).
 */
class ScreenNGallery(val galleryId: Long) : Screen {

    override val key: ScreenKey = "NGallery:$galleryId:${UUID.randomUUID()}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenNGallerySM, ScreenNGallerySM.Factory> { it.create(galleryId) }

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }
        BackHandler(onBack = onBack)

        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val localFavorites by vm.localFavorites.collectAsStateWithLifecycle()
        val serverFavorite by vm.serverFavorite.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()
        val favoriteChallenge by vm.favoriteChallenge.collectAsStateWithLifecycle()

        val onRead: (Int) -> Unit = remember(navigator, galleryId) {
            { pageNumber ->
                navigator.push(N_FullScreenReader(galleryId, pageNumber))
            }
        }

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { nextGalleryId ->
                navigator.push(ScreenNGallery(nextGalleryId))
            }
        }

        val onTagClick: (NTag) -> Unit = remember(navigator) {
            { tag ->
                navigator.push(ScreenNTag(tag))
            }
        }

        val context = LocalContext.current
        val onShareClick: () -> Unit = remember(galleryId, context) {
            {
                val currentTitle = (uiState as? NGalleryUiState.Success)?.gallery?.title
                useCaseShareText(
                    context = context,
                    text = "https://nhentai.net/g/$galleryId/",
                    subject = currentTitle
                )
            }
        }

        val pageMenuContent: @Composable (NPage, Int) -> Unit = { page, _ ->
            val successGallery = (uiState as? NGalleryUiState.Success)?.gallery
            if (successGallery != null) {
                val picture = remember(successGallery, page) {
                    NPicture.fromPage(successGallery.id, successGallery.mediaId, successGallery.title, page)
                }
                NPictureItemExpandMenu(
                    picture = picture,
                    isLiked = vm.isPageLiked(page.pageNumber),
                    onSetLiked = vm::setLiked,
                    onAddToCollection = { vm.openAddToCollection(picture) }
                )
            }
        }

        ScreenNGalleryContent(
            galleryId = galleryId,
            uiState = uiState,
            columns = columns,
            isFavorite = localFavorites?.any { it.id == galleryId } == true,
            canToggleFavorite = localFavorites != null,
            onBack = onBack,
            onRead = onRead,
            onFavoriteClick = vm::toggleFavorite,
            serverFavorite = serverFavorite,
            onServerFavoriteClick = vm::toggleServerFavorite,
            onGalleryClick = onGalleryClick,
            onShareClick = onShareClick,
            onTagClick = onTagClick,
            onRetry = vm::loadGallery,
            pageMenuContent = pageMenuContent,
            gridState = vm.pagesPosition.rememberState()
        )

        if (uiState is NGalleryUiState.Success) {
            favoriteChallenge?.let {
                NRequestChallengeDialog(
                    challenge = it,
                    onDismiss = { vm.finishFavoriteChallenge(verified = false) },
                    onVerified = { vm.finishFavoriteChallenge(verified = true) }
                )
            }
        }

        NCollectionDialogs(
            isSelectOpen = vm.isSelectCollectionOpen,
            isCreateOpen = vm.isCreateCollectionOpen,
            collections = vm.saved.collection.collections,
            onDismiss = {
                vm.isSelectCollectionOpen = false
                vm.isCreateCollectionOpen = false
            },
            onOpenCreate = {
                vm.isSelectCollectionOpen = false
                vm.isCreateCollectionOpen = true
            },
            onCreateCollection = { vm.addToCollection(it, create = true) },
            onSelectCollection = { vm.addToCollection(it) },
            coverUrlProvider = vm::getCollectionCoverUrl
        )
    }
}

@Composable
fun ScreenNGalleryContent(
    galleryId: Long,
    uiState: NGalleryUiState,
    columns: Int,
    onBack: () -> Unit,
    onRead: (Int) -> Unit,
    onGalleryClick: (Long) -> Unit,
    onTagClick: (NTag) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    serverFavorite: NServerFavoriteState = NServerFavoriteState(),
    onServerFavoriteClick: () -> Unit = {},
    onShareClick: (() -> Unit)? = null,
    pageMenuContent: (@Composable (NPage, Int) -> Unit)? = null,
    gridState: LazyGridState = rememberLazyGridState(),
    canToggleFavorite: Boolean = true
) {
    val topInset = getTopInsetDp()
    val title = (uiState as? NGalleryUiState.Success)?.gallery?.title ?: "Манга #$galleryId"
    var dismissedChallengeUrl by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            NGalleryTopBar(
                title = title,
                onBack = onBack,
                onShareClick = onShareClick,
                onRefresh = onRetry.takeIf { uiState is NGalleryUiState.Success },
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
                        TextButton(onClick = onRetry) { Text("Повторить") }
                    }
                }
                is NGalleryUiState.CloudflareRequired -> {
                    NCloudflareBanner(
                        onVerifyClick = { dismissedChallengeUrl = null },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    if (dismissedChallengeUrl != uiState.challenge.url) {
                        NRequestChallengeDialog(
                            challenge = uiState.challenge,
                            onDismiss = { dismissedChallengeUrl = uiState.challenge.url },
                            onVerified = onRetry
                        )
                    }
                }
                is NGalleryUiState.Success -> {
                    val actions = remember(onFavoriteClick, onServerFavoriteClick, onTagClick, onGalleryClick, onRead) {
                        NGallerySuccessActions(onFavoriteClick, onServerFavoriteClick, onTagClick, onGalleryClick, onRead)
                    }
                    NGallerySuccessContent(
                        galleryId = galleryId,
                        gallery = uiState.gallery,
                        columns = columns,
                        gridState = gridState,
                        isFavorite = isFavorite,
                        canToggleFavorite = canToggleFavorite,
                        serverFavorite = serverFavorite,
                        actions = actions,
                        pageMenuContent = pageMenuContent,
                    )
                }
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
        onBack = {},
        onRead = {},
        onGalleryClick = {},
        onTagClick = {},
        onRetry = {}
    )
}
