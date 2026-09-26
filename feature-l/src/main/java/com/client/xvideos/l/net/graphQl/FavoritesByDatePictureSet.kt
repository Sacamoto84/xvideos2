package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Выполняет сетевой GraphQL-запрос FavoritesByDatePictureSet для получения избранных/лайкнутых наборов картинок (альбомов) пользователя.
 *
 * @param repository Репозиторий сетевых запросов.
 * @param userId Идентификатор пользователя (если null — текущий авторизованный профиль).
 * @param page Номер страницы выдачи (начиная с 1).
 * @param showLikes Показывать ли лайкнутые наборы.
 * @return [Result] с JSON ответом сервера.
 */
suspend fun FavoritesByDatePictureSet(
    repository: Repository,
    userId: String? = null,
    page: Int = 1,
    showLikes: Boolean = true
): Result<String> {
    try {
        Timber.d("FavoritesByDatePictureSet init userId:$userId page:$page showLikes:$showLikes")
        val query = getFavoritesByDatePictureSet(userId = userId, page = page, showLikes = showLikes)
        val res = repository.openURI(query)
        res.onSuccess { raw ->
            Timber.d("FavoritesByDatePictureSet response (${raw.length} chars): %s", raw.take(500))
        }.onFailure { err ->
            Timber.w(err, "FavoritesByDatePictureSet request failed")
        }
        return res
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "FavoritesByDatePictureSet failed")
        return Result.failure(e)
    }
}
