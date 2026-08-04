package com.client.xvideos.x.screens.profile

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

class ScreenProfileSM @Inject constructor(

    val saved: SavedX,
) : ScreenModel {
    val favorites = saved.favorites.list
    fun addFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.add(item) }
    fun removeFavorite(item: ItemsX) = screenModelScope.launch { saved.favorites.remove(item) }
}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleProfile {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenProfileSM::class)
    abstract fun bindScreenProfileScreenModel(hiltListScreenModel: ScreenProfileSM): ScreenModel
}