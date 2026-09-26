package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Выполняет сетевой GraphQL-запрос FavoritesByDatePicture для получения лайкнутых картинок пользователя с пагинацией.
 *
 * @param repository Репозиторий сетевых запросов.
 * @param userId Идентификатор пользователя (если null — текущий авторизованный пользователь).
 * @param page Номер страницы (начиная с 1).
 * @param showLikes Показывать ли лайкнутые медиа.
 * @return [Result] с JSON ответом сервера.
 */
suspend fun FavoritesByDatePicture(
    repository: Repository,
    userId: String? = null,
    page: Int = 1,
    showLikes: Boolean = true
): Result<String> {
    try {
        Timber.d("FavoritesByDatePicture init userId:$userId page:$page showLikes:$showLikes")
        val query = getFavoritesByDatePicture(userId = userId, page = page, showLikes = showLikes)
        val res = repository.openURI(query)
        res.onSuccess { raw ->
            Timber.d("FavoritesByDatePicture response (${raw.length} chars): %s", raw.take(500))
        }.onFailure { err ->
            Timber.w(err, "FavoritesByDatePicture request failed")
        }
        return res
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "FavoritesByDatePicture failed")
        return Result.failure(e)
    }
}
