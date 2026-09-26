package com.client.xvideos.l.net

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.net.graphQl.LandingPageAlbumSearch
import com.client.xvideos.l.net.graphQl.LandingPageAlbumTag
import com.client.xvideos.l.net.graphQl.refreshMediaCategories
import com.client.xvideos.l.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Главный фасад API раздела Luscious (L).
 *
 * Предоставляет методы для получения:
 * - Подробностей конкретного альбома ([getAlbum]).
 * - Списков альбомов с пагинацией и фильтрами ([getAlbumList]).
 * - Агрегаций доступных фильтров ([getAlbumListAggregations]).
 * - Топовых альбомов ([getAlbumTopHits]).
 * - Посадочных страниц поиска и тегов ([getLandingPageAlbumTag], [getLandingPageAlbumSearch]).
 *
 * @property scope CoroutineScope приложения.
 * @property repository Репозиторий сетевых запросов.
 */
class Luscious(
    val scope : CoroutineScope,
    val repository: Repository
) {

    // Адреса переехали в repository.LusciousEndpoints: оттуда их читает и
    // Repository, из-за чего два пакета одного слоя ссылались друг на друга.

    init {
        scope.launch(Dispatchers.IO) {
            refreshMediaCategories(repository)
        }
    }

    /**
     * Возвращает объект [AlbumInfo] для работы с альбомом по ID или URL ссылки.
     *
     * @param albumInput Числовой идентификатор (Int, Long) или строка URL/ID.
     * @param download Флаг предварительной загрузки (исторический параметр).
     * @param requestScope Специфичный CoroutineScope для сетевых задач альбома.
     * @return Экземпляр [AlbumInfo].
     */
    fun getAlbum(
        albumInput: Any,
        download: Boolean = false,
        requestScope: CoroutineScope = scope
    ): AlbumInfo {
        val id = when (albumInput) {
            is Int -> if (albumInput > 0) albumInput else null
            is Long -> if (albumInput in 1..Int.MAX_VALUE) albumInput.toInt() else null
            is String -> {
                val trimmed = albumInput.trim()
                if (trimmed.isEmpty()) null
                else (trimmed.toIntOrNull() ?: extractIdFromUrl(trimmed)?.toIntOrNull())?.takeIf { it > 0 }
            }
            else -> throw IllegalArgumentException("albumInput must be Int, Long or String: $albumInput")
        } ?: throw IllegalArgumentException("Invalid album ID: $albumInput")

        return AlbumInfo(id, download, repository, requestScope)
    }

    /**
     * Запрашивает статистику фильтров (агрегации) для текущей страницы и фильтра.
     */
    suspend fun getAlbumListAggregations(page: Int, filter: AlbumListFilter?): Result<getAlbumListAggregationsResult> {
        return getAlbumListAggregationsImpl(page, filter, repository)
    }

    /**
     * Запрашивает пагинированный список альбомов.
     */
    suspend fun getAlbumList(page: Int, filter: AlbumListFilter?): Result<AlbumListImplInfoAndList> {
        return getAlbumListImpl(page, filter, repository)
    }

    /**
     * Создает экземпляр [AlbumTopHitsImpl] для наблюдения за топовыми альбомами.
     */
    fun getAlbumTopHits(): AlbumTopHitsImpl {
        return AlbumTopHitsImpl(repository, scope)
    }

    /**
     * Запрашивает промо-данные альбомов для экрана посадочной страницы тега.
     */
    suspend fun getLandingPageAlbumTag(tag : String =  "Blonde"): Result<Landing_page_albumType> {
        return LandingPageAlbumTag(tag, repository)
    }

    /**
     * Запрашивает промо-данные альбомов для посадочной страницы поиска.
     */
    suspend fun getLandingPageAlbumSearch(search : String, limit : Int = 9): Result<Landing_page_albumType> {
        return LandingPageAlbumSearch(search, repository, limit)
    }
}

private val ALBUM_ID_REGEX = Regex("(?:^|/)albums/(?:[^/]*_)?(\\d+)")

/**
 * Вспомогательная функция для извлечения числового ID альбома из произвольного URL Luscious.
 */
internal fun extractIdFromUrl(url: String): String? {
    if (url.length < 8) return null
    val trimmed = url.trim()
    if (!trimmed.contains("albums/")) return null
    return ALBUM_ID_REGEX.find(trimmed)?.groupValues?.getOrNull(1)
}

/**
 * Публичная функция для безопасного извлечения ID альбома из URL.
 */
fun extractAlbumIdOrNull(input: String?): String? =
    if (input.isNullOrBlank()) null else extractIdFromUrl(input)

/**
 * Проверяет, является ли строка корректным URL страницы альбома Luscious.
 */
fun isValidAlbumUrl(url: String?): Boolean =
    !extractAlbumIdOrNull(url).isNullOrBlank()

