package com.client.xvideos.x.feature.category

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.model.xCategoryOf

/**
 * Выбранная категория X. Лежит в настройках и переживает перезапуск; пока
 * настройки не открыты (юнит-тесты, превью) — только в памяти.
 */
@Stable
object XCategoryState {

    var current: XCategory by mutableStateOf(savedCategory())
        private set

    fun select(category: XCategory) {
        current = category
        if (Settings.isInitialized) Settings.x_category.setValue(category.name)
    }

    private fun savedCategory(): XCategory =
        if (Settings.isInitialized) xCategoryOf(Settings.x_category.field.value) else XCategory.STRAIGHT
}
