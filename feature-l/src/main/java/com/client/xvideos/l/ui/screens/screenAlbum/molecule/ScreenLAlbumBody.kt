package com.client.xvideos.l.ui.screens.screenAlbum.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Audience
import com.client.xvideos.l.model.Genre
import com.client.xvideos.l.model.isAnimatedMedia
import com.client.xvideos.l.net.AlbumInfo
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.l.ui.screens.albumLandingTag.ScreenLAlbumLandingTag
import com.client.xvideos.l.ui.screens.screenAlbum.albumListFilterForAudience
import com.client.xvideos.l.ui.screens.screenAlbum.albumListFilterForGenre
import com.client.xvideos.l.ui.screens.screenAlbum.model.LAlbumActions
import com.client.xvideos.l.ui.screens.screenAlbum.model.LAlbumHeaderState
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ScreenLAlbumBody(
    album: AlbumInfo,
    host: LazyRowPictureDetailsHost,
    savedAlbums: List<AlbumDetails>,
    state: LAlbumHeaderState,
    actions: LAlbumActions,
    navigator: Navigator,
    topInset: Dp,
    idAlbum: Long,
    onRequestDelete: (AlbumDetails) -> Unit
) {
    val parsed by album.albumInfo.collectAsStateWithLifecycle()
    val loadError by album.loadError.collectAsStateWithLifecycle()
    val isLoading by album.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by album.isRefreshing.collectAsStateWithLifecycle()

    val pullToRefreshState = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current

    val saved by remember(parsed?.id, savedAlbums) {
        derivedStateOf {
            val currentParsed = parsed
            currentParsed != null && currentParsed.id.isNotBlank() && savedAlbums.any { it.id == currentParsed.id }
        }
    }

    val albumPicsDetails = album.albumPicsDetails
    val hasAnimatedItems by remember(albumPicsDetails) {
        derivedStateOf {
            albumPicsDetails.pics.any { it.isAnimatedMedia() }
        }
    }
    val showInitialItemsLoading =
        albumPicsDetails.isPageRequestInFlight && host.filteredPic.isEmpty()

    LaunchedEffect(state.showOnlyAnimated, parsed, albumPicsDetails.pics.size) {
        Timber.d("ScreenLAlbum LaunchedEffect animated = ${state.showOnlyAnimated} size:${albumPicsDetails.pics.size}")
        if (parsed == null) return@LaunchedEffect

        val allPics = albumPicsDetails.pics.toList()
        val newFilteredAnimatedPics = allPics.filter { it.isAnimatedMedia() }

        if (state.showOnlyAnimated) {
            host.replaceFilteredPictures(newFilteredAnimatedPics)
        } else {
            host.replaceFilteredPictures(allPics)
        }
    }

    LaunchedEffect(parsed?.likeStatus) {
        actions.onSyncServerFavoriteStatus(parsed?.likeStatus)
    }

    val onGenreClick = remember(navigator) {
        { genre: Genre ->
            navigator.push(
                L_ScreenAlbumList.create(filter = albumListFilterForGenre(genre), title = "Genre: ${genre.title}")
            )
        }
    }
    val onAudienceClick = remember(navigator) {
        { audience: Audience ->
            navigator.push(
                L_ScreenAlbumList.create(filter = albumListFilterForAudience(audience), title = "Audience: ${audience.title}")
            )
        }
    }
    val onTagClick = remember(navigator) {
        { tag: String ->
            navigator.push(ScreenLAlbumLandingTag(tag))
        }
    }
    val onRefresh = remember(haptic, actions) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            actions.onRefresh()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (albumPicsDetails.percentLoad != 1.0f) {
                LinearProgressIndicator(
                    progress = { albumPicsDetails.percentLoad },
                    modifier = Modifier.fillMaxWidth(),
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                )
            }
        },
        containerColor = Theme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
            state = pullToRefreshState,
            indicator = {
                Indicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topInset),
                    isRefreshing = isRefreshing,
                    containerColor = Theme.tabLevel1,
                    color = Theme.L.red,
                    state = pullToRefreshState
                )
            }
        ) {
            L_LazyRowPictureDetails(
                host = host,
                expandMenu = ExpandMenuType.ALBUM,
                showInitialLoading = showInitialItemsLoading,
                itemBefore = {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(topInset)) { }

                        val currentParsed = parsed
                        if (currentParsed != null && currentParsed.id.isNotBlank()) {
                            ScreenLAlbumDetailsHeader(
                                parsed = currentParsed,
                                idAlbum = idAlbum,
                                saved = saved,
                                state = state,
                                actions = actions,
                                hasAnimatedItems = hasAnimatedItems,
                                albumPicsDetails = albumPicsDetails,
                                onGenreClick = onGenreClick,
                                onAudienceClick = onAudienceClick,
                                onTagClick = onTagClick,
                                onRequestDelete = onRequestDelete
                            )
                        } else if (loadError != null) {
                            ScreenLAlbumErrorHeader(
                                loadError = loadError.orEmpty(),
                                onRetry = { album.retry() },
                                onBack = { navigator.pop() }
                            )
                        } else if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Theme.L.red)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Preview
@Composable
private fun ScreenLAlbumBodyPreview() {
    ScreenLAlbumErrorHeader(
        loadError = "Preview Mode",
        onRetry = {},
        onBack = {}
    )
}
