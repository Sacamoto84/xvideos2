package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

private val WHITESPACE_REGEX = Regex("\\s+")

fun PicsDetails.matchesCollectionSearch(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true

    val haystack = buildList {
        add(album.orEmpty())
        add(url_to_original.orEmpty())
        add(url_to_video.orEmpty())
        thumbnails?.forEach { add(it.url.orEmpty()) }
    }.joinToString(" ").lowercase()

    return normalized
        .lowercase()
        .split(WHITESPACE_REGEX)
        .all { it in haystack }
}

@Stable
class ScreenLCollectionNameSM @AssistedInject constructor(
    @Assisted val collectionName: String,
    val savedL: SavedL,
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(collectionName: String): ScreenLCollectionNameSM
    }

    val host = LazyRowPictureDetailsHost(collectionName)

    init {
        ensureCollectionLoaded()
    }

    fun ensureCollectionLoaded() {
        savedL.collection.getCollectionItems(collectionName)
    }

    fun delete(item: PicsDetails) {
        savedL.collection.remove(item, collectionName)
    }

}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedCollectionName {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenLCollectionNameSM.Factory::class)
    abstract fun bindScreenLSavedCollectionNameScreenModel(
        hiltDetailsScreenModelFactory: ScreenLCollectionNameSM.Factory
    ): ScreenModelFactory
}
