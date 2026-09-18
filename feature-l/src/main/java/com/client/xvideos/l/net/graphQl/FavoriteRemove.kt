package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig

class FavoriteRemove(
    val repository: Repository
) {
    /**
     * Отправляет мутацию FavoriteRemove на сервер Luscious.
     * Запрос выполняется напрямую (DIRECT), так как это изменяющая состояние мутация.
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
