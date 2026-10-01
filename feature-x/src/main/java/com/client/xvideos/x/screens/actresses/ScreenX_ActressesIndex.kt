package com.client.xvideos.x.screens.actresses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.x.model.ActressesIndexDropdownType
import com.client.xvideos.x.model.ActressesIndexFilterOption
import com.client.xvideos.x.model.ActressesIndexItem
import com.client.xvideos.x.model.ActressesIndexUiState
import com.client.xvideos.x.screens.actresses.molecule.ActressesIndexGrid
import com.client.xvideos.x.screens.actresses.molecule.ActressesIndexTopBar
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

        ScreenX_ActressesIndexContent(
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
fun ScreenX_ActressesIndexContent(
    uiState: ActressesIndexUiState,
    onBack: () -> Unit,
    onActressClick: (ActressesIndexItem) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onToggleDropdown: (ActressesIndexDropdownType) -> Unit,
    onSelectOption: (ActressesIndexFilterOption) -> Unit,
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

@Preview
@Composable
private fun ScreenX_ActressesIndexContentPreview() {
    ScreenX_ActressesIndexContent(
        uiState = ActressesIndexUiState(),
        onBack = {},
        onActressClick = {},
        onLoadMore = {},
        onRetry = {},
        onToggleDropdown = {},
        onSelectOption = {},
        onSearchQueryChange = {},
        onCloseDropdown = {}
    )
}
