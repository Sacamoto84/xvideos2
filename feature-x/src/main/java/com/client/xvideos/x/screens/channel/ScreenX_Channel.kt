package com.client.xvideos.x.screens.channel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
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
import com.client.xvideos.x.model.ChannelSortOrder
import com.client.xvideos.x.model.ChannelUiState
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ProfileType
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.channel.atom.ChannelStatusCover
import com.client.xvideos.x.screens.channel.model.rememberChannelHeaderCollapseState
import com.client.xvideos.x.screens.channel.molecule.ChannelCollapsingLayout
import com.client.xvideos.x.screens.channel.molecule.ChannelErrorView
import com.client.xvideos.x.screens.channel.molecule.ChannelHeader
import com.client.xvideos.x.screens.channel.molecule.ChannelPagerBottomBar
import com.client.xvideos.x.screens.channel.molecule.ChannelStickyBar
import com.client.xvideos.x.screens.channel.molecule.ChannelVideosPager
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
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

        ScreenX_ChannelContent(
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

@Suppress("LongParameterList")
@Composable
fun ScreenX_ChannelContent(
    uiState: ChannelUiState,
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
    onSortChange: (ChannelSortOrder) -> Unit,
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
    // Пустое имя — только у некорректного адреса: ошибка на весь экран. Загрузку и сбой
    // при корректном адресе показывает страница 0 пейджера, шапка остаётся с именем.
    if (uiState.error != null && uiState.header.name.isBlank()) {
        ChannelErrorView(message = uiState.error, onRetry = onRetryInitial, modifier = modifier)
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

    val collapse = rememberChannelHeaderCollapseState { getGridState(pagerState.currentPage) }
    val topCutout = getTopInsetDp()
    val density = LocalDensity.current
    val topCutoutPx = with(density) { topCutout.roundToPx() }

    // Сброс на страницу 0 при смене сортировки или фильтра (шапка сохраняет позицию)
    LaunchedEffect(uiState.currentSort, uiState.selectedModel) {
        pagerState.scrollToPage(0)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF040404),
        bottomBar = {
            if (uiState.maxPages > 1) {
                ChannelPagerBottomBar(pagerState = pagerState, maxPages = uiState.maxPages)
            }
        },
    ) { paddingValues ->
        ChannelCollapsingLayout(
            headerOffsetPx = collapse.headerOffsetPx,
            topInsetPx = topCutoutPx,
            header = {
                ChannelHeader(
                    header = uiState.header,
                    onBack = handleBack,
                    isSubscribed = isSubscribed,
                    onToggleSubscription = onToggleSubscription,
                    onCollaboratorClick = onCollaboratorClick,
                    onRankingClick = onRankingClick,
                    modifier = collapse.scrollModifier
                        .onSizeChanged { size -> collapse.onHeaderHeightChanged(size.height.toFloat()) },
                )
            },
            stickyBar = {
                ChannelStickyBar(
                    uiState = uiState,
                    showBackButton = collapse.isBackInStickyBar,
                    onBack = handleBack,
                    onSortChange = onSortChange,
                    onModelQueryChange = onModelQueryChange,
                    onModelExpandedChange = onModelExpandedChange,
                    onSelectModel = onSelectModel,
                    modifier = collapse.scrollModifier,
                )
            },
            pager = {
                ChannelVideosPager(
                    pagerState = pagerState,
                    pagesCache = pagesCache,
                    loadingPages = loadingPages,
                    errorPages = errorPages,
                    currentSort = uiState.currentSort,
                    selectedModel = uiState.selectedModel,
                    isModel = uiState.header.isModel,
                    getGridState = getGridState,
                    onLoadPage = onLoadPage,
                    onRetryPage = onRetryPage,
                    onOpenVideo = onOpenVideo,
                    isFavorite = isFavorite,
                    isDownloaded = isDownloaded,
                    onFavoriteAdd = onFavoriteAdd,
                    onFavoriteRemove = onFavoriteRemove,
                    onDownload = onDownload,
                    onSaveToGallery = onSaveToGallery,
                    headerScrollModifier = collapse.scrollModifier,
                )
            },
            statusCover = {
                ChannelStatusCover(height = topCutout, modifier = collapse.scrollModifier)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clipToBounds()
                .nestedScroll(collapse.nestedScrollConnection)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun ScreenX_ChannelContentPreview() {
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
        totalVideosCount = 36,
    )
    val gridState = rememberLazyGridState()

    XvideosTheme(darkTheme = true) {
        ScreenX_ChannelContent(
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


