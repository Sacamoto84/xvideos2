package com.client.xvideos.r.ui.fullscreen

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.ScreenTransition
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.fullscreen.molecule.RedFullScreenFeed
import com.client.xvideos.r.ui.fullscreen.molecule.RedFullScreenSingle
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.RFeedSessionStore

@OptIn(ExperimentalVoyagerApi::class)
class ScreenRedFullScreen(
    val item: GifsInfo,
    private val feedKey: String? = null,
    private val startIndex: Int = 0
) : Screen, ScreenTransition {

    override val key: ScreenKey = "RedFullScreen:${item.id}:$startIndex"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenRedFullScreenSM>()
        val host = remember(feedKey) { feedKey?.let { RFeedSessionStore.get(it) } }

        ScreenRedFullScreenContent(
            host = host,
            startIndex = startIndex,
            item = item,
            vm = vm,
            navigator = navigator
        )
    }

    override fun enter(lastEvent: StackEvent): EnterTransition {
        return fadeIn(tween(300))
    }

    override fun exit(lastEvent: StackEvent): ExitTransition {
        return fadeOut(tween(300))
    }
}

@Composable
fun ScreenRedFullScreenContent(
    host: LazyRow123Host?,
    startIndex: Int,
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (host != null) {
            RedFullScreenFeed(
                host = host,
                startIndex = startIndex,
                fallbackItem = item,
                vm = vm,
                navigator = navigator
            )
        } else {
            RedFullScreenSingle(
                item = item,
                vm = vm,
                navigator = navigator
            )
        }
    }
}

@Preview
@Composable
private fun ScreenRedFullScreenPreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("ScreenRedFullScreen", color = Color.White)
    }
}
