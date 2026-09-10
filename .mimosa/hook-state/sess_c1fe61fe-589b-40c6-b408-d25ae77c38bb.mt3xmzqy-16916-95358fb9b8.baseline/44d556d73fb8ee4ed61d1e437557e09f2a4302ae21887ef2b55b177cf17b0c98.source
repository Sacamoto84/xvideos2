package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

class ScreenSavedCollectionSM @Inject constructor(
    val savedL: SavedL,
) : ScreenModel {

    val gridState = LazyGridState()

    /**
     * Хост держит полный список PicsDetails коллекции. Раньше здесь копилась
     * запись на каждую открытую за сессию коллекцию и ни одна не вытеснялась.
     * Теперь это LRU: помним состояние нескольких последних, остальные
     * пересоздаются при следующем открытии.
     */
    private val collectionHosts =
        object : LinkedHashMap<String, LazyRowPictureDetailsHost>(MAX_CACHED_HOSTS, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, LazyRowPictureDetailsHost>?
            ): Boolean = size > MAX_CACHED_HOSTS
        }

    fun hostFor(collectionName: String): LazyRowPictureDetailsHost {
        return collectionHosts.getOrPut(collectionName) {
            LazyRowPictureDetailsHost(collectionName)
        }
    }

    private companion object {
        const val MAX_CACHED_HOSTS = 3
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedCollection {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedCollectionSM::class)
    abstract fun bindScreenLSavedCollectionScreenModel(hiltListScreenModel: ScreenSavedCollectionSM): ScreenModel
}

