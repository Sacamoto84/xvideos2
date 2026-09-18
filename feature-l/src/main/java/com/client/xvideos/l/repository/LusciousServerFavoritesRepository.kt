package com.client.xvideos.l.repository

import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails

/**
 * Контракт для работы с серверными подписками пользователя и лайками Luscious.
 */
interface LusciousServerFavoritesRepository {

    /**
     * Получить user_id текущей авторизованной сессии с сервера.
     */
    suspend fun getSessionUserId(): Result<String>

    /**
     * Выполнить сетевой запрос FavoritesByDatePictureSet и вернуть сырую строку ответа.
     *
     * @param userId ID пользователя. Если null, извлекается автоматически из сессии.
     * @param page Номер страницы (начиная с 1).
     */
    suspend fun getSubscribedAlbumsRaw(userId: String? = null, page: Int = 1): Result<String>

    /**
     * Получить страницу подписанных альбомов пользователя.
     * user_id извлекается автоматически из текущей авторизованной сессии.
     *
     * @param page Номер страницы (начиная с 1).
     */
    suspend fun getSubscribedAlbums(page: Int): Result<List<AlbumDetails>>

    /**
     * Выполнить сетевой запрос FavoritesByDatePicture и вернуть сырую строку ответа.
     *
     * @param userId ID пользователя. Если null, извлекается автоматически из сессии.
     * @param page Номер страницы (начиная с 1).
     */
    suspend fun getServerLikedPicturesRaw(userId: String? = null, page: Int = 1): Result<String>

    /**
     * Получить страницу лайкнутых картинок пользователя с сервера.
     *
     * @param page Номер страницы (начиная с 1).
     */
    suspend fun getServerLikedPictures(page: Int): Result<List<PicsDetails>>
}
