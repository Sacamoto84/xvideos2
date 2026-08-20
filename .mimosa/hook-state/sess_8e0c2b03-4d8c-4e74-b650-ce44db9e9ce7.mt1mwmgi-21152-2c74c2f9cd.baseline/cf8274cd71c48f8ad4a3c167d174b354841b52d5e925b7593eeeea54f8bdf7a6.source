package com.client.xvideos.l.net

import com.client.xvideos.l.model.Album
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.FacetCollectionInfo
import com.client.xvideos.l.net.graphQl.getAlbumListGraphQL1
import com.client.xvideos.l.net.graphQl.getAlbumListWithAggregations
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class AlbumListFilterGenreCountResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("term")
    val term: String,

    @SerializedName("is_active")
    val isActive: Boolean
)

data class AlbumListFilterGenreCountResponseList(
    @SerializedName("list")
    val list: List<AlbumListFilterGenreCountResponse>
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
                return Result.failure(result.exceptionOrNull()!!)
            }

            val res = result.getOrThrow()
            val json = JsonParser.parseString(res).asJsonObject
            val get =
                json["data"]?.asJsonObject?.get("album")?.asJsonObject?.get("list_with_aggregations")?.asJsonObject
            val aggregations = get?.get("aggregations")?.asJsonArray


            ////
            val indexGenre = aggregations?.mapIndexedNotNull { i, el ->
                val obj = el.asJsonObject
                val shortName = obj.getAsJsonObject("field")?.get("short_name")?.asString
                if (shortName == "genre_ids") i else null
            }?.firstOrNull()

            if (indexGenre != null) {
                val genreValues =
                    aggregations.get(indexGenre)?.getAsJsonObject()?.get("values")?.asJsonArray
                val gson = Gson()
                val list = mutableListOf<AlbumListFilterGenreCountResponse>()
                genreValues?.forEach { element ->
                    val pic = gson.fromJson(element, AlbumListFilterGenreCountResponse::class.java)
                    list.add(pic)
                }

                filterGenreStateCount.addAll(list)
                Timber.i("!!! getAlbumListAggregations list размер : ${list.size}")
            }

            ////
            val indexTagged = aggregations?.mapIndexedNotNull { i, el ->
                val obj = el.asJsonObject
                val shortName = obj.getAsJsonObject("field")?.get("short_name")?.asString
                if (shortName == "tagged") i else null
            }?.firstOrNull()

            if (indexTagged != null) {
                val taggedValues =
                    aggregations.get(indexTagged)?.getAsJsonObject()?.get("values")?.asJsonArray
                val gson = Gson()
                val list = mutableListOf<AlbumListFilterGenreCountResponse>()
                taggedValues?.forEach { element ->
                    val pic = gson.fromJson(element, AlbumListFilterGenreCountResponse::class.java)
                    list.add(pic)
                }
                withContext(Dispatchers.Main) {
                    filterTaggedStateCount.addAll(list)
                    Timber.i(
                        "!!! getAlbumListAggregations list Tagged размер : ${list.size} ${
                            list.joinToString(
                                "\n"
                            ) { it.term }
                        }")
                }
            }
            ///
            val indexPicture = aggregations?.mapIndexedNotNull { i, el ->
                val obj = el.asJsonObject
                val shortName = obj.getAsJsonObject("field")?.get("short_name")?.asString
                if (shortName == "picture_count_rank") i else null
            }
                ?.firstOrNull()

            if (indexPicture != null) {
                val pictureValues = aggregations.get(indexPicture)?.getAsJsonObject()?.get("values")?.asJsonArray
                val gson = Gson()
                val list = mutableListOf<AlbumListFilterGenreCountResponse>()
                pictureValues?.forEach { element ->
                    val pic = gson.fromJson(element, AlbumListFilterGenreCountResponse::class.java)
                    list.add(pic)
                }

                filterPictureCountStateCount.addAll(list)
                Timber.i("!!! getAlbumListAggregations list filterPictureCountStateCount размер : ${list.size}")

            }

            filterPictureCountStateCount
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
    val json = JsonParser.parseString(response).asJsonObject
    val listJson = json["data"]
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.get("album")
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.get("list")
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?: error("AlbumList response missing data.album.list")

    val infoJson = listJson["info"]
        ?.takeIf { it.isJsonObject }
        ?: error("AlbumList response missing data.album.list.info")

    val itemsJson = listJson["items"]
        ?.takeIf { it.isJsonArray }
        ?.asJsonArray
        ?: error("AlbumList response missing data.album.list.items")

    val gson = Gson()
    val info = gson.fromJson(infoJson, FacetCollectionInfo::class.java)
        ?: error("AlbumList response info is empty")
    val items = itemsJson.mapNotNull { itemJson ->
        gson.fromJson(itemJson, Album::class.java)
    }

    AlbumListImplInfoAndList(
        info = info,
        items = items,
        filter = filter,
        page = page
    )
}
