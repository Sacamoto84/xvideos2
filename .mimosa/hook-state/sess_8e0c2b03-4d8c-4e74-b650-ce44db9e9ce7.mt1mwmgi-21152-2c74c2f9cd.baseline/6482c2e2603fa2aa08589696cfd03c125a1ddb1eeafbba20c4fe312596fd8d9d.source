package com.client.xvideos.l.ui.screens.screenFullScreen

import com.client.xvideos.common.theme.Theme

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lPreviewImageUrl
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuViewModel
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

// Соседние файлы того же пакета, выделенные отсюда: LFullScreenPage.kt (страница
// пейджера), LFullScreenVideo.kt (видео и постер), LPictureInfo.kt (диалог
// «Информация»), CheckerboardBackground.kt (подложка). Здесь остался сам экран.

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
