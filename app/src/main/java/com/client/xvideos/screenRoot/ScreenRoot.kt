package com.client.xvideos.screenRoot

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.navigation.LocalMainNavigator
import com.client.xvideos.common.navigation.NavigationDepthState
import com.client.xvideos.common.snackbar.show
import com.client.xvideos.common.traficStatistic.AppNetworkSpeedMonitorLite
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.explorer.LCollectionDialogs
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.filterIsInstance
import net.engawapg.lib.zoomable.ExperimentalZoomableApi
import javax.inject.Inject

/**
 * CompositionLocal для доступа к корневой ScreenModel из дочерних composable.
 *
 * Через неё экраны могут показать или скрыть общий overlay, не прокидывая
 * `ScreenRootSM` через длинную цепочку параметров.
 */
val LocalRootScreenModel = staticCompositionLocalOf<ScreenRootSM> { error("No ScreenRootSM provided") }

/**
 * Корневой экран приложения.
 *
 * Собирает общий каркас UI: Voyager navigation stack, snackbar host,
 * индикатор загрузок L-раздела, кнопку перехода домой, overlay-слой
 * и мини-монитор скорости сети.
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
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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
            Scaffold(

                modifier = Modifier.systemBarsPadding()
                ,
                floatingActionButtonPosition = FabPosition.Start,
                containerColor = Theme.backgroundAppRoot,
                snackbarHost = {
                    RootSnackbarHost(snackBarHostState)
                }
            ) { paddingValues ->

                Navigator(screen = MenuScreen) { nav ->
                    mainNavigator = nav
                    nav.lastItem.Content()
                }

                LCollectionDialogs(vm.savedL)

                vm.overlayContent.value?.let { content ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
            AppNetworkSpeedMonitorLite()
        }
    }
}


/**
 * ScreenModel корневого экрана.
 *
 * Хранит общий overlay как composable-лямбду. Это позволяет временно показать
 * поверх всего приложения диалог, полноэкранный слой или другой UI, не создавая
 * отдельный route в навигации.
 */
class ScreenRootSM @Inject constructor(
    val depthState: NavigationDepthState,
    val savedL: SavedL
) : ScreenModel {
    private val _overlayContent = mutableStateOf<(@Composable () -> Unit)?>(null)
    val overlayContent: State<(@Composable () -> Unit)?> = _overlayContent

    /**
     * Показывает overlay поверх текущего экрана.
     *
     * Переданная composable-функция будет отрисована внутри полноэкранного `Box`
     * в `ScreenRoot.Content()`.
     */
    fun showOverlay(content: @Composable () -> Unit) {
        _overlayContent.value = content
    }

    /**
     * Убирает текущий overlay и возвращает пользователю обычный экран.
     */
    fun hideOverlay() {
        _overlayContent.value = null
    }
}

/**
 * Hilt-модуль, регистрирующий корневые ScreenModel в multibinding Voyager.
 *
 * Благодаря этому биндингу `getScreenModel()` может создавать `ScreenRootSM`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenRootModule {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRootSM::class)
    abstract fun bindScreenRootSM(sm: ScreenRootSM): ScreenModel
}
