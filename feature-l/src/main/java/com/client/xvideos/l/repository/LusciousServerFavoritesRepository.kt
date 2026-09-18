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

    /**
     * Добавить лайк/избранное на сервер Luscious (мутация FavoriteAdd).
     *
     * @param anchorId ID объекта (картинки, альбома).
     * @param anchorType Тип объекта ("picture", "album"). По умолчанию "picture".
     * @param favoriteType Тип действия ("like", "favorite"). По умолчанию "like".
     */
    suspend fun addFavorite(
        anchorId: String,
        anchorType: String = "picture",
        favoriteType: String = "like"
    ): Result<Unit>

    /**
     * Поставить лайк картинке на сервере Luscious.
     */
    suspend fun likePicture(pictureId: String): Result<Unit> =
        addFavorite(anchorId = pictureId, anchorType = "picture", favoriteType = "like")

    /**
     * Добавить альбом в избранное/лайкнуть на сервере Luscious.
     */
    suspend fun likeAlbum(albumId: String): Result<Unit> =
        addFavorite(anchorId = albumId, anchorType = "album", favoriteType = "like")

    /**
     * Попытаться разрешить ID картинки на сервере Luscious по ID альбома и URL/имени файла медиа.
     * Используется для локально сохранённых лайков, у которых изначально не был сохранён ID.
     */
    suspend fun resolvePictureId(albumId: String, mediaUrlOrFileName: String): Result<String>

    /**
     * Удалить лайк/избранное на сервере Luscious (мутация FavoriteRemove).
     *
     * @param anchorId ID объекта (картинки, альбома).
     * @param anchorType Тип объекта ("picture", "album"). По умолчанию "picture".
     * @param favoriteType Тип действия ("like", "favorite"). По умолчанию "like".
     */
    suspend fun removeFavorite(
        anchorId: String,
        anchorType: String = "picture",
        favoriteType: String = "like"
    ): Result<Unit>

    /**
     * Удалить лайк картинки на сервере Luscious.
     */
    suspend fun unlikePicture(pictureId: String): Result<Unit> =
        removeFavorite(anchorId = pictureId, anchorType = "picture", favoriteType = "like")

    /**
     * Удалить альбом из избранного/убрать лайк на сервере Luscious.
     */
    suspend fun unlikeAlbum(albumId: String): Result<Unit> =
        removeFavorite(anchorId = albumId, anchorType = "album", favoriteType = "like")
}
