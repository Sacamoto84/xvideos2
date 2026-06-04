package com.client.xvideos.l.ui.screens.screenAlbum

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.screenRoot.LocalRootScreenModel
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Audience
import com.client.xvideos.l.model.Genre
import com.client.xvideos.l.net.graphQl.Genre as FilterGenre
import com.client.xvideos.l.net.AlbumPicsDetails
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumDialogDeleteAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoAudiences
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonSaveAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoFilterButton
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoGreeting
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoTags
import com.client.xvideos.l.ui.screens.albumLandingTag.ScreenLAlbumLandingTag
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import com.client.xvideos.l.ui.screens.screenAlbum.atom.ScrollToTopButton
import kotlinx.coroutines.delay
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import timber.log.Timber
import kotlin.math.ceil

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

        Scaffold(
            floatingActionButton = {
                AnimatedVisibility(
                    vm.host.state.firstVisibleItemIndex > 3 && vm.host.selectedImage == null,
                    enter = fadeIn(), exit = fadeOut()
                ) { ScrollToTopButton(vm.host.state) }
            },
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

            containerColor = ThemeL.greyBackground
        ) { padding ->

            Box(modifier = Modifier.fillMaxSize())
            {
                L_LazyRowPictureDetails(
                    host = vm.host,
                    expandMenu = ExpandMenuType.ALBUM,
                    showInitialLoading = showInitialItemsLoading,
                    itemBefore = {
                        Column(modifier = Modifier.displayCutoutPadding().padding(horizontal = 4.dp)) {
                            if (parsed != null) {

                                Row {
                                    UrlImage( parsed.cover.url, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(72.dp) )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(parsed.title, color = ThemeL.textColor, style = ThemeL.Type.rowTitle)
                                        Text( "${parsed.number_of_animated_pictures} gifs / ${parsed.number_of_pictures} pictures", color = ThemeL.textColor )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                AlbumInfoGreeting(parsed) { genre ->
                                    navigator.push(
                                        L_ScreenAlbumList.create(
                                            filter = albumListFilterForGenre(genre),
                                            title = "Genre: ${genre.title}"
                                        )
                                    )
                                }
                                AlbumInfoAudiences(parsed) { audience ->
                                    navigator.push(
                                        L_ScreenAlbumList.create(
                                            filter = albumListFilterForAudience(audience),
                                            title = "Audience: ${audience.title}"
                                        )
                                    )
                                }
                                AlbumInfoTags(parsed) { navigator.push(ScreenLAlbumLandingTag(it)) }
                                AlbumInfoButtonSaveAlbum(saved, onClick = { if (!saved) { vm.saveAlbum() } else { itemPendingDelete = parsed } })
                                AlbumInfoFilterButton( parsed, vm.showOnlyAnimated, { vm.showOnlyAnimated = it })
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

    val failedPages = albumPicsDetails.failedPages.toList()
    val protectionState = albumPicsDetails.protectionUiState
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
                color = if (htmlChallenge) Color(0xFFFFC857) else ThemeL.grey2,
                shape = RoundedCornerShape(8.dp)
            )
            .background(ThemeL.grey5, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (htmlChallenge) Color(0xFFFFC857) else ThemeL.grey2
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
                color = ThemeL.textColor,
                style = ThemeL.Type.rowTitle,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Если старая страница была в кэше, она уже показана. Недогруженные страницы: $failedPagesText",
            color = ThemeL.grey2,
            style = ThemeL.Type.rowSubtitle,
            modifier = Modifier.padding(top = 6.dp)
        )

        Button(
            onClick = onRetryFailedPages,
            enabled = failedPages.isNotEmpty() && !albumPicsDetails.isRetryingFailedPages,
            colors = ButtonDefaults.buttonColors(containerColor = ThemeL.primaryColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
            Spacer(Modifier.width(6.dp))
            Text(
                if (albumPicsDetails.isRetryingFailedPages) "Повторяю..." else "Повторить страницы",
                color = Color.Black,
                style = ThemeL.Type.button
            )
        }
    }
}

