package com.client.xvideos.xlive.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import com.client.xvideos.xlive.model.XLiveCollection
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.room.ScreenXLiveRoom
import com.client.xvideos.xlive.ui.root.atom.XLivePlaceholder
import com.client.xvideos.xlive.ui.root.molecule.XLiveBottomBar
import com.client.xvideos.xlive.ui.root.molecule.XLiveRoomsGrid

/**
 * Корень раздела XLive: нижние вкладки как у X, переключатель пола, вкладка
 * «Эфиры» с сеткой комнат. Тап по карточке открывает комнату.
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
        val feed by vm.feed.collectAsStateWithLifecycle()

        val actions = remember(vm, navigator) {
            XLiveFeedActions(
                onOpenRoom = { room -> navigator.push(ScreenXLiveRoom(room)) },
                onRetry = vm::reload,
                onRetryLoadMore = vm::retryLoadMore,
                onRefresh = vm::refresh,
                onLoadMore = vm::loadMore,
            )
        }

        ScreenXLiveContent(
            tab = tab,
            gender = gender,
            collection = collection,
            feed = feed,
            gridState = vm.gridPosition.rememberState(),
            onTab = vm::selectTab,
            onGender = vm::selectGender,
            onCollection = vm::selectCollection,
            actions = actions,
        )
    }
}

@Composable
fun ScreenXLiveContent(
    tab: XLiveTab,
    gender: XLiveGender,
    collection: XLiveCollection,
    feed: XLiveFeedState,
    gridState: LazyGridState,
    onTab: (XLiveTab) -> Unit,
    onGender: (XLiveGender) -> Unit,
    onCollection: (XLiveCollection) -> Unit,
    actions: XLiveFeedActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = XLiveColors.Background,
        bottomBar = {
            XLiveBottomBar(
                tab = tab,
                onTab = onTab,
                collection = collection,
                onCollection = onCollection,
                gender = gender,
                onGender = onGender,
            )
        },
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
            when (tab) {
                XLiveTab.ROOMS -> XLiveRoomsGrid(
                    feed = feed,
                    gridState = gridState,
                    actions = actions,
                )
                else -> XLivePlaceholder(text = "${tab.title} — следующим шагом")
            }
        }
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun ScreenXLivePreview() {
    ScreenXLiveContent(
        tab = XLiveTab.ROOMS,
        gender = XLiveGender.ALL,
        collection = XLiveCollection.POPULAR,
        feed = XLiveFeedState(isLoading = true),
        gridState = rememberLazyGridState(),
        onTab = {},
        onGender = {},
        onCollection = {},
        actions = XLiveFeedActions(),
    )
}
