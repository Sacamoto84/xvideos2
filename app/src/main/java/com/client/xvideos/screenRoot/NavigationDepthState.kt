package com.client.xvideos.screenRoot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
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
 * быстрого возврата домой. Меняется вложенными экранами (`ScreenAlbum`,
 * `ScreenLAlbumLandingTagSM`) при заходе и `L_ScreenExplorer` при выходе.
 *
 * Значение участвует в Compose-снапшоте (mutableIntStateOf), поэтому
 * наблюдатели автоматически перерисовываются.
 */
@Singleton
class NavigationDepthState @Inject constructor() {
    var depth by mutableIntStateOf(0)
}
