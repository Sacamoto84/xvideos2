package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig

/**
 * Исполнитель GraphQL-мутации FavoriteRemove для отзыва лайка или удаления из избранного на сервере Luscious.
 *
 * @property repository Репозиторий сетевых запросов.
 */
class FavoriteRemove(
    val repository: Repository
) {
    /**
     * Отправляет мутацию FavoriteRemove на сервер Luscious.
     * Запрос выполняется напрямую (DIRECT), так как это изменяющая состояние мутация.
     *
     * @param anchorId Уникальный ID объекта (картинки, альбома).
     * @param anchorType Тип объекта (`"picture"`, `"album"`).
     * @param favoriteType Тип действия (`"like"`, `"favorite"`).
     * @return [Result] с ответом сервера.
     */
    suspend fun removeFavorite(
        anchorId: String,
        anchorType: String = "picture",
        favoriteType: String = "like"
    ): Result<String> {
        val query = getFavoriteRemove(
            anchorId = anchorId,
            anchorType = anchorType,
            favoriteType = favoriteType
        )
        val res = repository.openURI(query, config = RepositoryUriConfig.DIRECT)
        return res
    }
}
