package com.client.xvideos.l.net

import com.client.xvideos.l.model.Album
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.FacetCollectionInfo
import com.client.xvideos.l.net.graphQl.getAlbumListGraphQL1
import com.client.xvideos.l.net.graphQl.getAlbumListWithAggregations
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Ответ агрегации по фильтру (количество альбомов по конкретному жанру/тегу).
 *
 * @property count Количество альбомов.
 * @property term Название термина/фильтра.
 * @property isActive Выбран ли фильтр пользователем.
 */
@Serializable
data class AlbumListFilterGenreCountResponse(
    @SerialName("count")
    val count: Int = 0,

    @SerialName("term")
    val term: String = "",

    @SerialName("is_active")
    val isActive: Boolean = false
)

@Serializable
data class AlbumListFilterGenreCountResponseList(
    @SerialName("list")
    val list: List<AlbumListFilterGenreCountResponse> = emptyList()
)

/**
 * Результат постраничной загрузки списка альбомов с пагинацией.
 *
 * @property info Метаданные пагинации (текущая страница, есть ли следующая/предыдущая, всего страниц).
 * @property items Список загруженных альбомов [Album].
 * @property filter Примененный фильтр.
 * @property page Номер запрошенной страницы.
 */
data class AlbumListImplInfoAndList(
    val info: FacetCollectionInfo = FacetCollectionInfo(
        page = 1,
        hasNextPage = true,
        hasPreviousPage = true,
        totalItems = 1,
        totalPages = 1,
        itemsPerPage = 1,
        urlComplete = ""
    ),
    val items: List<Album> = emptyList(),
    val filter: AlbumListFilter = AlbumListFilter(),
    val page: Int = 0
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val isNotEmpty: Boolean get() = items.isNotEmpty()
    val totalCount: Int get() = items.size
}

/**
 * Результат запроса агрегаций фильтров альбомов (распределение по жанрам, тегам и числу картинок).
 */
data class GetAlbumListAggregationsResult(
    val filterGenreStateCount: List<AlbumListFilterGenreCountResponse>,
    val filterTaggedStateCount: List<AlbumListFilterGenreCountResponse>,
    val filterPictureCountStateCount: List<AlbumListFilterGenreCountResponse>,
    val id: Int,
    val filter: AlbumListFilter?
) {
    val hasGenres: Boolean get() = filterGenreStateCount.isNotEmpty()
    val hasTags: Boolean get() = filterTaggedStateCount.isNotEmpty()
    val hasPictureCounts: Boolean get() = filterPictureCountStateCount.isNotEmpty()
}

typealias getAlbumListAggregationsResult = GetAlbumListAggregationsResult

/**
 * Запрашивает статистику и счетчики доступных фильтров (агрегации) для текущей страницы и фильтра.
 *
 * @param page Номер страницы.
 * @param filterIn Фильтр выдачи.
 * @param repository Репозиторий сетевых запросов.
 */
suspend fun getAlbumListAggregationsImpl(page: Int, filterIn: AlbumListFilter?, repository: Repository): Result<getAlbumListAggregationsResult> {

    var filterGenreStateCount: List<AlbumListFilterGenreCountResponse> = emptyList()
    var filterTaggedStateCount: List<AlbumListFilterGenreCountResponse> = emptyList()
    var filterPictureCountStateCount: List<AlbumListFilterGenreCountResponse> = emptyList()

    val filter = filterIn ?: AlbumListFilter()

    try {
        Timber.d("getAlbumListAggregations $page")

        val query = getAlbumListWithAggregations(page, filter)

        val result = repository.openURI(query)
        if (result.isFailure) {
            Timber.w("getAlbumListAggregations error ${result.exceptionOrNull()}")
            return Result.failure(result.exceptionOrNull() ?: IllegalStateException("Failed to load album aggregations"))
        }

        val res = result.getOrThrow()
        val json = LJson.parseToJsonElement(res).jsonObject
        val get = json["data"]?.jsonObject?.get("album")?.jsonObject?.get("list_with_aggregations")?.jsonObject
        val aggregations = get?.get("aggregations")?.jsonArray

        if (aggregations != null) {
            for (el in aggregations) {
                val obj = el as? JsonObject ?: continue
                when (obj["field"]?.jsonObject?.get("short_name")?.jsonPrimitive?.contentOrNull) {
                    "genre_ids" -> {
                        filterGenreStateCount = decodeAggregationValues(obj)
                        Timber.d("getAlbumListAggregations list size: ${filterGenreStateCount.size}")
                    }
                    "tagged" -> {
                        filterTaggedStateCount = decodeAggregationValues(obj)
                        Timber.d("getAlbumListAggregations list Tagged size: ${filterTaggedStateCount.size}")
                    }
                    "picture_count_rank" -> {
                        filterPictureCountStateCount = decodeAggregationValues(obj)
                        Timber.d("getAlbumListAggregations list filterPictureCountStateCount size: ${filterPictureCountStateCount.size}")
                    }
                }
            }
        }
    } catch (e: CancellationException) {
        // Отмена корутины не должна превращаться в Result.failure: вызывающий
        // показывает такой failure снекбаром уже на другом экране.
        throw e
    } catch (e: Exception) {
        Timber.w("getAlbumListAggregations Exception ${e.localizedMessage}")
        return Result.failure(e)
    }

    return Result.success(
        getAlbumListAggregationsResult(
            filterGenreStateCount,
            filterTaggedStateCount,
            filterPictureCountStateCount,
            page,
            filter
        )
    )
}

/**
 * Декодирует элементы массива значений агрегации.
 */
private fun decodeAggregationValues(obj: JsonObject): List<AlbumListFilterGenreCountResponse> {
    val values = obj["values"]?.jsonArray ?: return emptyList()
    if (values.isEmpty()) return emptyList()
    val result = ArrayList<AlbumListFilterGenreCountResponse>(values.size)
    for (element in values) {
        runCatching { LJson.decodeFromJsonElement<AlbumListFilterGenreCountResponse>(element) }
            .getOrNull()
            ?.let { result.add(it) }
    }
    return result
}


/**
 * Запрашивает постраничный список альбомов с учетом заданного фильтра [filterIn].
 *
 * @param page Номер страницы (1, 2, ...).
 * @param filterIn Настройки фильтрации (жанр, сортировка, теги).
 * @param repository Репозиторий сетевых запросов.
 * @return [Result] с метаданными пагинации и списком альбомов [AlbumListImplInfoAndList].
 */
suspend fun getAlbumListImpl(
    page: Int,
    filterIn: AlbumListFilter?,
    repository: Repository,
): Result<AlbumListImplInfoAndList>
{
    try {
        Timber.d("getAlbumList $page")
        val filter = filterIn ?: AlbumListFilter()
        val query = getAlbumListGraphQL1(page, filter)

        val result = repository.openURI(query, config = RepositoryUriConfig.CACHE_RAM )

        if (result.isFailure) {
            Timber.w("getAlbumList error: ${result.exceptionOrNull()?.message}")
            return Result.failure(result.exceptionOrNull() ?: IllegalStateException("getAlbumList unknown error"))
        }
        val parsed = parseAlbumListResponse(result.getOrThrow(), filter, page)
        if (parsed.isFailure) {
            Timber.w("getAlbumList parse error: ${parsed.exceptionOrNull()?.message}")
            repository.deleteCache(query, RepositoryUriConfig.CACHE_RAM)
            repository.deleteCache(query, RepositoryUriConfig.CACHE_ROM)
            return Result.failure(parsed.exceptionOrNull() ?: IllegalStateException("getAlbumList parse error"))
        }

        return parsed
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w("getAlbumList Exception ${e.localizedMessage}")
        return Result.failure(e)
    }
}

/**
 * Разбирает GraphQL ответ со списком альбомов.
 */
private fun parseAlbumListResponse(
    response: String,
    filter: AlbumListFilter,
    page: Int
): Result<AlbumListImplInfoAndList> = runCatching {
    val json = LJson.parseToJsonElement(response).jsonObject
    val errors = json["errors"]?.takeIf { it !is JsonNull }?.jsonArray
    if (!errors.isNullOrEmpty()) {
        val errorMsg = errors.firstOrNull()?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            ?: "GraphQL error loading album list"
        error(errorMsg)
    }

    val listJson = json["data"]
        ?.jsonObject
        ?.get("album")
        ?.jsonObject
        ?.get("list")
        ?.jsonObject
        ?: error("AlbumList response missing data.album.list")

    val infoJson = listJson["info"]
        ?.jsonObject
        ?: error("AlbumList response missing data.album.list.info")

    val itemsJson = listJson["items"]
        ?.jsonArray
        ?: error("AlbumList response missing data.album.list.items")

    val info = LJson.decodeFromJsonElement<FacetCollectionInfo>(infoJson)
    val items = if (itemsJson.isEmpty()) {
        emptyList()
    } else {
        val list = ArrayList<Album>(itemsJson.size)
        for (itemJson in itemsJson) {
            runCatching { LJson.decodeFromJsonElement<Album>(itemJson) }
                .getOrNull()
                ?.let { list.add(it) }
        }
        list
    }

    AlbumListImplInfoAndList(
        info = info,
        items = items,
        filter = filter,
        page = page
    )
}
