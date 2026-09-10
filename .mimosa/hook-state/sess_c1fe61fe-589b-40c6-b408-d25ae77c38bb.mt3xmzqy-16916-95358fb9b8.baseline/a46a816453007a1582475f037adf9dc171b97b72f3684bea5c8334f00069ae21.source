package com.client.xvideos.l.ui.screens.screenAlbum

import android.annotation.SuppressLint
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.screens.albumLandingTag.ScreenLAlbumLandingTag
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoAudiences
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonSaveAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonShareAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoFilterButton
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoGreeting
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoTags
import com.client.xvideos.l.ui.screens.screenAlbum.dialog.AlbumDialogDeleteAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import com.client.xvideos.common.navigation.rememberNavigationDepth
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScreenLAlbum(val idAlbum: Long) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalZoomableApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        rememberNavigationDepth().depth = 100

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

        val topInset = getTopInsetDp()

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

            if (vm.showOnlyAnimated) {
                vm.host.replaceFilteredPictures(newFilteredAnimatedPics)
            } else {
                vm.host.replaceFilteredPictures(allPics)
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

                            //.displayCutoutPadding()
                            .padding(horizontal = 4.dp))
                        {

                            Box(modifier = Modifier.fillMaxWidth().height(topInset)){ }

                            if (parsed != null) {

                                Row {
                                    UrlImage( parsed.cover?.url.orEmpty(), modifier = Modifier
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

