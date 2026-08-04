package com.client.xvideos.l.ui.screens.screenFullScreen

import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.theme.LavenderDialog

import android.os.Build
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.noRippleClickable
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lAnimationVideoUrl
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.model.lFullScreenImageUrls
import com.client.xvideos.l.model.lImageMediaUrl
import com.client.xvideos.l.model.lMediaRequestHeaders
import com.client.xvideos.l.model.lPreviewImageUrl
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuViewModel
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.redgifs.common.video.player_with_menu.atom.VideoPlayerWithMenuContent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

private const val TAG_URL = "url"

// Хелперы offsetForPage/startOffsetForPage/endOffsetForPage удалены: они читали
// currentPageOffsetFraction в composition, из-за чего каждая страница пейджера
// рекомпозилась на каждом кадре прокрутки. Если понадобится анимация перехода —
// читать offset только внутри graphicsLayer { } (фаза отрисовки).

@Parcelize
class L_FullScreenImage(
    val item: PicsDetails,
    /** Имя источника: id альбома, "l_likes" или имя коллекции. Идёт в загрузку файлов. */
    val albumName: String,
    /** Числовой id альбома для меню элемента. Пусто для лайков и коллекций. */
    val idAlbum: String = "",
    /** Ключ списка картинок в [LFullScreenPayload]. Сам список в Bundle не влезает. */
    val payloadKey: String = "",
    val autoPlay: Boolean = false,
    val isAnimated: Boolean = false,
    val expandMenu: ExpandMenuType,
    val isCollection: Boolean = false,
    @IgnoredOnParcel val onClose: (Int) -> Unit = {},

    ) : Screen, Parcelable {

    @IgnoredOnParcel
    override val key: ScreenKey = uniqueScreenKey

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(
        ExperimentalFoundationApi::class,
        ExperimentalMaterialApi::class,
        DelicateCoroutinesApi::class
    )
    @Composable
    override fun Content() {

//        run {
//            Timber.d("!!! >>>> filteredPic type: ${filteredPic::class.java.simpleName}")
//            //filteredPic.toList()
//        }

        /**
         * Показ полностью фуллскрин
         */
        var isFullScreen by remember { mutableStateOf(false) }

        // Снимок берём один раз: toList() + indexOf (equals по всем полям PicsDetails)
        // на каждой рекомпозиции давали заметный провал кадров при смене страницы.
        //
        // ifEmpty: хранилище списков не переживает смерть процесса, а сам экран
        // Parcelable и восстанавливается Voyager'ом. Без запасного варианта
        // indexOf вернёт -1 и coerceIn(0, -1) уронит экран.
        val filteredPic = remember {
            LFullScreenPayload.get(payloadKey).ifEmpty { listOf(item) }
        }

        val expandMenuViewModel: ExpandMenuViewModel = hiltViewModel()

        val navigator = LocalNavigator.currentOrThrow

        var isClosing by remember { mutableStateOf(false) }

        var corruptCancel by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        var rotate by remember { mutableStateOf(false) }
        var showInfoDialog by remember { mutableStateOf(false) }
        val verticalPager = Settings.l_fullscreen_vertical_pager.field.collectAsStateWithLifecycle().value
        val videoMuted = Settings.l_fullscreen_video_muted.field.collectAsStateWithLifecycle().value

        val initialIndex = remember { filteredPic.indexOf(item).coerceIn(0, filteredPic.lastIndex) }

        val pagerState = rememberPagerState( initialIndex, pageCount = { filteredPic.size } )

        // Состояние для LazyRow
        val lazyRowState = rememberLazyListState( cacheWindow = LazyLayoutCacheWindow( ahead = 200.dp, behind = 200.dp ) )


        LaunchedEffect(isClosing) {
            if (isClosing) {
                onClose( if (corruptCancel) pagerState.currentPage else -1 )
                navigator.pop()
            }
        }

        BackHandler { isClosing = true }


        // Текущий индекс из pagerState
        val currentIndex = pagerState.currentPage

        LaunchedEffect(currentIndex) { if (currentIndex != initialIndex) { corruptCancel = true } }


        // Автоматическая прокрутка LazyRow к текущему элементу.
        // scrollToItem, а не animateScrollToItem: анимация ленты миниатюр шла
        // одновременно со снапом пейджера и на слабом телефоне отъедала кадры.
        LaunchedEffect(currentIndex) {
            lazyRowState.scrollToItem((currentIndex - 2).coerceIn(0, filteredPic.size - 1))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                //Шахматная доска
                .checkerboardBackground( squareSize = 12.dp, lightColor = Color(0xFF252525), darkColor = Color(0xFF181818) )
                .noRippleClickable( onClick = { isFullScreen = isFullScreen.not() } )

        ) {
            if (showInfoDialog) {
                LPictureInfoDialog(
                    item = filteredPic.getOrNull(currentIndex) ?: item,
                    position = currentIndex,
                    total = filteredPic.size,
                    onDismiss = { showInfoDialog = false },
                    onAlbumClick = { albumId ->
                        showInfoDialog = false
                        // Штатный путь альбом -> фуллскрин -> инфо -> тот же альбом
                        // клал второй экземпляр экрана поверх первого. Если альбом
                        // уже в стеке — возвращаемся к нему.
                        val inStack = navigator.items.any { it is ScreenLAlbum && it.idAlbum == albumId }
                        if (inStack) {
                            navigator.popUntil { it is ScreenLAlbum && it.idAlbum == albumId }
                        } else {
                            navigator.push(ScreenLAlbum(albumId))
                        }
                    }
                )
            }

            if (verticalPager) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 0.dp,
                    beyondViewportPageCount = 1,
                    // url_to_original не уникален (см. L_LazyRowPictureDetails): дубль
                    // картинки в альбоме давал одинаковый ключ и падение пейджера.
                    key = { page -> "${filteredPic.getOrNull(page)?.url_to_original}#$page" }
                ) { page ->
                    LFullScreenPage(
                        pageItem = filteredPic[page],
                        page = page,
                        currentIndex = currentIndex,
                        pagerState = pagerState,
                        rotate = rotate,
                        albumName = albumName,
                        autoPlay = autoPlay,
                        videoMuted = videoMuted,
                        // Пейджер листается вертикально — горизонтальная перемотка не мешает.
                        seekDragEnabled = true,
                        onToggleFullScreen = { isFullScreen = isFullScreen.not() }
                    )
                }
            } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1,
                reverseLayout = false,
                // См. VerticalPager выше: url_to_original не уникален.
                key = { page -> "${filteredPic.getOrNull(page)?.url_to_original}#$page" }
            ) { page ->
                LFullScreenPage(
                    pageItem = filteredPic[page],
                    page = page,
                    currentIndex = currentIndex,
                    pagerState = pagerState,
                    rotate = rotate,
                    albumName = albumName,
                    autoPlay = autoPlay,
                    videoMuted = videoMuted,
                    // Зона перемотки в нижней трети плеера перехватывала
                    // горизонтальный свайп и страницы не листались.
                    seekDragEnabled = false,
                    onToggleFullScreen = { isFullScreen = isFullScreen.not() }
                )
            }
            }

            Box(modifier = Modifier.align(Alignment.TopStart)) { Text( currentIndex.toString(), color = Color.Gray, modifier = Modifier.padding(start = 8.dp), fontFamily = Theme.L.fontFamilyKarla )}

            AnimatedVisibility(visible = !isFullScreen, enter = fadeIn(), exit = fadeOut())
            {
                //Верхние кнопки
                Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart).offset(y = 4.dp), horizontalArrangement = Arrangement.SpaceBetween)
                {
                    Row {
                        IconButton(onClick = { rotate = rotate.not() }) { Icon(Icons.Default.ScreenRotation, contentDescription = "Повернуть изображение", tint = Color.White) }
                        IconButton(onClick = { Settings.l_fullscreen_vertical_pager.setValue(!verticalPager) }) { Icon( if (verticalPager) Icons.Default.SwapVert else Icons.Default.SwapHoriz, contentDescription = if (verticalPager) "Листать по горизонтали" else "Листать по вертикали", tint = Color.White) }
                        // Звук был зашит в mute без единой кнопки включить.
                        IconButton(onClick = { Settings.l_fullscreen_video_muted.setValue(!videoMuted) }) { Icon( if (videoMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = if (videoMuted) "Включить звук" else "Выключить звук", tint = Color.White) }
                    }

                    Row {
                        IconButton(onClick = { showInfoDialog = true }) { Icon( Icons.Default.Info, contentDescription = "Информация о картинке", tint = Color.White ) }
                        expandMenuViewModel.ExpandMenu( expandMenu, filteredPic.getOrNull(pagerState.currentPage) ?: item, idAlbum, isCollection )
                    }
                }
            }

            /** Единственный экземпляр P2P-хоста на экран — вне AnimatedVisibility,
             *  чтобы диалог/навигация не умирали при скрытии панели. */
            expandMenuViewModel.P2pShareHost()

            AnimatedVisibility(
                visible = !isFullScreen,
                // Панель выезжает снизу и уезжает вниз ({ it } = на полную свою высоту).
                enter = fadeIn(),
                exit  = fadeOut(),
            )
            {
                SwipeableBottomPanel { swipeableState, hiddenOffset ->

                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        LazyRow( state = lazyRowState, modifier = Modifier.height(72.dp) )
                        {
                            itemsIndexed(
                                filteredPic,
                                // См. VerticalPager выше: url_to_original не уникален.
                                key = { index, item -> "${item.url_to_original}#$index" }) { index, it1 ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .aspectRatio(it1.width.toFloat() / it1.height)
                                        // Раньше клик выставлял dataItem, а обратный
                                        // indexOf(dataItem) на дубликатах картинки
                                        // возвращал чужой индекс и пейджер прыгал назад.
                                        .clickable(onClick = {
                                            coroutineScope.launch { pagerState.scrollToPage(index) }
                                            corruptCancel = true
                                        })
                                        .border(2.dp, if (index == currentIndex) Color.Yellow else Color.Transparent, RoundedCornerShape(4.dp)).padding(2.dp)
                                ) {
                                    val thumbUrl = it1.lPreviewImageUrl("large_thumbnail")
                                    if (thumbUrl.isNotBlank() && !thumbUrl.isLVideoFileUrl()) {
                                        UrlImage(
                                            url = thumbUrl,
                                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).fillMaxSize(),
                                            contentScale = ContentScale.FillBounds,
                                            onSuccess = { }, albumName = albumName, autoPlay = false, isAnimated = false, sizeButton = 20.dp, sizeButtonIcon = 12.dp
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF202020))
                                                .fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }

        }
    }
}

@Composable
private fun LFullScreenPage(
    pageItem: PicsDetails,
    page: Int,
    currentIndex: Int,
    /**
     * Только для settledPage — он меняется раз в свайп.
     * Ничего, что обновляется каждый кадр (currentPageOffsetFraction,
     * currentPage вместе с ним), здесь читать нельзя: страница уйдёт
     * в рекомпозицию на каждом кадре прокрутки.
     */
    pagerState: PagerState,
    rotate: Boolean,
    albumName: String,
    autoPlay: Boolean,
    videoMuted: Boolean,
    seekDragEnabled: Boolean,
    onToggleFullScreen: () -> Unit
) {
    val zoomState = rememberZoomState()
    val coroutineScope = rememberCoroutineScope()
    val isCurrentPage = currentIndex == page

    // derivedStateOf, а не settledIndex параметром: чтение settledPage в scope
    // Content рекомпозило весь экран на каждый settle. Здесь рекомпозятся только
    // те страницы, у которых флаг реально поменялся.
    val isSettledPage by remember(page, pagerState) {
        derivedStateOf { pagerState.settledPage == page }
    }

    // Страница ушла из фокуса — снимаем зум. Пейджер держит соседние страницы
    // живыми, а увеличенная картинка рисуется graphicsLayer'ом без clip и
    // налезала на текущую страницу.
    // Ключ именно settled, а не current: currentPage флипается на середине свайпа,
    // и reset() (это snapTo, не анимация) схлопывал картинку прямо на глазах.
    LaunchedEffect(isSettledPage) { if (!isSettledPage) zoomState.reset() }

    // clipToBounds по границам страницы: зум и поворот (rotationZ + scale в
    // UrlImage) рисуют за пределами layout-границ, из-за чего соседние страницы
    // накладывались друг на друга при прокрутке.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(
                if (rotate) pageItem.height.toFloat() / pageItem.width
                else pageItem.width.toFloat() / pageItem.height,
                matchHeightConstraintsFirst = false
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val videoUrl = pageItem.lAnimationVideoUrl()
            if (videoUrl != null) {
                LFullScreenVideo(
                    url = videoUrl,
                    previewUrl = pageItem.lPreviewImageUrl("large_thumbnail"),
                    albumName = albumName,
                    autoPlay = autoPlay,
                    isCurrentPage = isCurrentPage,
                    isPlayerActive = isSettledPage,
                    isMuted = videoMuted,
                    seekDragEnabled = seekDragEnabled,
                    rotate = rotate,
                    modifier = Modifier.fillMaxSize(),
                    onTap = onToggleFullScreen
                )
            } else {
                val imageUrls = remember(pageItem.url_to_original, pageItem.thumbnails) {
                    pageItem.lFullScreenImageUrls()
                }
                var imageUrlIndex by remember(imageUrls) { mutableIntStateOf(0) }
                val imageUrl = imageUrls.getOrNull(imageUrlIndex).orEmpty()

                if (imageUrl.isNotBlank()) {
                    UrlImage(
                        rotate = rotate,
                        contentScale = ContentScale.Fit,
                        url = imageUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(
                                zoomState = zoomState,
                                enableOneFingerZoom = false,
                                onDoubleTap = { position ->
                                    coroutineScope.launch {
                                        if (zoomState.scale > 1.0f) {
                                            zoomState.changeScale(1.0f, Offset.Zero)
                                        } else {
                                            zoomState.changeScale(2.5f, position)
                                        }
                                    }
                                },
                                onTap = { onToggleFullScreen() }
                            ),
                        onSuccess = { },
                        onFailure = {
                            if (imageUrlIndex < imageUrls.lastIndex) {
                                imageUrlIndex += 1
                                timber.log.Timber.w("!!! L fullscreen image fallback ${imageUrlIndex}/${imageUrls.lastIndex}: ${imageUrls[imageUrlIndex]}")
                            }
                        },
                        albumName = albumName,
                        autoPlay = autoPlay,
                        isAnimated = pageItem.is_animated,
                        isVisible = isCurrentPage,
                        isFullScreen = true
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет ссылки на изображение", color = Color.Gray)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun LFullScreenVideo(
    url: String,
    previewUrl: String,
    albumName: String,
    autoPlay: Boolean,
    isCurrentPage: Boolean,
    isPlayerActive: Boolean,
    isMuted: Boolean,
    seekDragEnabled: Boolean,
    rotate: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    // Соседние страницы пейджера тоже скомпонованы, и каждая поднимала свой
    // ExoPlayer (кодек + буферы). Плеер создаём только когда прокрутка
    // остановилась на этой странице, до этого показываем постер.
    if (!isPlayerActive) {
        Box(modifier = modifier.noRippleClickable(onClick = onTap)) {
            LFullScreenVideoPoster(
                previewUrl = previewUrl,
                albumName = albumName,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val playerHost = remember(url) {
        MediaPlayerHost(
            mediaUrl = url,
            isPaused = !autoPlay || !isCurrentPage,
            isMuted = isMuted,
            headers = lMediaRequestHeaders()
        )
    }

    // playerHost помнится по url, поэтому переключение звука доводим отдельно.
    LaunchedEffect(playerHost, isMuted) {
        if (isMuted) playerHost.mute() else playerHost.unmute()
    }
    var playbackError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(playerHost) {
        playerHost.videoFitMode = ScreenResize.FIT
        playerHost.onError = {
            playbackError = true
            timber.log.Timber.e("!!! L fullscreen video error: ${it.message}")
        }
    }

    LaunchedEffect(autoPlay, isCurrentPage) {
        if (autoPlay && isCurrentPage) {
            playerHost.play()
        } else {
            playerHost.pause()
        }
    }

    Box(modifier = modifier) {
        VideoPlayerWithMenuContent(
            modifier = Modifier.fillMaxSize(),
            playerHost = playerHost,
            onClick = onTap,
            autoRotate = rotate,
            seekDragEnabled = seekDragEnabled
        )

        AnimatedVisibility(
            visible = playerHost.poster || playbackError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LFullScreenVideoPoster(
                previewUrl = previewUrl,
                albumName = albumName,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (playerHost.poster && !playbackError) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.LightGray
            )
        }
    }
}

@Composable
private fun LFullScreenVideoPoster(
    previewUrl: String,
    albumName: String,
    modifier: Modifier = Modifier
) {
    if (previewUrl.isNotBlank() && !previewUrl.isLVideoFileUrl()) {
        UrlImage(
            url = previewUrl,
            contentScale = ContentScale.Fit,
            modifier = modifier,
            albumName = albumName,
            autoPlay = false,
            isAnimated = false
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF202020)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
        }
    }
}

// Дополнительная функция для создания кастомного Modifier для блокировки pager при зуме
@Composable
private fun LPictureInfoDialog(
    item: PicsDetails,
    position: Int,
    total: Int,
    onDismiss: () -> Unit,
    onAlbumClick: ((Long) -> Unit)? = null
) {
    val albumId = item.album?.toLongOrNull()
    val uriHandler = LocalUriHandler.current

    LavenderDialog(
        title = "Информация",
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Альбом: ", color = Color.DarkGray, fontFamily = Theme.L.fontFamilyKarla)
                    if (albumId != null && onAlbumClick != null) {
                        TextButton(onClick = { onAlbumClick(albumId) }) {
                            Text(albumId.toString())
                        }
                    } else {
                        Text(item.album ?: "-", color = Color.DarkGray, fontFamily = Theme.L.fontFamilyKarla)
                    }
                }

                LPictureInfoText(
                    text = lPictureInfoText(item, position, total),
                    onUrlClick = { url -> uriHandler.openUri(url) }
                )
            }
        },
        confirmText = "OK",
        onConfirm = onDismiss,
    )
}

@Composable
private fun LPictureInfoText(
    text: String,
    onUrlClick: (String) -> Unit
) {
    val annotatedText = remember(text) { text.withClickableHttpsLinks() }

    ClickableText(
        text = annotatedText,
        style = TextStyle(
            color = Color.DarkGray,
            fontFamily = Theme.L.fontFamilyKarla
        ),
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(TAG_URL, offset, offset)
                .firstOrNull()
                ?.item
                ?.let(onUrlClick)
        }
    )
}

private fun String.withClickableHttpsLinks() = buildAnnotatedString {
    val urlRegex = Regex("""https://\S+""")
    var lastIndex = 0

    urlRegex.findAll(this@withClickableHttpsLinks).forEach { match ->
        val rawUrl = match.value
        val url = rawUrl.trimEnd('.', ',', ';', ')', ']', '}')
        val start = match.range.first
        val end = start + url.length

        append(this@withClickableHttpsLinks.substring(lastIndex, start))

        val annotatedStart = length
        append(url)
        addStringAnnotation(TAG_URL, url, annotatedStart, annotatedStart + url.length)
        addStyle(
            SpanStyle(
                color = Color(0xFF8AB4F8),
                textDecoration = TextDecoration.Underline
            ),
            annotatedStart,
            annotatedStart + url.length
        )

        append(rawUrl.substring(url.length))
        lastIndex = match.range.last + 1
        if (end < start) lastIndex = match.range.last + 1
    }

    if (lastIndex < this@withClickableHttpsLinks.length) {
        append(this@withClickableHttpsLinks.substring(lastIndex))
    }
}

private fun lPictureInfoText(
    item: PicsDetails,
    position: Int,
    total: Int
): String = buildString {
    appendLine("Позиция: ${position + 1} / $total")
    appendLine("Размер: ${item.width} x ${item.height}")
    appendLine("Анимация: ${item.is_animated}")
    appendLine()

    appendLine("Используемая картинка:")
    appendLine(item.lImageMediaUrl() ?: "-")
    appendLine()

    appendLine("URL для скачивания:")
    appendLine(item.lDownloadUrl() ?: "-")
    appendLine()

    appendLine("URL видео:")
    appendLine(item.lAnimationVideoUrl() ?: item.url_to_video ?: "-")
    appendLine()

    appendLine("url_to_original:")
    appendLine(item.url_to_original ?: "-")
    appendLine()

    appendLine("url_to_video raw:")
    appendLine(item.url_to_video ?: "-")
    appendLine()

    appendLine("FullScreen candidates (${item.lFullScreenImageUrls().size}):")
    item.lFullScreenImageUrls().forEachIndexed { index, url ->
        appendLine("${index + 1}. $url")
    }
    appendLine()

    appendLine("Preview large_thumbnail:")
    appendLine(item.lPreviewImageUrl("large_thumbnail").ifBlank { "-" })
    appendLine()

    appendLine("Preview small:")
    appendLine(item.lPreviewImageUrl("small").ifBlank { "-" })
    appendLine()

    appendLine("Preview xMax:")
    appendLine(item.lPreviewImageUrl("xMax").ifBlank { "-" })
    appendLine()

    val thumbnails = item.thumbnails.orEmpty()
    appendLine("Thumbnails (${thumbnails.size}):")
    thumbnails.forEachIndexed { index, thumbnail ->
        appendLine("${index + 1}. size=${thumbnail.size ?: "-"} ${thumbnail.width}x${thumbnail.height}")
        appendLine(thumbnail.url ?: "-")
    }
}

// Вариант 2: Создание кастомного Modifier
fun Modifier.checkerboardBackground(
    squareSize: Dp = 8.dp,
    lightColor: Color = Color.White,
    darkColor: Color = Color.LightGray
): Modifier = this.then(
    Modifier.drawBehind {
        val squareSizePx = squareSize.toPx()
        val squaresHorizontal = (size.width / squareSizePx).toInt() + 1
        val squaresVertical = (size.height / squareSizePx).toInt() + 1

        for (i in 0..squaresHorizontal) {
            for (j in 0..squaresVertical) {
                val isLightSquare = (i + j) % 2 == 0
                val color = if (isLightSquare) lightColor else darkColor

                drawRect(
                    color = color,
                    topLeft = Offset(
                        x = i * squareSizePx,
                        y = j * squareSizePx
                    ),
                    size = Size(squareSizePx, squareSizePx)
                )
            }
        }
    }
)
