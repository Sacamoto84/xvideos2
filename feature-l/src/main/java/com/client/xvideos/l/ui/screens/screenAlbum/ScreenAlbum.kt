package com.client.xvideos.l.ui.screens.screenAlbum

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import androidx.compose.ui.unit.Dp
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.model.isAnimatedMedia
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Audience
import com.client.xvideos.l.model.Genre
import com.client.xvideos.l.net.AlbumInfo
import com.client.xvideos.l.net.AlbumPicsDetails
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.screens.albumLandingTag.ScreenLAlbumLandingTag
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoAudiences
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonSaveAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonServerFavorite
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonShareAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoFilterButton
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoGreeting
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoTags
import com.client.xvideos.l.ui.screens.screenAlbum.dialog.AlbumDialogDeleteAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import com.client.xvideos.common.navigation.rememberNavigationDepth
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import timber.log.Timber
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ALBUM_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

internal fun formatEpochSeconds(seconds: Double, zoneId: ZoneId = ZoneId.systemDefault()): String? {
    if (!seconds.isFinite() || seconds <= 0.0) return null
    return try {
        Instant.ofEpochSecond(seconds.toLong())
            .atZone(zoneId)
            .format(ALBUM_DATE_FORMATTER)
    } catch (_: DateTimeException) {
        null
    }
}

class ScreenLAlbum(val idAlbum: Long) : Screen {

    override val key: ScreenKey = "LAlbum:$idAlbum"

    @OptIn(ExperimentalZoomableApi::class, ExperimentalMaterial3Api::class)
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @Composable
    override fun Content() {

        rememberNavigationDepth().depth = 100

        val navigator = LocalNavigator.currentOrThrow

        val vm = getScreenModel<ScreenLAlbumSM, ScreenLAlbumSM.Factory> { factory -> factory.create(idAlbum) }

        /**  ➜ сюда запоминаем элемент, который пользователь хочет удалить  */
        var itemPendingDelete by remember { mutableStateOf<AlbumDetails?>(null) }

        // Активен только когда НЕ открыта полноэкранная картинка — в этом случае
        // back перехватывает L_FullScreenImage (закрывает картинку), и выход из альбома не происходит.
        BackHandler(enabled = vm.host.selectedImage == null && itemPendingDelete != null) {
            itemPendingDelete = null
        }
        BackHandler(enabled = vm.host.selectedImage == null && itemPendingDelete == null && vm.showOnlyAnimated) {
            vm.showOnlyAnimated = false
        }
        BackHandler(enabled = vm.host.selectedImage == null && itemPendingDelete == null && !vm.showOnlyAnimated) {
            navigator.pop()
        }

        val topInset = getTopInsetDp()
        val album by vm.albumInfo.collectAsStateWithLifecycle()

        /* ---------- Диалог подтверждения ---------- */
        itemPendingDelete?.let { pending ->
            AlbumDialogDeleteAlbum(
                pending = pending,
                onDismiss = { itemPendingDelete = null },
                onClick = {
                    vm.saved.albums.remove(pending)
                    itemPendingDelete = null
                }
            )
        }
        /* ---------- /Диалог ---------- */

        vm.p2pAlbumSource?.let { source ->
            // Навигация — side effect, нельзя звать прямо из композиции.
            LaunchedEffect(source) {
                navigator.push(ScreenP2pSend(source))
                vm.dismissP2pAlbum()
            }
        }

        val currentAlbum = album
        if (currentAlbum != null) {
            ScreenLAlbumBody(
                album = currentAlbum,
                vm = vm,
                navigator = navigator,
                topInset = topInset,
                idAlbum = idAlbum,
                onRequestDelete = { itemPendingDelete = it }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Theme.L.red)
            }
        }
    }
}

@OptIn(ExperimentalZoomableApi::class, ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
private fun ScreenLAlbumBody(
    album: AlbumInfo,
    vm: ScreenLAlbumSM,
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

    val saved by remember(parsed?.id) {
        derivedStateOf {
            val currentParsed = parsed
            currentParsed != null && currentParsed.id.isNotBlank() && vm.saved.albums.list.any { it.id == currentParsed.id }
        }
    }

    val albumPicsDetails = album.albumPicsDetails
    val hasAnimatedItems by remember(albumPicsDetails) {
        derivedStateOf {
            albumPicsDetails.pics.any { it.isAnimatedMedia() }
        }
    }
    val showInitialItemsLoading =
        albumPicsDetails.isPageRequestInFlight && vm.host.filteredPic.isEmpty()

    LaunchedEffect(vm.showOnlyAnimated, parsed, albumPicsDetails.pics.size) {
        Timber.d("ScreenLAlbum LaunchedEffect animated = ${vm.showOnlyAnimated} size:${albumPicsDetails.pics.size}")
        if (parsed == null) return@LaunchedEffect

        val allPics = albumPicsDetails.pics.toList()
        val newFilteredAnimatedPics = allPics.filter { it.isAnimatedMedia() }

        if (vm.showOnlyAnimated) {
            vm.host.replaceFilteredPictures(newFilteredAnimatedPics)
        } else {
            vm.host.replaceFilteredPictures(allPics)
        }
    }

    LaunchedEffect(parsed?.likeStatus) {
        vm.syncServerFavoriteStatus(parsed?.likeStatus)
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
    val onRetryFailedPages = remember(vm) {
        { vm.retryFailedAlbumPages() }
    }
    val onRefresh = remember(haptic, vm) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            vm.refresh()
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
                host = vm.host,
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
                                vm = vm,
                                hasAnimatedItems = hasAnimatedItems,
                                albumPicsDetails = albumPicsDetails,
                                onGenreClick = onGenreClick,
                                onAudienceClick = onAudienceClick,
                                onTagClick = onTagClick,
                                onRequestDelete = onRequestDelete,
                                onRetryFailedPages = onRetryFailedPages
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

@Composable
private fun ScreenLAlbumDetailsHeader(
    parsed: AlbumDetails,
    idAlbum: Long,
    saved: Boolean,
    vm: ScreenLAlbumSM,
    hasAnimatedItems: Boolean,
    albumPicsDetails: AlbumPicsDetails,
    onGenreClick: (Genre) -> Unit,
    onAudienceClick: (Audience) -> Unit,
    onTagClick: (String) -> Unit,
    onRequestDelete: (AlbumDetails) -> Unit,
    onRetryFailedPages: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        UrlImage(
            parsed.cover?.url.orEmpty(),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .size(72.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(parsed.title, color = Theme.L.textColor, style = Theme.L.Type.rowTitle)
            Text("${parsed.number_of_animated_pictures} gifs / ${parsed.number_of_pictures} pictures", color = Theme.L.textColor)
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    val strId = remember(idAlbum) {
        buildAnnotatedString {
            withStyle(style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle()) { append("Id: ") }
            withStyle(style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(idAlbum.toString()) }
        }
    }
    Text(strId, color = Theme.L.textColor)

    Spacer(modifier = Modifier.height(4.dp))

    val textCreated = remember(parsed.created) { formatEpochSeconds(parsed.created) }
    val textModified = remember(parsed.modified) { formatEpochSeconds(parsed.modified) }

    val strCreated = remember(textCreated) {
        textCreated?.let { created ->
            buildAnnotatedString {
                withStyle(style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle()) { append("Created: ") }
                withStyle(style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(created) }
            }
        }
    }
    if (strCreated != null) {
        Text(strCreated, color = Theme.L.textColor)
    }

    val strModified = remember(textModified) {
        textModified?.let { modified ->
            buildAnnotatedString {
                withStyle(style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle()) { append("Modified: ") }
                withStyle(style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(modified) }
            }
        }
    }
    if (strModified != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(strModified, color = Theme.L.textColor)
    }

    Spacer(modifier = Modifier.height(4.dp))
    AlbumInfoGreeting(parsed, onGenreClick)
    AlbumInfoAudiences(parsed, onAudienceClick)
    val activeTags = remember(parsed.tags) {
        parsed.tags.reversed().filter { it.count > 0 }
    }
    AlbumInfoTags({ activeTags }, onTagClick)
    AlbumInfoButtonSaveAlbum(saved, onClick = {
        if (!saved) {
            vm.saveAlbum()
        } else {
            onRequestDelete(parsed)
        }
    })
    AlbumInfoButtonServerFavorite(
        isFavorite = vm.isServerFavorite ?: (parsed.likeStatus.orEmpty().isNotBlank() && parsed.likeStatus != "none" && parsed.likeStatus != "dislike"),
        isLoading = vm.isServerFavoriteLoading,
        onClick = { vm.toggleServerFavorite(parsed) }
    )
    AlbumInfoButtonShareAlbum(onClick = { vm.shareAlbumP2p(parsed) })
    AlbumInfoFilterButton(
        parsed = parsed,
        checked = vm.showOnlyAnimated,
        hasAnimatedItems = hasAnimatedItems,
        onCheckedChange = { vm.showOnlyAnimated = it }
    )
    LAlbumNetworkIssuePanel(
        albumPicsDetails = albumPicsDetails,
        onRetryFailedPages = onRetryFailedPages
    )
}

@Composable
private fun ScreenLAlbumErrorHeader(
    loadError: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Не удалось загрузить данные альбома",
            color = Theme.L.textColor,
            style = Theme.L.Type.rowTitle
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = loadError,
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Theme.L.red)
            ) {
                Text("Повторить", color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Theme.tabLevel1)
            ) {
                Text("Назад", color = Theme.L.textColor)
            }
        }
    }
}

@Composable
private fun ScreenLAlbumPreviewBody(
    title: String,
    animatedCount: Int,
    pictureCount: Int,
    percentLoad: Float,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (percentLoad != 1.0f) {
                LinearProgressIndicator(
                    progress = { percentLoad },
                    modifier = Modifier.fillMaxWidth(),
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                )
            }
        },
        containerColor = Theme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 4.dp)
        ) {
            Row {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .size(72.dp)
                        .background(Theme.tabLevel1)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(title, color = Theme.L.textColor, style = Theme.L.Type.rowTitle)
                    Text(
                        "$animatedCount gifs / $pictureCount pictures",
                        color = Theme.L.textColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626, widthDp = 360, heightDp = 720)
@Composable
private fun ScreenLAlbumPreview() {
    ScreenLAlbumPreviewBody(
        title = "Example Album Title",
        animatedCount = 12,
        pictureCount = 48,
        percentLoad = 0.4f,
    )
}

