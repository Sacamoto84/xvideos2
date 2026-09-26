package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.runtime.Stable
import androidx.compose.foundation.lazy.grid.LazyGridState
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.featured.saved.SavedL
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * [ScreenModel] для экрана списка сохраненных коллекций Luscious.
 *
 * Управляет состоянием прокрутки сетки коллекций [gridState] и делегирует операции
 * переименования и удаления коллекций в [SavedL.collection].
 *
 * @property savedL Единый фасад локальных хранилищ Luscious.
 */
@Stable
class ScreenSavedCollectionSM @Inject constructor(
    val savedL: SavedL,
) : ScreenModel {

    /** Состояние скролла сетки коллекций. */
    val gridState = LazyGridState()

    /**
     * Переименование коллекции (выполняется асинхронно на пуле IO).
     *
     * @param oldName Исходное название коллекции.
     * @param newName Новое название коллекции.
     */
    fun renameCollection(oldName: String, newName: String) {
        savedL.collection.renameCollection(oldName, newName)
    }

    /**
     * Удаление коллекции (выполняется асинхронно на пуле IO).
     *
     * @param name Название удаляемой коллекции.
     */
    fun deleteCollection(name: String) {
        savedL.collection.deleteCollection(name)
    }
}

/**
 * Hilt-модуль мультибиндинга для [ScreenSavedCollectionSM].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedCollection {

    /** Регистрирует [ScreenSavedCollectionSM] в карте ScreenModel Voyager. */
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedCollectionSM::class)
    abstract fun bindScreenLSavedCollectionScreenModel(hiltListScreenModel: ScreenSavedCollectionSM): ScreenModel
}
