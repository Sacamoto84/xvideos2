package com.client.xvideos.r.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import com.client.xvideos.common.ui.atom.DownloadIndicator
import com.client.xvideos.r.ui.explorer.LocalRNavigationState
import com.client.xvideos.r.ui.explorer.RNavigationState
import com.client.xvideos.r.ui.explorer.ScreenRedExplorer
import com.client.xvideos.r.ui.root.molecule.RedRootDialogs

class R_Screen_Root : Screen {

    override val key: ScreenKey = "R_Screen_Root"

    @OptIn(ExperimentalVoyagerApi::class)
    @Composable
    override fun Content() {
        val vm: ScreenRedRootSM = getScreenModel()
        val savedRed = vm.savedRed
        val percentDownload by vm.downloadRed.downloader.percent.collectAsStateWithLifecycle()

        val isAnyDialogOpen = savedRed.collections.visibleDialog ||
            savedRed.collections.visibleDialogCreateNew ||
            vm.block.blockVisibleDialog

        BackHandler(enabled = isAnyDialogOpen) {
            when (resolveRedRootDialogBackAction(
                visibleDialogCreateNew = savedRed.collections.visibleDialogCreateNew,
                visibleDialog = savedRed.collections.visibleDialog,
                blockVisibleDialog = vm.block.blockVisibleDialog
            )) {
                RedRootDialogBackAction.DISMISS_NEW_COLLECTION -> savedRed.collections.visibleDialogCreateNew = false
                RedRootDialogBackAction.DISMISS_COLLECTION_PICKER -> savedRed.collections.visibleDialog = false
                RedRootDialogBackAction.DISMISS_BLOCK -> vm.block.blockVisibleDialog = false
                RedRootDialogBackAction.NONE -> Unit
            }
        }

        R_Screen_RootContent(
            navigationState = vm.navigationState,
            percentDownload = percentDownload,
            dialogsContent = {
                RedRootDialogs(
                    savedRed = { savedRed },
                    block = { vm.block }
                )
            }
        )
    }
}

@Composable
fun R_Screen_RootContent(
    navigationState: RNavigationState,
    percentDownload: Float,
    dialogsContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalRNavigationState provides navigationState) {
        dialogsContent()

        Scaffold(
            modifier = modifier.imePadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { DownloadIndicator(percentDownload) }
        ) { padding ->
            Box(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
                Navigator(ScreenRedExplorer()) { navigator ->
                    ScreenTransition(
                        navigator = navigator,
                        transition = {
                            val (initialOffset, targetOffset) = when (navigator.lastEvent) {
                                StackEvent.Pop -> ({ size: Int -> -size }) to ({ size: Int -> size })
                                else -> ({ size: Int -> size }) to ({ size: Int -> -size })
                            }
                            slideInHorizontally(tween(200), initialOffset) togetherWith slideOutHorizontally(tween(200), targetOffset)
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun R_Screen_RootPreview() {
    R_Screen_RootContent(
        navigationState = RNavigationState(),
        percentDownload = 0f
    )
}
