package com.client.xvideos.l.ui.screens.explorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Синглтон состояния навигации по вкладкам внутри модуля Luscious.
 *
 * Сохраняет текущую активную вкладку верхнего уровня [rootTab] и подвкладку раздела "Сохраненное" [savedTab].
 */
@Singleton
class LNavigationState @Inject constructor() {
    /** Индекс текущей активной вкладки верхнего уровня (Top Hits, Search, Saved). */
    var rootTab by mutableIntStateOf(0)
    /** Индекс текущей активной вкладки внутри сохраненного контента (Коллекции, Лайки, Подписки). */
    var savedTab by mutableIntStateOf(0)
}
