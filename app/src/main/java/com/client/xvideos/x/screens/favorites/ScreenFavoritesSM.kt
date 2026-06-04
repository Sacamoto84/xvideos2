package com.client.xvideos.x.screens.favorites

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


class ScreenFavoritesSM @Inject constructor(
    val saved : SavedX
) : ScreenModel {

    val favorites = saved.favorites.list

    fun addFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.add(item) }

    fun removeFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.remove(item) }

    /** Скачать (сохранить) видео в раздел «Сохранённое». */
    fun download(item: ItemsX) = saved.downloads.download(item)

}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleFavorites {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenFavoritesSM::class)
    abstract fun bindScreenFavoritesScreenModel(hiltListScreenModel: ScreenFavoritesSM): ScreenModel
}