package com.client.xvideos.x.screens.channel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.ChannelCollaborator
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.channel.atom.ChannelHeader
import com.client.xvideos.x.screens.channel.atom.ChannelModelFilterBar
import com.client.xvideos.x.screens.channel.atom.ChannelSortBar
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Экран канала автора/студии X (`ScreenX_Channel`).
 *
 * Отображает обложку-баннер, аватарку, статистику, блок описания «Обо мне»,
 * переключатель сортировок («Свежие», «Новые», «Топ»), фильтр поиска по моделям
 * и бесконечную ленту видео в 2 колонки.
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
        BackHandler(onBack = onBack)

        val onOpenVideo = remember(navigator) {
            { item: ItemsX ->
                navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item))
            }
        }

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
            onBack = onBack,
            isSubscribed = vm.isSubscribed,
            onToggleSubscription = vm::toggleSubscription,
            onSortChange = vm::changeSort,
            onLoadMore = vm::loadNextPage,
            onRetry = vm::loadInitial,
            onOpenVideo = onOpenVideo,
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
    onBack: () -> Unit,
    isSubscribed: Boolean = false,
    onToggleSubscription: () -> Unit = {},
    onSortChange: (com.client.xvideos.x.model.ChannelSortOrder) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenVideo: (ItemsX) -> Unit,
    onCollaboratorClick: (ChannelCollaborator) -> Unit = {},
    onRankingClick: (targetUrl: String, title: String) -> Unit = { _, _ -> },
    onSelectModel: (ChannelModelFilterItem?) -> Unit = {},
    onModelQueryChange: (String) -> Unit = {},
    onModelExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val topCutout = getTopInsetDp()

    // Триггер бесконечной пагинации при приближении к концу ленты
    LaunchedEffect(gridState, uiState.videos.size, uiState.isLoadingMore, uiState.isEndReached) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 6
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd && !uiState.isLoadingMore && !uiState.isEndReached) {
                onLoadMore()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF040404),
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = topCutout),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Шапка канала (баннер, аватар, имя, подписчики, описание)
            item(span = { GridItemSpan(2) }) {
                ChannelHeader(
                    header = uiState.header,
                    onBack = onBack,
                    isSubscribed = isSubscribed,
                    onToggleSubscription = onToggleSubscription,
                    onCollaboratorClick = onCollaboratorClick,
                    onRankingClick = onRankingClick,
                )
            }

            // 2. Панель сортировки
            item(span = { GridItemSpan(2) }) {
                ChannelSortBar(
                    selectedSort = uiState.currentSort,
                    onSortChange = onSortChange,
                )
            }

            // 2.5 Фильтрация по моделям/каналам (если на странице найдены доступные модели)
            if (uiState.hasModelFilters) {
                item(span = { GridItemSpan(2) }) {
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

            // 3. Состояния: загрузка первой страницы
            if (uiState.isLoadingInitial) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFDE2600))
                    }
                }
            } else if (uiState.error != null) {
                // 4. Ошибка загрузки с кнопкой повтора
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error,
                            color = Color(0xFFCCCCCC),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE2600))
                        ) {
                            Text("Повторить", color = Color.White)
                        }
                    }
                }
            } else if (uiState.isEmpty) {
                // 5. Профиль пуст
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
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
                }
            } else {
                // 6. Сетка видеороликов
                items(
                    items = uiState.videos,
                    key = { it.id }
                ) { video ->
                    ChannelVideoItem(
                        item = video,
                        onOpenVideo = onOpenVideo,
                    )
                }

                // 7. Индикатор подгрузки следующей страницы
                if (uiState.isLoadingMore) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFDE2600),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // 8. Все видео загружены
                if (uiState.isEndReached && uiState.videos.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = if (uiState.header.isModel) {
                                "Все видео модели загружены"
                            } else {
                                "Все видео канала загружены"
                            },
                            color = Color(0xFF666666),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelVideoItem(
    item: ItemsX,
    onOpenVideo: (ItemsX) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(item, onOpenVideo) { { onOpenVideo(item) } }

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
            // Длительность в правом нижнем углу
            if (item.duration.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
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
        }
    }
}
