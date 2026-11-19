package com.client.xvideos.n.saved

import androidx.compose.runtime.Stable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единая точка доступа к локальным хранилищам раздела N (манга):
 * - [favorites] — сохранённые произведения целиком (альбомы);
 * - [likes] — сохранённые понравившиеся страницы/картинки;
 * - [collection] — пользовательские коллекции страниц/картинок.
 */
@Stable
@Singleton
class NSaved @Inject constructor(
    val favorites: NFavoritesRepository,
    val likes: NLikesRepository,
    val collection: NCollectionRepository
) {
    suspend fun refreshAll() {
        favorites.refresh()
        likes.refresh()
        collection.refresh()
    }
}
