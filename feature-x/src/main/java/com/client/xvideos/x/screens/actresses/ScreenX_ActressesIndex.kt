package com.client.xvideos.x.screens.actresses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.ActressesIndexItem
import com.client.xvideos.x.model.ActressesIndexUiState
import com.client.xvideos.x.screens.actresses.atom.ActressCard
import com.client.xvideos.x.screens.actresses.atom.ActressesFilterBar
import com.client.xvideos.x.screens.channel.ScreenX_Channel
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Экран каталога рейтингов актрис/порнозвезд/моделей (`ScreenX_ActressesIndex`).
 *
 * Отображает заголовок каталога, выпадающие фильтры по странам, типам моделей и периодам,
 * а также 2-колоночную сетку карточек моделей с бесконечной прокруткой.
 *
 * @param urlPath Относительный или полный URL каталога (например, `"/porn-actresses-index/from/russia/ever"`).
 * @param initialTitle Начальный заголовок для быстрого отображения.
 */
class ScreenX_ActressesIndex(
    val urlPath: String,
    val initialTitle: String = "",
) : Screen {

    override val key: ScreenKey = "ScreenX_ActressesIndex:$urlPath"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenX_ActressesIndexSM, ScreenX_ActressesIndexSM.Factory> { factory ->
            factory.create(urlPath, initialTitle)
        }

        val onBack = remember(navigator) { { navigator.pop().let {} } }
        BackHandler(onBack = onBack)

        val onActressClick = remember(navigator) {
            { item: ActressesIndexItem ->
                if (item.slug.isNotBlank()) {
                    navigator.push(
                        ScreenX_Channel(
                            slug = item.slug,
                            isModel = true,
                        )
                    )
                }
            }
        }

        ActressesIndexContent(
            uiState = vm.uiState,
            onBack = onBack,
            onActressClick = onActressClick,
            onLoadMore = vm::loadNextPage,
            onRetry = vm::loadInitial,
            onToggleDropdown = vm::toggleDropdown,
            onSelectOption = vm::selectFilterOption,
            onSearchQueryChange = vm::onDropdownSearchQueryChange,
            onCloseDropdown = vm::closeDropdown,
        )
    }
}

@Composable
fun ActressesIndexContent(
    uiState: ActressesIndexUiState,
    onBack: () -> Unit,
    onActressClick: (ActressesIndexItem) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onToggleDropdown: (com.client.xvideos.x.model.ActressesIndexDropdownType) -> Unit,
    onSelectOption: (com.client.xvideos.x.model.ActressesIndexFilterOption) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseDropdown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val topCutout = getTopInsetDp()

    // Бесконечная пагинация при прокрутке вниз
    LaunchedEffect(gridState, uiState.items.size, uiState.isLoadingMore, uiState.isEndReached) {
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
        topBar = {
            ActressesIndexTopBar(
                title = uiState.title,
                subtitle = uiState.catalog.subtitle,
                topCutout = topCutout,
                catalog = uiState.catalog,
                activeDropdown = uiState.activeDropdown,
                searchQuery = uiState.dropdownSearchQuery,
                onBack = onBack,
                onToggleDropdown = onToggleDropdown,
                onSelectOption = onSelectOption,
                onSearchQueryChange = onSearchQueryChange,
                onCloseDropdown = onCloseDropdown,
            )
        }
    ) { paddingValues ->
        ActressesIndexGrid(
            uiState = uiState,
            gridState = gridState,
            onActressClick = onActressClick,
            onRetry = onRetry,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun ActressesIndexGrid(
    uiState: ActressesIndexUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onActressClick: (ActressesIndexItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        if (uiState.isLoadingInitial) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFDE2600))
                }
            }
        } else if (uiState.error != null) {
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
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "По выбранным фильтрам ничего не найдено",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = uiState.items,
                key = { "${it.slug}_${it.rankText}" }
            ) { actress ->
                ActressCard(
                    item = actress,
                    onClick = { onActressClick(actress) },
                )
            }

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
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            if (uiState.isEndReached && uiState.items.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Все модели каталога загружены",
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

@Composable
private fun ActressesIndexTopBar(
    title: String,
    subtitle: String,
    topCutout: androidx.compose.ui.unit.Dp,
    catalog: com.client.xvideos.x.model.ActressesIndexCatalog,
    activeDropdown: com.client.xvideos.x.model.ActressesIndexDropdownType?,
    searchQuery: String,
    onBack: () -> Unit,
    onToggleDropdown: (com.client.xvideos.x.model.ActressesIndexDropdownType) -> Unit,
    onSelectOption: (com.client.xvideos.x.model.ActressesIndexFilterOption) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseDropdown: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0B0E))
            .padding(top = topCutout)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title.ifBlank { "Каталог актрис" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        ActressesFilterBar(
            catalog = catalog,
            activeDropdown = activeDropdown,
            searchQuery = searchQuery,
            onToggleDropdown = onToggleDropdown,
            onSelectOption = onSelectOption,
            onSearchQueryChange = onSearchQueryChange,
            onCloseDropdown = onCloseDropdown,
        )
    }
}
