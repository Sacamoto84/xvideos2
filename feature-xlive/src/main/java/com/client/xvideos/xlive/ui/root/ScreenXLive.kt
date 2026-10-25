package com.client.xvideos.xlive.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.feed.XLiveFeedActions
import com.client.xvideos.xlive.ui.feed.XLiveFeedState
import com.client.xvideos.xlive.ui.feed.atom.XLivePlaceholder
import com.client.xvideos.xlive.ui.feed.molecule.XLiveRoomsGrid
import com.client.xvideos.xlive.ui.room.ScreenXLiveRoom
import com.client.xvideos.xlive.ui.root.molecule.XLiveBottomBar
import com.client.xvideos.xlive.ui.saved.XLiveSavedActions
import com.client.xvideos.xlive.ui.saved.XLiveSavedState
import com.client.xvideos.xlive.ui.saved.XLiveSavedTab
import com.client.xvideos.xlive.ui.saved.molecule.XLiveSavedGrid
import com.client.xvideos.xlive.ui.tag.ScreenXLiveTag
import com.client.xvideos.xlive.ui.tags.XLiveTagsActions
import com.client.xvideos.xlive.ui.tags.XLiveTagsState
import com.client.xvideos.xlive.ui.tags.molecule.XLiveTagsList

/**
 * Корень раздела XLive: нижние вкладки как у X, переключатель пола, вкладки
 * «Эфиры» (сетка комнат), «Теги» (таблица тегов) и «Избранное» («Мои»,
 * «Подписки», «История»). Тап по карточке или превью открывает комнату, тап
 * по тегу — комнаты с ним.
 */
class ScreenXLive : Screen {

    override val key: ScreenKey = "ScreenXLive"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = rememberScreenModel { XLiveSM() }
        val tab by vm.tab.collectAsStateWithLifecycle()
        val gender by vm.gender.collectAsStateWithLifecycle()
        val collection by vm.collection.collectAsStateWithLifecycle()
        val tagSort by vm.tagSort.collectAsStateWithLifecycle()
        val feed by vm.feed.collectAsStateWithLifecycle()
        val tags by vm.tags.collectAsStateWithLifecycle()
        val savedTab by vm.savedTab.collectAsStateWithLifecycle()
        val my by vm.my.collectAsStateWithLifecycle()
        val recent by vm.recent.collectAsStateWithLifecycle()

        val barActions = remember(vm) {
            XLiveBarActions(
                onTab = vm::selectTab,
                onGender = vm::selectGender,
                onCollection = vm::selectCollection,
                onTagSort = vm::selectTagSort,
                onSavedTab = vm::selectSavedTab,
            )
        }
        val feedActions = remember(vm, navigator) {
            XLiveFeedActions(
                onOpenRoom = { room -> navigator.push(ScreenXLiveRoom(room)) },
                onRetry = vm::reload,
                onRetryLoadMore = vm::retryLoadMore,
                onRefresh = vm::refresh,
                onLoadMore = vm::loadMore,
            )
        }
        val tagsActions = remember(vm, navigator) {
            XLiveTagsActions(
                onOpenTag = { tag -> navigator.push(ScreenXLiveTag(tag, vm.gender.value)) },
                onOpenRoom = { room -> navigator.push(ScreenXLiveRoom(room)) },
                onRetry = vm::reloadTags,
                onRetryLoadMore = vm::retryLoadMoreTags,
                onLoadMore = vm::loadMoreTags,
            )
        }

        val savedActions = remember(vm, navigator) {
            XLiveSavedActions(
                onOpenRoom = { room -> navigator.push(ScreenXLiveRoom(room)) },
                onRemove = vm::removeSaved,
                onRefresh = vm::refreshSaved,
            )
        }
        val isHistory = savedTab == XLiveSavedTab.HISTORY

        ScreenXLiveContent(
            bar = XLiveBarState(
                tab = tab,
                gender = gender,
                collection = collection,
                tagSort = tagSort,
                savedTab = savedTab,
            ),
            barActions = barActions,
            feed = feed,
            gridState = vm.gridPosition.rememberState(),
            feedActions = feedActions,
            tags = tags,
            tagsListState = vm.tagsPosition.rememberState(),
            tagsActions = tagsActions,
            saved = if (isHistory) recent else my,
            savedGridState = if (isHistory) vm.recentPosition.rememberState() else vm.myPosition.rememberState(),
            savedActions = savedActions,
        )
    }
}

@Composable
fun ScreenXLiveContent(
    bar: XLiveBarState,
    barActions: XLiveBarActions,
    feed: XLiveFeedState,
    gridState: LazyGridState,
    feedActions: XLiveFeedActions,
    tags: XLiveTagsState,
    tagsListState: LazyListState,
    tagsActions: XLiveTagsActions,
    saved: XLiveSavedState,
    savedGridState: LazyGridState,
    savedActions: XLiveSavedActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = XLiveColors.Background,
        bottomBar = { XLiveBottomBar(bar = bar, actions = barActions) },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        // Сверху — отступ на вырез, как у меню и шапок X.
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top)),
            contentAlignment = Alignment.Center,
        ) {
            when (bar.tab) {
                XLiveTab.ROOMS -> XLiveRoomsGrid(feed = feed, gridState = gridState, actions = feedActions)
                XLiveTab.TAGS -> XLiveTagsList(state = tags, listState = tagsListState, actions = tagsActions)
                XLiveTab.SAVED -> when (bar.savedTab) {
                    XLiveSavedTab.MY -> XLiveSavedGrid(
                        state = saved,
                        gridState = savedGridState,
                        actions = savedActions,
                        emptyText = "Здесь будут комнаты, сохранённые закладкой в шапке комнаты",
                        listName = "«Моих»",
                    )
                    XLiveSavedTab.FOLLOWING -> XLivePlaceholder(text = "Подписки аккаунта на сайте — следующим шагом")
                    XLiveSavedTab.HISTORY -> XLiveSavedGrid(
                        state = saved,
                        gridState = savedGridState,
                        actions = savedActions,
                        emptyText = "Здесь будут комнаты, которые вы открывали",
                        listName = "истории",
                    )
                }
                XLiveTab.SEARCH -> XLivePlaceholder(text = "${bar.tab.title} — следующим шагом")
            }
        }
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun ScreenXLivePreview() {
    ScreenXLiveContent(
        bar = XLiveBarState(),
        barActions = XLiveBarActions(),
        feed = XLiveFeedState(isLoading = true),
        gridState = rememberLazyGridState(),
        feedActions = XLiveFeedActions(),
        tags = XLiveTagsState(),
        tagsListState = rememberLazyListState(),
        tagsActions = XLiveTagsActions(),
        saved = XLiveSavedState(),
        savedGridState = rememberLazyGridState(),
        savedActions = XLiveSavedActions(),
    )
}
