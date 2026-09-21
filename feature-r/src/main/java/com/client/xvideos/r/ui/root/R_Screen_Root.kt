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
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import com.client.xvideos.common.collectionDB.ui.DaialogNewCollection
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.ui.explorer.ScreenRedExplorer
import com.client.xvideos.r.ui.block.DialogBlock
import com.client.xvideos.common.ui.atom.DownloadIndicator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

import androidx.compose.runtime.CompositionLocalProvider
import com.client.xvideos.r.ui.explorer.LocalRNavigationState
import com.client.xvideos.r.ui.explorer.RNavigationState

class R_Screen_Root : Screen {

    override val key: ScreenKey = "R_Screen_Root"

    @OptIn(ExperimentalVoyagerApi::class)
    @Composable
    override fun Content() {

        val vm: ScreenRedRootSM = getScreenModel()

        CompositionLocalProvider(LocalRNavigationState provides vm.navigationState) {
            val savedRed = vm.savedRed

            val percentDownload = vm.downloadRed.downloader.percent.collectAsStateWithLifecycle().value

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

            //Диалог коллекции
            if (savedRed.collections.visibleDialog) { R_DialogCollection(savedRed = {savedRed}) }

            if (savedRed.collections.visibleDialogCreateNew) {

                DaialogNewCollection(
                    visible = savedRed.collections.visibleDialogCreateNew,
                    onDismiss = { savedRed.collections.visibleDialogCreateNew = false },
                    onBlockConfirmed = { collection ->
                        if (collection.isNotEmpty()) {
                            savedRed.collections.createCollection(collection)
                            savedRed.collections.visibleDialogCreateNew = false
                        }
                    }
                )

            }


            //Диалог для блокировки
            if (vm.block.blockVisibleDialog) { R_DialogBlock(block = {vm.block}) }




            // Раньше здесь заново публиковался LocalRootScreenModel — корневая
            // ScreenModel приложения. Внутри R её никто не читал, а раздел из-за
            // неё знал про точку сборки. ScreenRoot публикует её сам, выше по
            // дереву; глубину навигации разделы берут из Hilt-графа.
            Scaffold(
                modifier = Modifier.imePadding(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = { DownloadIndicator(percentDownload) }) { padding ->
                Box(modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
                    Navigator(ScreenRedExplorer()) { navigator ->
                        //SlideTransition(navigator)


                        ScreenTransition(
                            navigator = navigator,
                            transition = {
                                val (initialOffset, targetOffset) = when (navigator.lastEvent) {
                                    StackEvent.Pop -> ({ size: Int -> -size }) to ({ size: Int -> size })
                                    else -> ({ size: Int -> size }) to ({ size: Int -> -size })
                                }
                                slideInHorizontally(tween(200), initialOffset) togetherWith  slideOutHorizontally(tween(200), targetOffset)
                            }
                        )


                    }
                }
            }
        }
    }
}

@Composable
private fun R_DialogBlock(block:  () -> BlockRed){

    DialogBlock(
        visible = block().blockVisibleDialog,
        onDismiss = { block().blockVisibleDialog = false },
        onBlockConfirmed = {
            block().blockItem?.let { item ->
                block().blockItem(item)
                block().blockItem = null
            }
        }
    )

}


@Composable
private fun R_DialogCollection(savedRed: () -> SavedRed){

    val haptic = LocalHapticFeedback.current

    DialogCollection(
        visible = savedRed().collections.visibleDialog,
        onDismiss = { savedRed().collections.visibleDialog = false },
        onClickNewCollection = {
            //Отобразить диалог создания новой коллекции
            savedRed().collections.visibleDialogCreateNew = true
        },
        onSelectCollection = { collection ->
            // Было `!!` без проверки: если диалог открыт, а выбранный элемент
            // успели сбросить, это краш прямо по нажатию на коллекцию.
            savedRed().collections.collectionItemGifInfo?.let { item ->
                savedRed().collections.addCollection(item, collection)
            }
            savedRed().collections.visibleDialog = false
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        },
        savedRed = savedRed
    )

}

@Stable
class ScreenRedRootSM @Inject constructor(
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val block: BlockRed,
    val navigationState: RNavigationState
) : ScreenModel

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedRootBlock {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedRootSM::class)
    abstract fun bindScreenRedRootScreenModel(hiltListScreenModel: ScreenRedRootSM): ScreenModel
}

internal enum class RedRootDialogBackAction {
    DISMISS_NEW_COLLECTION,
    DISMISS_COLLECTION_PICKER,
    DISMISS_BLOCK,
    NONE
}

internal fun resolveRedRootDialogBackAction(
    visibleDialogCreateNew: Boolean,
    visibleDialog: Boolean,
    blockVisibleDialog: Boolean
): RedRootDialogBackAction = when {
    visibleDialogCreateNew -> RedRootDialogBackAction.DISMISS_NEW_COLLECTION
    visibleDialog -> RedRootDialogBackAction.DISMISS_COLLECTION_PICKER
    blockVisibleDialog -> RedRootDialogBackAction.DISMISS_BLOCK
    else -> RedRootDialogBackAction.NONE
}
