package com.client.xvideos.l.ui.screens.screenAlbum

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Audience
import com.client.xvideos.l.model.Genre
import com.client.xvideos.l.net.AlbumPicsDetails
import com.client.xvideos.l.net.LAlbumPageLoadIssue
import com.client.xvideos.l.repository.LRepositoryProtectionUiState
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.screens.albumLandingTag.ScreenLAlbumLandingTag
import com.client.xvideos.l.ui.screens.screenAlbum.dialog.AlbumDialogDeleteAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoAudiences
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonSaveAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonShareAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoFilterButton
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoGreeting
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoTags
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import com.client.xvideos.screenRoot.LocalRootScreenModel
import kotlinx.coroutines.delay
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import com.client.xvideos.l.net.graphQl.Genre as FilterGenre

class ScreenLAlbum(val idAlbum: Long) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalZoomableApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        LocalRootScreenModel.current.depthState.depth = 100

        val navigator = LocalNavigator.currentOrThrow

        val vm = getScreenModel<ScreenLAlbumSM, ScreenLAlbumSM.Factory> { factory -> factory.create(idAlbum) }

        // Виброотклик при возврате из альбома. Активен только когда НЕ открыта
        // полноэкранная картинка — в этом случае back перехватывает L_FullScreenImage
        // (закрывает картинку), и выход из альбома не происходит.
        val haptic = LocalHapticFeedback.current

        BackHandler(enabled = vm.host.selectedImage == null) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            navigator.pop()
        }

        val album = vm.albumInfo.collectAsStateWithLifecycle().value

        val parsed = vm.albumInfo.collectAsStateWithLifecycle().value?.albumInfo?.collectAsStateWithLifecycle()?.value

        val saved = vm.saved.albums.list.any { it.id == parsed?.id }

        val albumPicsDetails = album?.albumPicsDetails
        val showInitialItemsLoading =
            albumPicsDetails?.isPageRequestInFlight == true &&
                    vm.host.filteredPic.isEmpty()

        LaunchedEffect(vm.showOnlyAnimated, parsed, album?.albumPicsDetails?.pics?.size) {

            Timber.d("!!! iiii ScreenLAlbum LaunchedEffect animated = ${vm.showOnlyAnimated} size:${album?.albumPicsDetails?.pics?.size}")

            if (parsed == null) return@LaunchedEffect

            val allPics = album?.albumPicsDetails?.pics?.toList() ?: emptyList()

            val newFilteredAnimatedPics = allPics.filter { it.is_animated } //Список анимированных елементов
            val newFilteredNoAnimatedPics = allPics.filter { !it.is_animated } //Список анимированных елементов

            if (vm.showOnlyAnimated) {
                //val a = vm.host.filteredPic.toMutableList()
                //a.removeAll(newFilteredNoAnimatedPics)
                vm.host.filteredPic.clear()
                vm.host.filteredPic.addAll(newFilteredAnimatedPics)
            } else {
                vm.host.filteredPic.clear()
                vm.host.filteredPic.addAll(allPics)
            }

        }

        /**  ➜ сюда запоминаем элемент, который пользователь хочет удалить  */
        var itemPendingDelete by remember { mutableStateOf<AlbumDetails?>(null) }

        /* ---------- Диалог подтверждения ---------- */
        itemPendingDelete?.let { pending ->
            AlbumDialogDeleteAlbum(pending, onDismiss = { itemPendingDelete = null }, {
                vm.saved.albums.remove(pending)
                itemPendingDelete = null
            })
        }
        /* ---------- /Диалог ---------- */

        vm.p2pAlbumSource?.let { source ->
            // Навигация — side effect, нельзя звать прямо из композиции.
            LaunchedEffect(source) {
                navigator.push(ScreenP2pSend(source))
                vm.dismissP2pAlbum()
            }
        }

        Scaffold(
            bottomBar = {
                if (album?.albumPicsDetails?.percentLoad != 1.0f) {
                    LinearProgressIndicator(
                        progress = { album?.albumPicsDetails?.percentLoad ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                    )
                }
            },

            containerColor = Theme.background
        ) { padding ->

            Box(modifier = Modifier.fillMaxSize())
            {
                L_LazyRowPictureDetails(
                    host = vm.host,
                    expandMenu = ExpandMenuType.ALBUM,
                    showInitialLoading = showInitialItemsLoading,
                    itemBefore = {
                        Column(modifier = Modifier
                            .displayCutoutPadding()
                            .padding(horizontal = 4.dp)) {
                            if (parsed != null) {

                                Row {
                                    UrlImage( parsed.cover.url, modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .size(72.dp) )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(parsed.title, color = Theme.L.textColor, style = Theme.L.Type.rowTitle)
                                        Text( "${parsed.number_of_animated_pictures} gifs / ${parsed.number_of_pictures} pictures", color = Theme.L.textColor )
                                    }
                                }


                                Spacer(modifier = Modifier.height(4.dp))
                                val strId = buildAnnotatedString {
                                    withStyle( style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle() ) { append("Id: ") }
                                    withStyle( style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(idAlbum.toString()) }
                                }
                                Text(strId, color = Theme.L.textColor)

                                Spacer(modifier = Modifier.height(4.dp))

                                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

                                val textCreated =
                                    Instant.ofEpochSecond(parsed.created.toLong()) // или ofEpochMilli
                                        .atZone(ZoneId.systemDefault()).format(formatter)

                                val textModified =
                                    Instant.ofEpochSecond(parsed.modified.toLong()) // или ofEpochMilli
                                        .atZone(ZoneId.systemDefault()).format(formatter)


                                val str = buildAnnotatedString {
                                    withStyle( style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle() ) { append("Created: ") }
                                    withStyle( style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(textCreated) }
                                }

                                if (parsed.created != 0.0) {
                                    Text(str, color = Theme.L.textColor)
                                }

                                val str1 = buildAnnotatedString {
                                    withStyle( style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp).toSpanStyle() ) { append("Modified: ") }
                                    withStyle( style = Theme.L.Type.rowTitle.copy(fontSize = 14.sp).toSpanStyle()) { append(textModified) }
                                }

                                if (parsed.modified != 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text( str1, color = Theme.L.textColor)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                AlbumInfoGreeting(parsed) { genre ->
                                    navigator.push(
                                        L_ScreenAlbumList.create( filter = albumListFilterForGenre(genre), title = "Genre: ${genre.title}" )
                                    )
                                }
                                AlbumInfoAudiences(parsed) { audience ->
                                    navigator.push(
                                        L_ScreenAlbumList.create( filter = albumListFilterForAudience(audience), title = "Audience: ${audience.title}" )
                                    )
                                }
                                AlbumInfoTags({
                                    parsed.tags.reversed().filter { it.count > 0 }
                                }) { navigator.push(ScreenLAlbumLandingTag(it)) }
                                AlbumInfoButtonSaveAlbum(saved, onClick = { if (!saved) { vm.saveAlbum() } else { itemPendingDelete = parsed } })
                                AlbumInfoButtonShareAlbum(onClick = { vm.shareAlbumP2p(parsed) })
                                AlbumInfoFilterButton( parsed, vm.showOnlyAnimated) { vm.showOnlyAnimated = it }
                                LAlbumNetworkIssuePanel(
                                    albumPicsDetails = albumPicsDetails,
                                    onRetryFailedPages = { vm.retryFailedAlbumPages() }
                                )
                            }
                        }
                    }
                )
            }

        }

    }

}

private fun albumListFilterForGenre(genre: Genre): AlbumListFilter {
    return AlbumListFilter(
        genresPlus = listOf(
            FilterGenre(
                id = genre.id,
                title = genre.title,
                slug = genre.url.extractLPathSlug() ?: genre.title.lowercase().replace(" ", "-"),
                description = "",
                uploadingRules = "",
                posterUrl = null,
                actsAsWarning = genre.actsAsWarning,
                actsAsDefault = false,
                representsUncategorized = false,
                url = genre.url,
                parent = null,
                onlyAllowsModel = null,
                onlyContent = null
            )
        )
    )
}

private fun albumListFilterForAudience(audience: Audience): AlbumListFilter {
    return AlbumListFilter(audienceIds = "+${audience.id}")
}

private fun String.extractLPathSlug(): String? {
    val name = trim('/').substringAfterLast('/').substringBefore('?')
    return name.substringBeforeLast("_").takeIf { it.isNotBlank() }
}

@Composable
private fun LAlbumNetworkIssuePanel(
    albumPicsDetails: AlbumPicsDetails?,
    onRetryFailedPages: () -> Unit
) {
    if (albumPicsDetails == null) return
    LAlbumNetworkIssuePanel(
        failedPages = albumPicsDetails.failedPages.toList(),
        protectionState = albumPicsDetails.protectionUiState,
        isRetryingFailedPages = albumPicsDetails.isRetryingFailedPages,
        onRetryFailedPages = onRetryFailedPages
    )
}

@Composable
private fun LAlbumNetworkIssuePanel(
    failedPages: List<LAlbumPageLoadIssue>,
    protectionState: LRepositoryProtectionUiState,
    isRetryingFailedPages: Boolean,
    onRetryFailedPages: () -> Unit
) {
    val shouldShow = failedPages.isNotEmpty() || protectionState.active
    if (!shouldShow) return

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(protectionState.active, protectionState.retryAtMs) {
        while (protectionState.active && protectionState.remainingMs(nowMs) > 0L) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
        nowMs = System.currentTimeMillis()
    }

    val retryAfterSeconds = ceil(protectionState.remainingMs(nowMs) / 1000.0)
        .toInt()
        .coerceAtLeast(0)
    val htmlChallenge = protectionState.active || failedPages.any { it.htmlChallenge }
    val failedPagesText = failedPages.joinToString(", ") { it.page.toString() }
        .ifBlank { "нет" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
            .border(
                width = 1.dp,
                color = if (htmlChallenge) Color(0xFFFFC857) else Theme.L.grey2,
                shape = RoundedCornerShape(8.dp)
            )
            .background(Theme.L.grey5, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (htmlChallenge) Color(0xFFFFC857) else Theme.L.grey2
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (htmlChallenge) {
                    if (retryAfterSeconds > 0) {
                        "Сервер временно отдаёт защитную страницу, повтор через $retryAfterSeconds сек."
                    } else {
                        "Сервер временно отдаёт защитную страницу."
                    }
                } else {
                    "Часть страниц альбома не загрузилась."
                },
                color = Theme.L.textColor,
                style = Theme.L.Type.rowTitle,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Если старая страница была в кэше, она уже показана. Недогруженные страницы: $failedPagesText",
            color = Theme.L.grey2,
            style = Theme.L.Type.rowSubtitle,
            modifier = Modifier.padding(top = 6.dp)
        )

        Button(
            onClick = onRetryFailedPages,
            enabled = failedPages.isNotEmpty() && !isRetryingFailedPages,
            colors = ButtonDefaults.buttonColors(containerColor = Theme.L.primaryColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
            Spacer(Modifier.width(6.dp))
            Text(
                if (isRetryingFailedPages) "Повторяю..." else "Повторить страницы",
                color = Color.Black,
                style = Theme.L.Type.button
            )
        }
    }
}


// ----------------------------------------------------------------------------
// PREVIEW
//
// ScreenLAlbum.Content() завязан на ScreenModel (getScreenModel) и stateful
// L_LazyRowPictureDetails(host=...), поэтому реальный экран в @Preview не
// построить. Ниже — stateless-копия раскладки (Scaffold + нижний прогресс-бар
// + плашка-шапка альбома) с фейковыми данными. Только для визуальной проверки.
// ----------------------------------------------------------------------------

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun ScreenLAlbumPreviewBody(
    title: String,
    animatedCount: Int,
    pictureCount: Int,
    percentLoad: Float,
) {
    Scaffold(
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

@Preview(showBackground = true, backgroundColor = 0xFF262626, widthDp = 360)
@Composable
private fun LAlbumNetworkIssuePanelPreview() {
    XvideosTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Failed Pages only:", color = Color.White)
            LAlbumNetworkIssuePanel(
                failedPages = listOf(
                    LAlbumPageLoadIssue(1, "Error", false),
                    LAlbumPageLoadIssue(2, "Error", false)
                ),
                protectionState = LRepositoryProtectionUiState(active = false),
                isRetryingFailedPages = false,
                onRetryFailedPages = {}
            )

            Spacer(Modifier.height(16.dp))
            Text("HTML Challenge active:", color = Color.White)
            LAlbumNetworkIssuePanel(
                failedPages = emptyList(),
                protectionState = LRepositoryProtectionUiState(active = true, retryAtMs = System.currentTimeMillis() + 30000),
                isRetryingFailedPages = false,
                onRetryFailedPages = {}
            )

            Spacer(Modifier.height(16.dp))
            Text("Retrying state:", color = Color.White)
            LAlbumNetworkIssuePanel(
                failedPages = listOf(LAlbumPageLoadIssue(1, "Error", false)),
                protectionState = LRepositoryProtectionUiState(active = false),
                isRetryingFailedPages = true,
                onRetryFailedPages = {}
            )
        }
    }
}


