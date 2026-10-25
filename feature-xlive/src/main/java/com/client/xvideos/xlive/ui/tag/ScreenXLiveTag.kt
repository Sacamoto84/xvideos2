package com.client.xvideos.xlive.ui.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.ui.XLiveColors
import com.client.xvideos.xlive.ui.feed.XLiveFeedActions
import com.client.xvideos.xlive.ui.feed.XLiveFeedState
import com.client.xvideos.xlive.ui.feed.molecule.XLiveRoomsGrid
import com.client.xvideos.xlive.ui.room.ScreenXLiveRoom
import com.client.xvideos.xlive.ui.tag.molecule.XLiveTagTopBar

/**
 * Комнаты с тегом [tag]: шапка с тегом и та же сетка, что во «Эфирах».
 *
 * @param gender пол, под которым тег открыли; из комнаты — все.
 */
class ScreenXLiveTag(
    private val tag: String,
    private val gender: XLiveGender = XLiveGender.ALL,
) : Screen {

    override val key: ScreenKey = "ScreenXLiveTag_${gender.code}_$tag"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = rememberScreenModel { XLiveTagSM(tag, gender) }
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

        ScreenXLiveTagContent(
            tag = tag,
            gender = gender,
            feed = feed,
            gridState = vm.gridPosition.rememberState(),
            actions = actions,
            onBack = { navigator.pop() },
        )
    }
}

@Composable
fun ScreenXLiveTagContent(
    tag: String,
    gender: XLiveGender,
    feed: XLiveFeedState,
    gridState: LazyGridState,
    actions: XLiveFeedActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XLiveColors.Background)
    ) {
        XLiveTagTopBar(
            tag = tag,
            subtitle = if (gender == XLiveGender.ALL) "" else gender.title,
            onBack = onBack,
        )
        XLiveRoomsGrid(feed = feed, gridState = gridState, actions = actions)
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun ScreenXLiveTagPreview() {
    ScreenXLiveTagContent(
        tag = "petite",
        gender = XLiveGender.FEMALE,
        feed = XLiveFeedState(isLoading = true),
        gridState = rememberLazyGridState(),
        actions = XLiveFeedActions(),
        onBack = {},
    )
}
