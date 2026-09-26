package com.client.xvideos.x.screens.favorites

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.feature.saved.SavedX
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ScreenModel экрана избранных роликов раздела X.
 *
 * Предоставляет список избранного и операции добавления, удаления и сохранения в галерею.
 *
 * @property saved Фасад локальных данных раздела X ([SavedX]).
 */
@Stable
class ScreenFavoritesSM @Inject constructor(
    val saved : SavedX
) : ScreenModel {

    /** Реактивный список элементов избранного. */
    val favorites = saved.favorites.list

    /** Добавить ролик в избранное. */
    fun addFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.add(item) }

    /** Удалить ролик из избранного. */
    fun removeFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.remove(item) }

    /** Скачать (сохранить) видео в раздел «Сохранённое». */
    fun download(item: ItemsX) = saved.downloads.download(item)

    /** Сохранить видео целиком в общую галерею (Movies/xvideos_download). */
    fun saveToGallery(item: ItemsX) = saved.downloads.saveToGallery(item)

}

/**
 * Hilt-модуль привязки [ScreenFavoritesSM].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleFavorites {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenFavoritesSM::class)
    abstract fun bindScreenFavoritesScreenModel(hiltListScreenModel: ScreenFavoritesSM): ScreenModel
}
