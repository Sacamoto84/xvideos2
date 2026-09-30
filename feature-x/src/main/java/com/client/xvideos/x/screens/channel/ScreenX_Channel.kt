package com.client.xvideos.x.screens.channel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.x.model.ChannelCollaborator
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.model.ChannelUiState
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ProfileType
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.channel.molecule.ChannelCollapsingLayout
import com.client.xvideos.x.screens.channel.molecule.ChannelHeader
import com.client.xvideos.x.screens.channel.molecule.ChannelModelFilterBar
import com.client.xvideos.x.screens.channel.molecule.ChannelSortBar
import com.client.xvideos.x.screens.channel.molecule.ChannelVideoItem
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
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
    val handleBack: () -> Unit = remember(pagerState, coroutineScope, onBack) {
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

    // Сброс на страницу 0 при смене сортировки или фильтра (шапка сохраняет позицию)
    LaunchedEffect(uiState.currentSort, uiState.selectedModel) {
        pagerState.scrollToPage(0)
    }

    var flingAnimationJob by remember { mutableStateOf<Job?>(null) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    flingAnimationJob?.cancel()
                    flingAnimationJob = null
                }
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
                // Изоляция скролла списка: достижение верха сетки видео не стягивает шапку вниз
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y < 0f && headerOffsetPx > -headerHeightPx && headerHeightPx > 0f) {
                    flingAnimationJob?.cancel()
                    val anim = Animatable(headerOffsetPx)
                    flingAnimationJob = coroutineScope.launch {
                        anim.animateTo(
                            targetValue = -headerHeightPx,
                            initialVelocity = available.y,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) {
                            headerOffsetPx = value
                        }
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Изоляция флинга списка: инерция сетки не раскрывает шапку
                return Velocity.Zero
            }
        }
    }

    val headerScrollableState = rememberScrollableState { delta ->
        flingAnimationJob?.cancel()
        flingAnimationJob = null
        var consumed = 0f
        if (delta < 0f) {
            // Палец вверх: схлопываем шапку
            if (headerHeightPx > 0f && headerOffsetPx > -headerHeightPx) {
                val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                val headerConsumed = newOffset - headerOffsetPx
                headerOffsetPx = newOffset
                consumed += headerConsumed
            }
            // Если шапка уже схлопнута, передаем скролл в сетку видео
            val remaining = delta - consumed
            if (remaining < 0f) {
                val gridState = getGridState(pagerState.currentPage)
                val gridConsumed = gridState.dispatchRawDelta(remaining)
                consumed += gridConsumed
            }
        } else if (delta > 0f) {
            // Палец вниз: разворачиваем шапку
            if (headerHeightPx > 0f && headerOffsetPx < 0f) {
                val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                val headerConsumed = newOffset - headerOffsetPx
                headerOffsetPx = newOffset
                consumed += headerConsumed
            }
        }
        consumed
    }

    val headerFlingBehavior = remember(coroutineScope, headerHeightPx) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                if (headerHeightPx <= 0f) return 0f
                flingAnimationJob?.cancel()
                val target = if (initialVelocity > 300f || (initialVelocity >= -300f && headerOffsetPx > -headerHeightPx * 0.5f)) {
                    0f
                } else {
                    -headerHeightPx
                }
                val anim = Animatable(headerOffsetPx)
                flingAnimationJob = coroutineScope.launch {
                    anim.animateTo(
                        targetValue = target,
                        initialVelocity = initialVelocity,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) {
                        headerOffsetPx = value
                    }
                }
                return initialVelocity
            }
        }
    }

    val headerScrollModifier = Modifier.scrollable(
        state = headerScrollableState,
        orientation = Orientation.Vertical,
        flingBehavior = headerFlingBehavior,
    )

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
            topInsetPx = topCutoutPx,
            header = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(headerScrollModifier)
                        .onSizeChanged { size ->
                            val height = size.height.toFloat()
                            if (headerHeightPx != height) {
                                val wasFullyCollapsed = headerHeightPx > 0f && headerOffsetPx <= -headerHeightPx + 1f
                                headerHeightPx = height
                                if (wasFullyCollapsed) {
                                    headerOffsetPx = -height
                                } else if (headerOffsetPx < -height) {
                                    headerOffsetPx = -height
                                }
                            }
                        }
                ) {
                    ChannelHeader(
                        header = uiState.header,
                        onBack = handleBack,
                        isSubscribed = isSubscribed,
                        onToggleSubscription = onToggleSubscription,
                        onCollaboratorClick = onCollaboratorClick,
                        onRankingClick = onRankingClick,
                    )
                }
            },
            stickyBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040404))
                        .then(headerScrollModifier)
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
                                onClick = handleBack,
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
                            totalVideos = uiState.totalVideosCount.takeIf { it > 0 }
                                ?: uiState.header.videoCount,
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
                            modifier = Modifier
                                .fillMaxSize()
                                .then(headerScrollModifier),
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
                                .then(headerScrollModifier)
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
                                .then(headerScrollModifier)
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
                        .then(headerScrollModifier)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clipToBounds()
                .nestedScroll(nestedScrollConnection)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun ChannelScreenContentPreview() {
    val sampleVideos = List(6) { index ->
        ItemsX(
            id = index.toLong(),
            title = "Sample Video Title ${index + 1}",
            duration = "10:20",
            views = "100K",
            channel = "Sample Channel",
            href = "/video$index",
            nameProfile = "Sample Channel",
            linkProfile = "/channels/sample",
        )
    }
    val sampleUiState = ChannelUiState(
        header = ChannelHeaderModel(
            slug = "sample_channel",
            name = "Sample Channel",
            subscribers = "10.5K",
            totalViews = "1.2M",
            aboutMe = "Welcome to the official Sample Channel!",
            videoCount = 36,
            profileType = ProfileType.CHANNEL,
        ),
        isLoadingInitial = false,
        totalVideosCount = 36,
    )
    val gridState = rememberLazyGridState()

    XvideosTheme(darkTheme = true) {
        ChannelScreenContent(
            uiState = sampleUiState,
            pagesCache = mapOf(0 to sampleVideos),
            loadingPages = emptySet(),
            errorPages = emptyMap(),
            getGridState = { gridState },
            onLoadPage = {},
            onRetryPage = {},
            initialPage = 0,
            onCurrentPageChange = {},
            onBack = {},
            onSortChange = {},
            onRetryInitial = {},
            onOpenVideo = {},
        )
    }
}

