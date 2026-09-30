package com.client.xvideos.screenRoot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.common.navigation.NavigationDepthState
import com.client.xvideos.l.featured.saved.SavedL
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * CompositionLocal для доступа к корневой ScreenModel из дочерних composable.
 *
 * Через неё экраны могут показать или скрыть общий overlay, не прокидывая
 * `ScreenRootSM` через длинную цепочку параметров.
 */
val LocalRootScreenModel = staticCompositionLocalOf<ScreenRootSM> { error("No ScreenRootSM provided") }

/**
 * ScreenModel корневого экрана.
 *
 * Хранит общий overlay как composable-лямбду. Это позволяет временно показать
 * поверх всего приложения диалог, полноэкранный слой или другой UI, не создавая
 * отдельный route в навигации.
 */
@Stable
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
