package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.di.rememberApplicationScope
import com.client.xvideos.common.navigation.LocalMainNavigator
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.FloatingScrollButtons
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForLazyStaggeredGrid
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenu
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuP2pHost
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuViewModel
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.atom.InitialPictureItemsLoading
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.molecule.LPictureGridItem
import com.client.xvideos.l.ui.screens.screenFullScreen.L_FullScreenImage
import com.client.xvideos.l.ui.screens.screenFullScreen.model.LFullScreenPayload
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(DelicateCoroutinesApi::class)
@Composable
fun L_LazyRowPictureDetails(
    host: LazyRowPictureDetailsHost,
    itemBefore: @Composable () -> Unit = {},
    expandMenu: ExpandMenuType,
    tag: String = "",
    showInitialLoading: Boolean = false,
    isCollection: Boolean = false,
    showScrollButtons: Boolean = true
) {
    val expandMenuViewModel: ExpandMenuViewModel = hiltViewModel()
    val navigator = LocalNavigator.currentOrThrow
    // FullScreen — глобальный оверлей: пушим на корневой навигатор, иначе из
    // вложенного навигатора (открытая коллекция) экран впишется в область таба,
    // оставив нижние навбары. Fallback на локальный, если корневой недоступен.
    val mainNavigator = LocalMainNavigator.current
    // Не rememberCoroutineScope(): доскролл запускается из колбэка закрытия
    // полноэкранного просмотра, когда эта лента лежит в бэкстеке и уже не
    // скомпонована — такой scope к тому моменту отменён. Раньше здесь брали
    // screenModelScope корневого экрана, что то же самое по времени жизни.
    val appScope = rememberApplicationScope()
    val haptic = LocalHapticFeedback.current

    // Без `by`: позиция скролла меняется каждый кадр, и разворачивать её здесь
    // нельзя — чтение на этом уровне перекомпоновывало бы весь экран на каждом
    // кадре прокрутки. State отдаётся скроллбару лямбдой, см. VerticalScrollbar.
    val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForLazyStaggeredGrid( host.state, 0 )

    val thumbnailsSize by Settings.thumbalistSize.field.collectAsStateWithLifecycle()

    /** Показывать ли кнопку "вверх" */
    val showScrollToTop by remember(host.state) { derivedStateOf { host.state.firstVisibleItemIndex > 2 } }

    /** Показывать ли кнопку "вниз" */
    val showScrollToBottom by remember(host.state) {
        derivedStateOf {
            val layoutInfo = host.state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo
            if (totalItems <= 4 || visible.isEmpty()) {
                false
            } else {
                val lastVisibleIndex = visible.maxOf { it.index }
                lastVisibleIndex < totalItems - 1
            }
        }
    }

    /** Активный диапазон элементов для загрузки: видимые на экране + буфер перед и после скролла */
    val activeItemRange by rememberActiveItemRange(host, showInitialLoading)

    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize().background(Theme.background)) {

        LazyVerticalStaggeredGrid(
            state = host.state,
            columns = StaggeredGridCells.Fixed(host.columns),
            modifier = Modifier.fillMaxSize().hazeSource(hazeState).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
        ) {

            item(span = StaggeredGridItemSpan.FullLine, contentType = "header_before") { itemBefore() }

            if (showInitialLoading) {
                item(span = StaggeredGridItemSpan.FullLine, contentType = "loading_placeholder") {
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
            itemsIndexed(
                items = host.filteredPic,
                key = { index, item -> "${item.url_to_original}#$index" },
                contentType = { _, _ -> "picture_grid_item" }
            ) { index, item ->
                fun openFullScreen() {
                    val payloadKey = LFullScreenPayload.put(host.filteredPic.toList())
                    (mainNavigator ?: navigator).push(
                        L_FullScreenImage(
                            item = item,
                            payloadKey = payloadKey,
                            onClose = { position ->
                                Timber.d("scrollToItem $position")
                                val targetIndex = calculateGridScrollIndex(position, host.filteredPic.size, showInitialLoading)
                                if (targetIndex != null) {
                                    appScope.launch {
                                        withContext(Dispatchers.Main) {
                                            host.state.scrollToItem(targetIndex)
                                        }
                                    }
                                }
                            },
                            albumName = host.albumName,
                            idAlbum = host.idAlbum,
                            expandMenu = expandMenu,
                            isCollection = isCollection,
                            autoPlay = true,
                            isAnimated = item.is_animated,
                        )
                    )
                }

                LPictureGridItem(
                    item = item,
                    index = index,
                    thumbnailsSize = thumbnailsSize,
                    isVisible = index in activeItemRange,
                    albumName = host.albumName,
                    onOpenFullScreen = ::openFullScreen,
                    menuContent = {
                        ExpandMenu(expandMenu, item, host.idAlbum, expandMenuViewModel, isCollection = isCollection, host = host)
                    }
                )
            }
        }

        /** Единственный экземпляр P2P-хоста на весь список (не в item'ах!) */
        ExpandMenuP2pHost(expandMenuViewModel)

        /** Вертикальный индикатор прокрутки */
        Box( modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd).width(2.dp) ) { VerticalScrollbar { scrollPercent.value } }

        val onScrollToTop: () -> Unit = remember(haptic, scope, host) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch { host.state.scrollToItem(0) }
            }
        }
        val onScrollToBottom: () -> Unit = remember(haptic, scope, host, showInitialLoading) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch {
                    val lastIndex = host.filteredPic.lastIndex
                    if (lastIndex >= 0) {
                        val target = calculateGridScrollIndex(
                            position = lastIndex,
                            itemCount = host.filteredPic.size,
                            showInitialLoading = showInitialLoading
                        ) ?: (host.filteredPic.size + 1)
                        host.state.scrollToItem(target)
                    }
                }
            }
        }

        /** FloatingButtons "Вверх" и "Вниз" */
        FloatingScrollButtons(
            visible = showScrollButtons,
            showScrollToTop = showScrollToTop,
            showScrollToBottom = showScrollToBottom,
            hazeState = hazeState,
            contentColor = Theme.ScrollFab.contentColorL,
            onScrollToTop = onScrollToTop,
            onScrollToBottom = onScrollToBottom,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun L_LazyRowPictureDetailsPreview() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        InitialPictureItemsLoading()
    }
}

@Composable
private fun rememberActiveItemRange(
    host: LazyRowPictureDetailsHost,
    showInitialLoading: Boolean
): State<IntRange> {
    return remember(host, showInitialLoading) {
        derivedStateOf {
            val visible = host.state.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                0..15
            } else {
                val headerOffset = if (showInitialLoading) 2 else 1
                val first = (visible.minOf { it.index } - headerOffset).coerceAtLeast(0)
                val last = (visible.maxOf { it.index } - headerOffset).coerceAtLeast(0)
                val bufferBefore = host.columns
                val bufferAfter = host.columns * 2
                ((first - bufferBefore).coerceAtLeast(0))..(last + bufferAfter)
            }
        }
    }
}

internal fun calculateGridScrollIndex(position: Int, itemCount: Int, showInitialLoading: Boolean): Int? {
    if (position !in 0 until itemCount) return null
    val headerOffset = if (showInitialLoading) 2 else 1
    return position + headerOffset
}

