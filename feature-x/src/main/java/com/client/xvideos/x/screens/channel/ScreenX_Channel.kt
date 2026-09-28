package com.client.xvideos.x.screens.channel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.icons.IconFavorite18
import com.client.xvideos.common.icons.IconSave18
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.ChannelCollaborator
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.channel.atom.ChannelHeader
import com.client.xvideos.x.screens.channel.atom.ChannelModelFilterBar
import com.client.xvideos.x.screens.channel.atom.ChannelSortBar
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Экран канала автора/студии X (`ScreenX_Channel`).
 *
 * Отображает обложку-баннер, аватарку, статистику, блок описания «Обо мне»,
 * переключатель сортировок («Свежие», «Новые», «Топ»), фильтр поиска по моделям
 * и горизонтальный пейджер страниц видео с плавно схлопывающейся шапкой при скролле.
 *
 * @param slug Идентификатор канала (например, `"dart_oficial"`).
 * @param initialModel Исходная модель автора из блока тегов плеера (для быстрого первого кадра).
 * @param isModel Флаг профиля актрисы/модели (`true`) или канала (`false`).
 */
class ScreenX_Channel(
    val slug: String,
    val initialModel: TagsMainUploaderPornstar? = null,
    val isModel: Boolean = false,
) : Screen {

    override val key: ScreenKey = "ScreenX_Channel:${if (isModel) "model" else "channel"}:$slug"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenX_ChannelSM, ScreenX_ChannelSM.Factory> { factory ->
            factory.create(slug, initialModel, isModel)
        }

        val onBack = remember(navigator) { { navigator.pop().let {} } }

        val onOpenVideo = remember(navigator) {
            { item: ItemsX ->
                navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item))
            }
        }

        val downloadedVideoIds by vm.saved.downloads.downloadedVideoIds.collectAsState()
        val isFavorite: (Long) -> Boolean = remember(vm) { { id -> vm.isFavorite(id) } }
        val isDownloaded: (Long) -> Boolean = remember(downloadedVideoIds) { { id -> downloadedVideoIds.contains(id) } }
        val onFavoriteAdd: (ItemsX) -> Unit = remember(vm) { { item -> vm.addFavorite(item) } }
        val onFavoriteRemove: (ItemsX) -> Unit = remember(vm) { { item -> vm.removeFavorite(item) } }
        val onDownload: (ItemsX) -> Unit = remember(vm) { { item -> vm.download(item) } }
        val onSaveToGallery: (ItemsX) -> Unit = remember(vm) { { item -> vm.saveToGallery(item) } }

        val onCollaboratorClick = remember(navigator) {
            { collaborator: ChannelCollaborator ->
                val targetSlug = collaborator.cleanSlug
                if (targetSlug.isNotBlank()) {
                    navigator.push(
                        ScreenX_Channel(
                            slug = targetSlug,
                            isModel = collaborator.isModel,
                        )
                    )
                }
            }
        }

        val onRankingClick = remember(navigator) {
            { targetUrl: String, title: String ->
                if (targetUrl.isNotBlank()) {
                    navigator.push(
                        com.client.xvideos.x.screens.actresses.ScreenX_ActressesIndex(
                            urlPath = targetUrl,
                            initialTitle = title,
                        )
                    )
                }
            }
        }

        ChannelScreenContent(
            uiState = vm.uiState,
            pagesCache = vm.pagesCache,
            loadingPages = vm.loadingPages,
            errorPages = vm.errorPages,
            getGridState = vm::getGridState,
            onLoadPage = vm::loadPage,
            onRetryPage = vm::retryPage,
            initialPage = vm.currentPage,
            onCurrentPageChange = { page -> vm.currentPage = page },
            onBack = onBack,
            isSubscribed = vm.isSubscribed,
            onToggleSubscription = vm::toggleSubscription,
            onSortChange = vm::changeSort,
            onRetryInitial = vm::loadInitial,
            onOpenVideo = onOpenVideo,
            isFavorite = isFavorite,
            isDownloaded = isDownloaded,
            onFavoriteAdd = onFavoriteAdd,
            onFavoriteRemove = onFavoriteRemove,
            onDownload = onDownload,
            onSaveToGallery = onSaveToGallery,
            onCollaboratorClick = onCollaboratorClick,
            onRankingClick = onRankingClick,
            onSelectModel = vm::selectModel,
            onModelQueryChange = vm::onModelFilterQueryChange,
            onModelExpandedChange = vm::setModelFilterExpanded,
        )
    }
}

@Composable
fun ChannelScreenContent(
    uiState: com.client.xvideos.x.model.ChannelUiState,
    pagesCache: Map<Int, List<ItemsX>>,
    loadingPages: Set<Int>,
    errorPages: Map<Int, String>,
    getGridState: (Int) -> LazyGridState,
    onLoadPage: (Int) -> Unit,
    onRetryPage: (Int) -> Unit,
    initialPage: Int,
    onCurrentPageChange: (Int) -> Unit,
    onBack: () -> Unit,
    isSubscribed: Boolean = false,
    onToggleSubscription: () -> Unit = {},
    onSortChange: (com.client.xvideos.x.model.ChannelSortOrder) -> Unit,
    onRetryInitial: () -> Unit,
    onOpenVideo: (ItemsX) -> Unit,
    isFavorite: (Long) -> Boolean = { false },
    isDownloaded: (Long) -> Boolean = { false },
    onFavoriteAdd: (ItemsX) -> Unit = {},
    onFavoriteRemove: (ItemsX) -> Unit = {},
    onDownload: (ItemsX) -> Unit = {},
    onSaveToGallery: (ItemsX) -> Unit = {},
    onCollaboratorClick: (ChannelCollaborator) -> Unit = {},
    onRankingClick: (targetUrl: String, title: String) -> Unit = { _, _ -> },
    onSelectModel: (ChannelModelFilterItem?) -> Unit = {},
    onModelQueryChange: (String) -> Unit = {},
    onModelExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoadingInitial && uiState.header.name.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF040404)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFDE2600))
        }
        return
    }

    if (uiState.error != null && uiState.header.name.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF040404))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.error,
                    color = Color(0xFFCCCCCC),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetryInitial,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE2600))
                ) {
                    Text("Повторить", color = Color.White)
                }
            }
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (uiState.maxPages - 1).coerceAtLeast(0))
    ) { uiState.maxPages }

    LaunchedEffect(pagerState.currentPage) {
        onCurrentPageChange(pagerState.currentPage)
    }

    // При возврате назад: если мы не на 0-й странице, сначала возвращаемся на страницу 0
    val handleBack: () -> Unit = remember(pagerState.currentPage, coroutineScope, onBack) {
        {
            if (pagerState.currentPage > 0) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }.let {}
            } else {
                onBack()
            }
        }
    }
    BackHandler(onBack = handleBack)

    var headerOffsetPx by rememberSaveable { mutableFloatStateOf(0f) }
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val topCutout = getTopInsetDp()
    val density = LocalDensity.current
    val topCutoutPx = with(density) { topCutout.roundToPx() }

    // Сброс на страницу 0 и раскрытие шапки при смене сортировки или фильтра
    LaunchedEffect(uiState.currentSort, uiState.selectedModel) {
        pagerState.scrollToPage(0)
        headerOffsetPx = 0f
    }

    val nestedScrollConnection = remember(headerHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f && headerHeightPx > 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0f && headerHeightPx > 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumedY = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }
        }
    }

    val onPageChange: (Int) -> Unit = remember(pagerState, coroutineScope, uiState.maxPages) {
        { targetPage ->
            val clamped = targetPage.coerceIn(0, (uiState.maxPages - 1).coerceAtLeast(0))
            coroutineScope.launch {
                pagerState.animateScrollToPage(clamped)
            }
        }
    }

    val bottomBarContent: @Composable () -> Unit = remember(pagerState.currentPage, uiState.maxPages, onPageChange) {
        {
            BottomListDashBoardNavigationButtons2(
                value = pagerState.currentPage,
                onChange = onPageChange,
                max = uiState.maxPages,
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF040404),
        bottomBar = {
            if (uiState.maxPages > 1) {
                bottomBarContent()
            }
        },
    ) { paddingValues ->
        ChannelCollapsingLayout(
            headerOffsetPx = headerOffsetPx,
            onHeaderHeightMeasured = { height ->
                if (headerHeightPx != height) {
                    headerHeightPx = height
                }
            },
            topInsetPx = topCutoutPx,
            header = {
                ChannelHeader(
                    header = uiState.header,
                    onBack = onBack,
                    isSubscribed = isSubscribed,
                    onToggleSubscription = onToggleSubscription,
                    onCollaboratorClick = onCollaboratorClick,
                    onRankingClick = onRankingClick,
                )
            },
            stickyBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040404))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = headerOffsetPx < -80f,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally(),
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        ChannelSortBar(
                            selectedSort = uiState.currentSort,
                            onSortChange = onSortChange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (uiState.hasModelFilters) {
                        ChannelModelFilterBar(
                            header = uiState.header,
                            selectedModel = uiState.selectedModel,
                            filteredModels = uiState.filteredModels,
                            searchQuery = uiState.modelFilterQuery,
                            isExpanded = uiState.isModelFilterExpanded,
                            onQueryChange = onModelQueryChange,
                            onExpandedChange = onModelExpandedChange,
                            onSelectModel = onSelectModel,
                            totalVideos = uiState.videos.size,
                        )
                    }
                }
            },
            pager = {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { pageIndex -> pageIndex }
                ) { page ->
                    val pageVideos = pagesCache[page]
                    val isPageLoading = page in loadingPages
                    val pageError = errorPages[page]
                    val gridState = getGridState(page)

                    LaunchedEffect(page, uiState.currentSort, uiState.selectedModel) {
                        if (pageVideos == null && !isPageLoading && pageError == null) {
                            onLoadPage(page)
                        }
                    }

                    if (isPageLoading && pageVideos == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFDE2600),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else if (pageError != null && pageVideos == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = pageError,
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onRetryPage(page) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE2600))
                                ) {
                                    Text("Повторить", color = Color.White)
                                }
                            }
                        }
                    } else if (pageVideos != null && pageVideos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.header.isModel) {
                                    "У этой модели пока нет опубликованных видео"
                                } else {
                                    "У этого канала пока нет опубликованных видео"
                                },
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (pageVideos != null) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = pageVideos,
                                key = { it.id }
                            ) { video ->
                                ChannelVideoItem(
                                    item = video,
                                    isFavorite = isFavorite(video.id),
                                    isDownloaded = isDownloaded(video.id),
                                    onOpenVideo = onOpenVideo,
                                    onFavoriteAdd = onFavoriteAdd,
                                    onFavoriteRemove = onFavoriteRemove,
                                    onDownload = onDownload,
                                    onSaveToGallery = onSaveToGallery,
                                )
                            }
                        }
                    }
                }
            },
            statusCover = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topCutout)
                        .background(Color(0xFF040404))
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
        )
    }
}

/**
 * Кастомный макет для схлопывающейся шапки профиля канала, липкой панели сортировок/фильтров
 * и горизонтального пейджера страниц с видеороликами.
 */
@Composable
private fun ChannelCollapsingLayout(
    headerOffsetPx: Float,
    onHeaderHeightMeasured: (Float) -> Unit,
    topInsetPx: Int,
    header: @Composable () -> Unit,
    stickyBar: @Composable () -> Unit,
    pager: @Composable () -> Unit,
    statusCover: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            header()
            stickyBar()
            pager()
            statusCover()
        },
        modifier = modifier
    ) { measurables, constraints ->
        val headerMeasurable = measurables.getOrNull(0)
        val stickyBarMeasurable = measurables.getOrNull(1)
        val pagerMeasurable = measurables.getOrNull(2)
        val statusCoverMeasurable = measurables.getOrNull(3)

        val headerPlaceable = headerMeasurable?.measure(constraints.copy(minHeight = 0))
        val headerHeight = headerPlaceable?.height ?: 0
        onHeaderHeightMeasured(headerHeight.toFloat())

        val stickyBarPlaceable = stickyBarMeasurable?.measure(constraints.copy(minHeight = 0))
        val stickyBarHeight = stickyBarPlaceable?.height ?: 0

        val availablePagerHeight = (constraints.maxHeight - stickyBarHeight - topInsetPx).coerceAtLeast(0)
        val pagerPlaceable = pagerMeasurable?.measure(
            constraints.copy(minHeight = availablePagerHeight, maxHeight = availablePagerHeight)
        )

        val statusCoverPlaceable = statusCoverMeasurable?.measure(
            constraints.copy(minHeight = topInsetPx, maxHeight = topInsetPx)
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            val offset = headerOffsetPx.roundToInt()
            val headerY = topInsetPx + offset
            val stickyY = (topInsetPx + headerHeight + offset).coerceAtLeast(topInsetPx)
            val pagerY = stickyY + stickyBarHeight

            // Порядок отрисовки слоёв:
            // 1. Пейджер снизу
            pagerPlaceable?.placeWithLayer(0, pagerY)
            // 2. Шапка профиля
            headerPlaceable?.placeWithLayer(0, headerY)
            // 3. Липкая панель сортировок (перекрывает шапку при схлопывании)
            stickyBarPlaceable?.placeWithLayer(0, stickyY)
            // 4. Плашка выреза под строку состояния в самом верху
            statusCoverPlaceable?.placeWithLayer(0, 0)
        }
    }
}

@Composable
private fun ChannelVideoItem(
    item: ItemsX,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onOpenVideo: (ItemsX) -> Unit,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(item, onOpenVideo) { { onOpenVideo(item) } }
    val handleFavoriteAdd = remember(item, onFavoriteAdd) { { onFavoriteAdd(item) } }
    val handleFavoriteRemove = remember(item, onFavoriteRemove) { { onFavoriteRemove(item) } }
    val handleDownload = remember(item, onDownload) { { onDownload(item) } }
    val handleSaveToGallery = remember(item, onSaveToGallery) { { onSaveToGallery(item) } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(352f / 198f)
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141418))
    ) {
        UrlVideoImageAndLongClickX(
            item = item,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
        ) {
            // Длительность, иконка избранного и индикатор скачивания в правом нижнем углу
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isDownloaded) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xB3000000))
                            .padding(2.dp)
                    ) {
                        IconSave18()
                    }
                }
                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xB3000000))
                            .padding(2.dp)
                    ) {
                        IconFavorite18()
                    }
                }
                if (item.duration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xB3000000))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.duration,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            // Количество просмотров в левом нижнем углу
            if (item.views.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xB3000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.views,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDDDDDD)
                    )
                }
            }

            // Меню с тремя точками в правом верхнем углу
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                X_DashboardExpandMenu(
                    isFavorite = isFavorite,
                    onFavoriteAdd = handleFavoriteAdd,
                    onFavoriteRemove = handleFavoriteRemove,
                    onDownload = handleDownload,
                    onSaveToGallery = handleSaveToGallery,
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun ChannelVideoItemPreview() {
    com.client.xvideos.ui.theme.XvideosTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            ChannelVideoItem(
                item = ItemsX(
                    id = 12345L,
                    title = "Sample Video Title",
                    duration = "12:34",
                    views = "1.2M",
                    channel = "Sample Channel",
                    href = "/video12345",
                    nameProfile = "Sample Channel",
                    linkProfile = "/channels/sample",
                ),
                isFavorite = true,
                isDownloaded = true,
                onOpenVideo = {},
                onFavoriteAdd = {},
                onFavoriteRemove = {},
                onDownload = {},
                onSaveToGallery = {},
            )
        }
    }
}
