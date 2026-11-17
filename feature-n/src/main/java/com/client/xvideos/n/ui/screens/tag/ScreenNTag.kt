package com.client.xvideos.n.ui.screens.tag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.net.NCloudflareState
import com.client.xvideos.n.ui.atom.NCloudflareBanner
import com.client.xvideos.n.ui.atom.NGalleryTopBar
import com.client.xvideos.n.ui.molecule.NCloudflareChallengeDialog
import com.client.xvideos.n.ui.molecule.NGalleryGrid
import com.client.xvideos.n.ui.screens.explorer.tab.list.NGalleryListUiState
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery

/**
 * Экран списка работ по выбранному тегу, категории или автору.
 */
class ScreenNTag(val tag: NTag) : Screen {

    override val key: ScreenKey = "NTag:${tag.type}:${tag.id}:${tag.name}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenNTagSM, ScreenNTagSM.Factory> { factory ->
            factory.create(tag)
        }

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }
        BackHandler(onBack = onBack)

        val uiState by vm.uiState.collectAsStateWithLifecycle()
        val isLoadingMore by vm.isLoadingMore.collectAsStateWithLifecycle()
        val columns by vm.columns.collectAsStateWithLifecycle()
        val challengeUrl by NCloudflareState.challengeUrl.collectAsStateWithLifecycle()

        val onGalleryClick: (Long) -> Unit = remember(navigator) {
            { id -> navigator.push(ScreenNGallery(id)) }
        }

        ScreenNTagContent(
            tag = tag,
            uiState = uiState,
            columns = columns,
            challengeUrl = challengeUrl,
            isLoadingMore = isLoadingMore,
            onBack = onBack,
            onGalleryClick = onGalleryClick,
            onLoadMore = vm::loadNextPage,
            onRetry = vm::loadTagGalleries
        )
    }
}

@Composable
fun ScreenNTagContent(
    tag: NTag,
    uiState: NGalleryListUiState,
    columns: Int,
    challengeUrl: String?,
    onBack: () -> Unit,
    onGalleryClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null
) {
    val topInset = getTopInsetDp()
    val displayTitle = if (tag.type.isNotEmpty() && tag.type != "tag") {
        "${tag.type}: ${tag.name}"
    } else {
        tag.name
    }

    Scaffold(
        topBar = {
            NGalleryTopBar(
                title = displayTitle,
                onBack = onBack,
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
                            text = "Работ с этим тегом пока нет",
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
private fun ScreenNTagContentPreview() {
    ScreenNTagContent(
        tag = NTag(id = 1, type = "artist", name = "cool_artist", count = 42),
        uiState = NGalleryListUiState.Loading,
        columns = 2,
        challengeUrl = null,
        onBack = {},
        onGalleryClick = {},
        onRetry = {}
    )
}
