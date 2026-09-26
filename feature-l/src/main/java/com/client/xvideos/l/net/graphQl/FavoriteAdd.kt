package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Выполняет сетевой GraphQL-запрос FavoriteAdd для добавления лайка на сервер Luscious.
 *
 * Мутация выполняется с [RepositoryUriConfig.DIRECT] в обход дискового/RAM кэша.
 *
 * @param repository Репозиторий сетевых запросов.
 * @param anchorId Уникальный ID целевого объекта (картинки, альбома).
 * @param anchorType Тип целевого объекта (`"picture"`, `"album"`).
 * @param favoriteType Тип реакции (`"like"`, `"favorite"`).
 * @return [Result] с сырым ответом сервера.
 */
suspend fun FavoriteAdd(
    repository: Repository,
    anchorId: String,
    anchorType: String = "picture",
    favoriteType: String = "like"
): Result<String> {
    try {
        Timber.d("FavoriteAdd init anchorId:$anchorId anchorType:$anchorType favoriteType:$favoriteType")
        val query = getFavoriteAdd(
            anchorId = anchorId,
            anchorType = anchorType,
            favoriteType = favoriteType
        )
        // Мутации выполняются с RepositoryUriConfig.DIRECT (без ROM/RAM кеширования)
        val res = repository.openURI(query, config = RepositoryUriConfig.DIRECT)
        res.onSuccess { raw ->
            Timber.d("FavoriteAdd response (%d chars): %s", raw.length, raw.take(500))
        }.onFailure { err ->
            Timber.w(err, "FavoriteAdd request failed")
        }
        return res
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "FavoriteAdd failed")
        return Result.failure(e)
    }
}
