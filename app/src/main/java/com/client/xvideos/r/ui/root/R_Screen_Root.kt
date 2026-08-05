package com.client.xvideos.r.ui.root

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
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
import com.client.xvideos.screenRoot.LocalRootScreenModel
import com.client.xvideos.screenRoot.ScreenRootSM
import com.client.xvideos.r.common.block.ui.DialogBlock
import com.client.xvideos.common.ui.atom.DownloadIndicator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import timber.log.Timber
import javax.inject.Inject

class R_Screen_Root : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalVoyagerApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val vm: ScreenRedRootSM = getScreenModel()

        val savedRed = vm.savedRed

        val percentDownload = vm.downloadRed.downloader.percent.collectAsStateWithLifecycle().value

        BackHandler { Timber.i("iii BackHandler Root") }

        //Диалог коллекции
        if (savedRed.collections.visibleDialog) { R_DialogCollection(savedRed = {savedRed}) }

        if (savedRed.collections.visibleDialogCreateNew) {

                DaialogNewCollection(
                    visible = savedRed.collections.visibleDialogCreateNew,
                    onDismiss = { savedRed.collections.visibleDialogCreateNew = false },
                    onBlockConfirmed = { collection ->
                        if ((collection != "")) {
                            savedRed.collections.createCollection(collection)
                            savedRed.collections.visibleDialogCreateNew = false
                        }
                    }
                )

        }


        //Диалог для блокировки
        if (vm.block.blockVisibleDialog) { R_DialogBlock(block = {vm.block}) }




        CompositionLocalProvider(LocalRootScreenModel provides getScreenModel<ScreenRootSM>()) {
            Scaffold(
                modifier = Modifier.imePadding(),
                bottomBar = { DownloadIndicator(percentDownload) }) {
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

class ScreenRedRootSM @Inject constructor(
    val savedRed: SavedRed,
    val downloadRed: DownloadRed,
    val block: BlockRed
) : ScreenModel

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedRootBlock {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedRootSM::class)
    abstract fun bindScreenRedRootScreenModel(hiltListScreenModel: ScreenRedRootSM): ScreenModel
}
