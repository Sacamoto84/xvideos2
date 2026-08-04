package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.screenRoot.LocalMainNavigator
import com.client.xvideos.screenRoot.LocalRootScreenModel
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lAnimationVideoUrl
import com.client.xvideos.l.model.lMediaRequestHeaders
import com.client.xvideos.l.model.lPreviewImageUrl
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuViewModel
import com.client.xvideos.l.ui.screens.screenFullScreen.L_FullScreenImage
import com.client.xvideos.l.ui.screens.screenFullScreen.fullScreenImageFilteredPicArray
import com.client.xvideos.r.ui.profile.atom.VerticalScrollbar
import com.client.xvideos.r.ui.profile.rememberVisibleRangePercentIgnoringFirstNForLazyStaggeredGrid
import com.redgifs.common.video.player_with_menu.atom.VideoPlayerWithMenuContent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Staggered grid для отображения миниатюр изображений/видео альбома.
 *
 * Поддерживает кликабельные превью, полноэкранный просмотр с возвратом на позицию,
 * контекстное меню, кастомные размеры миниатюр и индикатор прогресса прокрутки.
 *
 * @param host Контейнер состояния и данных альбома
 * @param itemBefore Header-контент перед списком (опционально)
 * @param expandMenu Тип меню дополнительных действий для элементов
 * @param tag Тег для UI-тестов
 */

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun L_LazyRowPictureDetails(
    host: LazyRowPictureDetailsHost,
    itemBefore: @Composable () -> Unit = {},
    expandMenu: ExpandMenuType,
    tag: String = "",
    showInitialLoading: Boolean = false,
    isCollection: Boolean = false
) {
    val expandMenuViewModel: ExpandMenuViewModel = hiltViewModel()
    val navigator = LocalNavigator.currentOrThrow
    // FullScreen — глобальный оверлей: пушим на корневой навигатор, иначе из
    // вложенного навигатора (открытая коллекция) экран впишется в область таба,
    // оставив нижние навбары. Fallback на локальный, если корневой недоступен.
    val mainNavigator = LocalMainNavigator.current
    val rootVm = LocalRootScreenModel.current
    val haptic = LocalHapticFeedback.current

    // Без `by`: позиция скролла меняется каждый кадр, и разворачивать её здесь
    // нельзя — чтение на этом уровне перекомпоновывало бы весь экран на каждом
    // кадре прокрутки. State отдаётся скроллбару лямбдой, см. VerticalScrollbar.
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyStaggeredGrid( host.state, 0 )

    val thumbnailsSize = Settings.thumbalistSize.field.collectAsStateWithLifecycle().value

    /** Показывать ли кнопку "вверх" */
    val showScrollToTop by remember { derivedStateOf { host.state.firstVisibleItemIndex > 2 } }

    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Theme.background)) {

        LazyVerticalStaggeredGrid(
            state = host.state,
            columns = StaggeredGridCells.Fixed(host.columns),
            modifier = Modifier.fillMaxSize().then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
        ) {

            item(span = StaggeredGridItemSpan.FullLine) { itemBefore() }

            if (showInitialLoading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    //Загрузка элементов...
                    InitialPictureItemsLoading()
                }
            }

            // Ключ обязан быть уникальным, иначе LazyLayout падает с
            // IllegalArgumentException "Key ... was already used". У PicsDetails нет id,
            // а url_to_original не уникален: normalizePictureUrls подменяет его на
            // lBestThumbnailImageUrl(), и одна и та же картинка, добавленная в альбом
            // дважды, даёт две записи с одинаковым URL. Добавляем индекс — порядок
            // списка стабильный (страницы дописываются в хвост), поэтому идентичность
            // уже показанных элементов сохраняется.
            itemsIndexed( host.filteredPic, key = { index, item -> "${item.url_to_original}#$index" } )
            { index, item ->

                //if (item.url_to_original != null)
                //{
                    Box( modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center )
                    {
                        val aspect = item.width.toFloat() / item.height

                        val previewUrl = item.lPreviewImageUrl(thumbnailsSize)
                        val videoUrl = item.lAnimationVideoUrl()
                        var playInline by remember(item.url_to_original, item.url_to_video) { mutableStateOf(false) }

                        fun openFullScreen() {
                            fullScreenImageFilteredPicArray = host.filteredPic.toList()
                            (mainNavigator ?: navigator).push(
                                L_FullScreenImage(
                                    item = item,
                                    onClose = { position ->
                                        Timber.i("scrollToItem $position")
                                        if (position != -1) {
                                            rootVm.screenModelScope.launch {
                                                host.state.scrollToItem(position)
                                                delay(100)
                                            }
                                        }
                                    },
                                    albumName = host.albumName,
                                    // Меню элемента ждёт числовой id, а не имя источника:
                                    // раньше сюда уходил albumName и это работало только
                                    // потому, что у экрана альбома оба поля совпадают.
                                    idAlbum = host.idAlbum,
                                    expandMenu = expandMenu,
                                    isCollection = isCollection,
                                    autoPlay = true,
                                    isAnimated = item.is_animated,
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(1.dp)
                                .aspectRatio(aspect)
                                //.clipToBounds()
                                .border( width = 0.5.dp, color = Theme.tabLevel4, shape = RoundedCornerShape(4.dp) )
                                .clip(RoundedCornerShape(4.dp))
                                .background(Theme.tabLevel1)
                        )
                        {
                            if (playInline && videoUrl != null) {
                                LInlineAnimationVideo(
                                    url = videoUrl,
                                    previewUrl = previewUrl,
                                    albumName = host.albumName,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (previewUrl.isNotBlank() && !previewUrl.isLVideoFileUrl()) {
                                UrlImage(
                                    url = previewUrl,
                                    contentScale = ContentScale.FillHeight,
                                    urlGif = item.url_to_original,
                                    modifier = Modifier.fillMaxSize(),
                                    albumName = host.albumName,
                                    isAnimated = false,
                                    backgroung = Theme.tabLevel1
                                )
                            } else {
                                AnimatedVideoPlaceholder(modifier = Modifier.fillMaxSize())
                            }

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .combinedClickable(
                                        onClick = {
                                            if (videoUrl != null) {
                                                playInline = true
                                            } else {
                                                openFullScreen()
                                            }
                                        },
                                        onLongClick = {
                                            if (videoUrl != null) {
                                                openFullScreen()
                                            }
                                        }
                                    )
                            )

                            if (videoUrl != null && !playInline) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                )
                            }
                        }

                        Text(
                            index.toString(),
                            modifier = Modifier.padding(start = 4.dp).align(Alignment.TopStart),
                            color = Theme.L.textColor,
                            style = Theme.L.Type.mediaIndex
                        )

                        Box(modifier = Modifier.align(Alignment.TopEnd)) { expandMenuViewModel.ExpandMenu( expandMenu, item, host.idAlbum, isCollection ) }
                    }
                //}
            }
        }

        /** Единственный экземпляр P2P-хоста на весь список (не в item'ах!) */
        expandMenuViewModel.P2pShareHost()

        /** Вертикальный индикатор прокрутки */
        Box( modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd).width(2.dp) ) { VerticalScrollbar { scrollPercent.value } }

        /** FloatingButton "Вверх" */
        AnimatedVisibility( visible = showScrollToTop, modifier = Modifier.align(Alignment.BottomEnd), enter = fadeIn() + scaleIn(),  exit = fadeOut() + scaleOut() )
        {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback( androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm )
                    scope.launch { host.state.scrollToItem(0) }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon( imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top" )
            }
        }

    }
}

@Composable
private fun InitialPictureItemsLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Theme.L.g0)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Загрузка элементов...",
                color = Theme.L.textColor,
                style = Theme.L.Type.rowSubtitle
            )
        }
    }
}

@Composable
private fun LInlineAnimationVideo(
    url: String,
    previewUrl: String,
    albumName: String,
    modifier: Modifier = Modifier
) {
    val isInspection = androidx.compose.ui.platform.LocalInspectionMode.current
    val playerHost = remember(url) {
        MediaPlayerHost(
            mediaUrl = url,
            isPaused = false,
            isMuted = true,
            headers = lMediaRequestHeaders()
        )
    }
    var playbackError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(playerHost) {
        if (!isInspection) {
            playerHost.videoFitMode = ScreenResize.FILL
            playerHost.onError = {
                playbackError = true
                Timber.e("!!! L inline video error: ${it.message}")
            }
            playerHost.play()
        }
    }

    Box(modifier = modifier) {
        if (!isInspection) {
            VideoPlayerWithMenuContent(
                modifier = Modifier.fillMaxSize(),
                playerHost = playerHost,
                onClick = {},
                autoRotate = false
            )
        }

        AnimatedVisibility(
            visible = playerHost.poster || playbackError || isInspection,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (previewUrl.isNotBlank() && !previewUrl.isLVideoFileUrl()) {
                UrlImage(
                    url = previewUrl,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.fillMaxSize(),
                    albumName = albumName,
                    isAnimated = false
                )
            } else {
                AnimatedVideoPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }

        if (playerHost.poster && !playbackError && !isInspection) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.LightGray
            )
        }
    }
}

@Composable
private fun AnimatedVideoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Theme.L.grey6),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun AnimatedVideoPlaceholderPreview() {
    com.client.xvideos.ui.theme.XvideosTheme {
        AnimatedVideoPlaceholder(
            modifier = Modifier
                .width(200.dp)
                .height(150.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun InitialPictureItemsLoadingPreview() {
    com.client.xvideos.ui.theme.XvideosTheme {
        InitialPictureItemsLoading()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun LInlineAnimationVideoPreview() {
    com.client.xvideos.ui.theme.XvideosTheme {
        LInlineAnimationVideo(
            url = "https://sample.com/video.mp4",
            previewUrl = "https://ah-img.luscious.net/Joking42/499900/sample_3941cb87cea03_01J9ZXQ9XTDKY6PQ01ZRWF1FFZ.1680x0.jpg",
            albumName = "Sample Album",
            modifier = Modifier
                .width(200.dp)
                .height(150.dp)
        )
    }
}
