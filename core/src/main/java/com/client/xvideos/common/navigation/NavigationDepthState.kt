package com.client.xvideos.common.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.navigator.Navigator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton-хранилище логической «глубины» навигации.
 *
 * Раньше это был глобальный `var depth` верхнего уровня. Чтобы убрать неявное
 * глобальное состояние и иметь возможность тестировать/инжектить его, теперь
 * значение живёт в Hilt-singleton.
 *
 * Используется корневым экраном для решения, нужно ли показывать кнопку
 * быстрого возврата домой. Меняется вложенными экранами разделов при заходе и
 * `L_ScreenExplorer` при выходе.
 *
 * Значение участвует в Compose-снапшоте (mutableIntStateOf), поэтому
 * наблюдатели автоматически перерисовываются.
 */
@Singleton
class NavigationDepthState @Inject constructor() {
    var depth by mutableIntStateOf(0)
}

/** Доступ к Hilt-синглтонам из composable вне DI-графа. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NavigationDepthEntryPoint {
    fun navigationDepthState(): NavigationDepthState
}

/**
 * Навигатор корневого `Navigator`, если экран показан внутри него.
 *
 * Вложенные разделы пушат полноэкранные экраны именно в него, чтобы те легли
 * поверх всего, а не внутрь таба.
 */
val LocalMainNavigator = staticCompositionLocalOf<Navigator?> { null }
