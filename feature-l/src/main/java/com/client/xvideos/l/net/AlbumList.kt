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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

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
)


data class getAlbumListAggregationsResult(
    val filterGenreStateCount: List<AlbumListFilterGenreCountResponse>,
    val filterTaggedStateCount: List<AlbumListFilterGenreCountResponse>,
    val filterPictureCountStateCount: List<AlbumListFilterGenreCountResponse>,
    val id: Int,
    val filter: AlbumListFilter?
)





//    var info by mutableStateOf(
//        FacetCollectionInfo(
//            page = 1,
//            hasNextPage = false,
//            hasPreviousPage = false,
//            totalItems = 0,
//            totalPages = 1,
//            itemsPerPage = 30,
//            urlComplete = ""
//        )
//    )


    suspend fun getAlbumListAggregationsImpl(page: Int, filterIn: AlbumListFilter?, repository: Repository): Result<getAlbumListAggregationsResult> {

        val filterGenreStateCount = mutableListOf<AlbumListFilterGenreCountResponse>()
        val filterTaggedStateCount = mutableListOf<AlbumListFilterGenreCountResponse>()
        val filterPictureCountStateCount = mutableListOf<AlbumListFilterGenreCountResponse>()

        val filter = filterIn ?: AlbumListFilter()

        try {
            Timber.i("!!! getAlbumListAggregations $page")

            val q = getAlbumListWithAggregations(page, filter)

            //Timber.i("!!! getAlbumListAggregations $q")

            val result = repository.openURI(q)
            if (result.isFailure) {
                Timber.i("!!! getAlbumListAggregations error ${result.exceptionOrNull()}")
                return Result.failure(result.exceptionOrNull() ?: IllegalStateException("Failed to load album aggregations"))
            }

            val res = result.getOrThrow()
            val json = LJson.parseToJsonElement(res).jsonObject
            val get = json["data"]?.jsonObject?.get("album")?.jsonObject?.get("list_with_aggregations")?.jsonObject
            val aggregations = get?.get("aggregations")?.jsonArray

            val indexGenre = aggregations?.indexOfFirst { el ->
                (el as? JsonObject)?.get("field")?.jsonObject?.get("short_name")?.jsonPrimitive?.contentOrNull == "genre_ids"
            }?.takeIf { it >= 0 }

            if (indexGenre != null) {
                val genreValues = aggregations[indexGenre].jsonObject["values"]?.jsonArray
                val list = genreValues?.mapNotNull { element ->
                    runCatching { LJson.decodeFromJsonElement<AlbumListFilterGenreCountResponse>(element) }.getOrNull()
                }.orEmpty()

                filterGenreStateCount.addAll(list)
                Timber.i("!!! getAlbumListAggregations list размер : ${list.size}")
            }

            val indexTagged = aggregations?.indexOfFirst { el ->
                (el as? JsonObject)?.get("field")?.jsonObject?.get("short_name")?.jsonPrimitive?.contentOrNull == "tagged"
            }?.takeIf { it >= 0 }

            if (indexTagged != null) {
                val taggedValues = aggregations[indexTagged].jsonObject["values"]?.jsonArray
                val list = taggedValues?.mapNotNull { element ->
                    runCatching { LJson.decodeFromJsonElement<AlbumListFilterGenreCountResponse>(element) }.getOrNull()
                }.orEmpty()
                filterTaggedStateCount.addAll(list)
                Timber.i("!!! getAlbumListAggregations list Tagged размер : ${list.size}")
            }

            val indexPicture = aggregations?.indexOfFirst { el ->
                (el as? JsonObject)?.get("field")?.jsonObject?.get("short_name")?.jsonPrimitive?.contentOrNull == "picture_count_rank"
            }?.takeIf { it >= 0 }

            if (indexPicture != null) {
                val pictureValues = aggregations[indexPicture].jsonObject["values"]?.jsonArray
                val list = pictureValues?.mapNotNull { element ->
                    runCatching { LJson.decodeFromJsonElement<AlbumListFilterGenreCountResponse>(element) }.getOrNull()
                }.orEmpty()

                filterPictureCountStateCount.addAll(list)
                Timber.i("!!! getAlbumListAggregations list filterPictureCountStateCount размер : ${list.size}")
            }
        } catch (e: CancellationException) {
            // Отмена корутины не должна превращаться в Result.failure: вызывающий
            // показывает такой failure снекбаром уже на другом экране.
            throw e
        } catch (e: Exception) {
            Timber.w("!!! getAlbumListAggregations Exception ${e.localizedMessage}")
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
     * Получить список альбомов с учетом фильтра
     */
    suspend fun getAlbumListImpl(
        page: Int,
        filterIn: AlbumListFilter?,
        repository: Repository,
    ): Result<AlbumListImplInfoAndList>
    {
        val items = mutableListOf<Album>()
        try {
            Timber.i("!!! getAlbumList $page")
            val filter = filterIn ?: AlbumListFilter()
            val q = getAlbumListGraphQL1(page, filter)

            val result = repository.openURI(q, config = RepositoryUriConfig.CACHE_RAM )

            if (result.isFailure) {
                Timber.w("!!! getAlbumList error: ${result.exceptionOrNull()?.message}")
                return Result.failure(result.exceptionOrNull() ?: IllegalStateException("getAlbumList unknown error"))
            }
            val parsed = parseAlbumListResponse(result.getOrThrow(), filter, page)
            if (parsed.isFailure) {
                Timber.w("!!! getAlbumList parse error: ${parsed.exceptionOrNull()?.message}")
                repository.deleteCache(q, RepositoryUriConfig.CACHE_RAM)
                repository.deleteCache(q, RepositoryUriConfig.CACHE_ROM)
                return Result.failure(parsed.exceptionOrNull() ?: IllegalStateException("getAlbumList parse error"))
            }

            val parsedResult = parsed.getOrThrow()
            val info = parsedResult.info
            items.addAll(parsedResult.items)
            //Timber.i("!!! getAlbumList info ${info.page} ${items.toList()}")
            return Result.success(
                AlbumListImplInfoAndList(
                    info = info,
                    items = items,
                    filter = filter,
                    page = page
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w("!!! getAlbumList Exception ${e.localizedMessage}")
            return Result.failure(e)
        }
    }

private fun parseAlbumListResponse(
    response: String,
    filter: AlbumListFilter,
    page: Int
): Result<AlbumListImplInfoAndList> = runCatching {
    val json = LJson.parseToJsonElement(response).jsonObject
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
    val items = itemsJson.mapNotNull { itemJson ->
        runCatching { LJson.decodeFromJsonElement<Album>(itemJson) }.getOrNull()
    }

    AlbumListImplInfoAndList(
        info = info,
        items = items,
        filter = filter,
        page = page
    )
}
