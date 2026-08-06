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
     *
     *         Возвращает объект `Album` на основе параметра albumInput;
     *
     *         albumInput can either be an integer, being the album Id
     *         Example (NSFW)<https://www.luscious.net/albums/animated-gifs_374481/>'s Id being 374481
     *         Or it can be a string, the link itself
     *
     */
    fun getAlbum(
        albumInput: Any,
        download: Boolean = false,
        requestScope: CoroutineScope = scope
    ): AlbumInfo {

        val id = when (albumInput) {
            is Int -> albumInput.toString()
            is Long -> albumInput.toString()
            is String -> extractIdFromUrl(albumInput) ?: albumInput // Если URL, извлекаем ID, иначе используем как есть
            else -> throw IllegalArgumentException("albumInput must be Int or String")
        }

        return AlbumInfo(id.toInt(), download, repository, requestScope)
    }

    // Вспомогательная функция для извлечения ID из URL
    private fun extractIdFromUrl(url: String): String? {
        val regex = Regex("/albums/[^_]+_(\\d+)")
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }

    suspend fun getAlbumListAggregations(page: Int, filter: AlbumListFilter?): Result<getAlbumListAggregationsResult> {
        return getAlbumListAggregationsImpl(page, filter, repository)
    }

    suspend fun getAlbumList(page: Int, filter: AlbumListFilter?): Result<AlbumListImplInfoAndList> {
        return getAlbumListImpl(page, filter, repository)
    }

    fun getAlbumTopHits(): AlbumTopHitsImpl {
        return AlbumTopHitsImpl(repository, scope)
    }

    suspend fun getLandingPageAlbumTag(tag : String =  "Blonde"): Result<Landing_page_albumType> {
        return LandingPageAlbumTag(tag, repository)
    }

    suspend fun getLandingPageAlbumSearch(search : String, limit : Int = 9): Result<Landing_page_albumType> {
        return LandingPageAlbumSearch(search, repository, limit)
    }

}
