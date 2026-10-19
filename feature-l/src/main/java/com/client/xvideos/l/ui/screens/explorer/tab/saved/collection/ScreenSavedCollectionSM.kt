package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.runtime.Stable
import androidx.compose.foundation.lazy.grid.LazyGridState
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.model.LCollectionViewState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

/**
 * [ScreenModel] для экрана списка сохраненных коллекций L.
 *
 * Управляет состоянием прокрутки сетки коллекций [gridState] и делегирует операции
 * переименования и удаления коллекций в [SavedL.collection].
 *
 * @property savedL Единый фасад локальных хранилищ L.
 */
@Stable
class ScreenSavedCollectionSM @Inject constructor(
    val savedL: SavedL,
) : ScreenModel {

    /** Состояние скролла сетки коллекций. */
    val gridState = LazyGridState()

    /**
     * Открытая коллекция. Живёт здесь, а не в композиции: полноэкранный просмотр
     * открывается в корневом навигаторе, и проводник L уходит из композиции.
     */
    private var openedView: LCollectionViewState? = null

    /** Состояние коллекции [name]: прежнее, пока открыта она же, иначе новое. */
    fun collectionView(name: String): LCollectionViewState =
        openedView?.takeIf { it.collectionName == name }
            ?: LCollectionViewState(name).also { openedView = it }

    /** Открывает коллекцию из списка — с начала и без прежнего отбора. */
    fun openCollection(name: String) {
        openedView = null
        savedL.collection.setCollection(name)
    }

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
