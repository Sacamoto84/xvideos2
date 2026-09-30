package com.client.xvideos.screenRoot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.navigation.LocalMainNavigator
import com.client.xvideos.common.snackbar.show
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.traficStatistic.AppNetworkSpeedMonitorLite
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.explorer.LCollectionDialogs
import kotlinx.coroutines.flow.filterIsInstance
import net.engawapg.lib.zoomable.ExperimentalZoomableApi

/**
 * Корневой экран приложения.
 *
 * Собирает общий каркас UI: Voyager navigation stack, snackbar host,
 * диалоги коллекций L-раздела, overlay-слой и мини-монитор скорости сети.
 */
object ScreenRoot : Screen {

    private fun readResolve(): Any = ScreenRoot

    override val key: ScreenKey = "ScreenRoot"

    /**
     * Строит корневой Compose UI и связывает глобальные обработчики событий.
     *
     * Здесь создаётся `Navigator`, подписка на `Event.ShowSnackBar`,
     * публикация `CompositionLocal` и вывод overlay-контента поверх текущего
     * экрана без разрушения навигационного стека.
     */
    @OptIn(ExperimentalZoomableApi::class)
    @Composable
    override fun Content() {
        val haptic = LocalHapticFeedback.current
        val vm: ScreenRootSM = getScreenModel()
        val snackBarHostState = remember { SnackbarHostState() }

        var mainNavigator by remember { mutableStateOf<Navigator?>(null) }

        LaunchedEffect(Unit) {
            EventBus.events
                .filterIsInstance<Event.ShowSnackBar>()
                .collect { event ->
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    snackBarHostState.show(event.message)
                }
        }

        CompositionLocalProvider(
            LocalRootScreenModel provides vm,
            LocalMainNavigator provides mainNavigator
        ) {
            ScreenRootContent(
                savedL = vm.savedL,
                overlayContent = vm.overlayContent.value,
                snackBarHostState = snackBarHostState,
                onMainNavigatorChange = { nav ->
                    mainNavigator = nav
                }
            )
        }
    }
}

@Composable
fun ScreenRootContent(
    savedL: SavedL,
    overlayContent: (@Composable () -> Unit)?,
    snackBarHostState: SnackbarHostState,
    onMainNavigatorChange: (Navigator?) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButtonPosition = FabPosition.Start,
        containerColor = Theme.backgroundAppRoot,
        snackbarHost = {
            RootSnackbarHost(snackBarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Navigator(screen = MenuScreen) { nav ->
                DisposableEffect(nav) {
                    onMainNavigatorChange(nav)
                    onDispose {
                        onMainNavigatorChange(null)
                    }
                }
                nav.lastItem.Content()
            }

            LCollectionDialogs(savedL)

            overlayContent?.let { content ->
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
    AppNetworkSpeedMonitorLite()
}
