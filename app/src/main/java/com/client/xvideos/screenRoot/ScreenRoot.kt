package com.client.xvideos.screenRoot

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
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
 * CompositionLocal с главным Voyager-навигатором.
 *
 * Нужен дочерним экранам, которым требуется управлять корневым стеком навигации:
 * заменить текущий раздел, вернуться домой или проверить текущий экран.
 */
val LocalMainNavigator = staticCompositionLocalOf<Navigator?> { null }

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

                modifier = Modifier
                ,
                floatingActionButtonPosition = FabPosition.Start,
                floatingActionButton = {
                    //HomeFloatingActionButton(mainNavigator)
                },
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
 * Плавающая кнопка возврата к L-разделу.
 *
 * Следит за размером основного navigation stack через `snapshotFlow` и показывает
 * badge с глубиной, если пользователь ушёл дальше первого экрана.
 */
@Composable
private fun HomeFloatingActionButton(mainNavigator: Navigator?) {
    var navigationDepth by remember { mutableIntStateOf(0) }
    val rootVm = LocalRootScreenModel.current

    LaunchedEffect(mainNavigator) {
        mainNavigator?.let { nav ->
            snapshotFlow { nav.items.size }
                .collect { stackSize ->
                    navigationDepth = if (stackSize > 1) stackSize - 1 else 0
                }
        }
    }

    if (navigationDepth > 0 || rootVm.depthState.depth > 0) {
        Box {
            SmallFloatingActionButton(
                onClick = {
                    mainNavigator?.let { nav ->
                        if (nav.lastItem !is MenuScreen) {
                            nav.replaceAll(MenuScreen)
                        }
                    }
                },
                content = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") }
            )

            if (navigationDepth > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp),
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Text(
                        text = navigationDepth.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
