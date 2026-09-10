package com.client.xvideos.l.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance

// Расширение для удобного использования в ViewModels
inline fun <reified T : RepositoryResult> Flow<RepositoryResult>.filterResult(): Flow<T> {
    return filterIsInstance<T>()
}

// Команды для репозитория
sealed class RepositoryAction {

    /**
     * Загрузить альбом
     */
    data class LoadAlbum(val id: Long) : RepositoryAction()


    object Login : RepositoryAction()

//    data class SearchAlbums(val query: String, val page: Int = 1) : RepositoryAction()
//    data class SaveAlbum(val albumId: String) : RepositoryAction()
//    data class UnsaveAlbum(val albumId: String) : RepositoryAction()
//    data class LoadAlbumDetails(val albumId: String) : RepositoryAction()
    //object LoadSavedAlbums : RepositoryAction()

    // Pictures actions
    //data class LoadPictures(val albumId: String, val page: Int = 1) : RepositoryAction()

    // Settings actions
//    data class UpdateSettings(val key: String, val value: String) : RepositoryAction()
//    data class GetSettings(val key: String) : RepositoryAction()

    // Cache actions
    //object ClearCache : RepositoryAction()
    //data class ClearAlbumCache(val albumId: String) : RepositoryAction()
}
