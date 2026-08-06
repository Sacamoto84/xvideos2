package com.client.xvideos.r.network.api

import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.fileDB.folder.FileStringCacheTable
import com.client.xvideos.r.model.CreatorResponse
import com.client.xvideos.r.model.MediaResponse
import com.client.xvideos.r.model.MediaType
import com.client.xvideos.r.model.NicheResponse
import com.client.xvideos.r.model.NichesResponse
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.TopCreatorsResponse
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.model.search.SearchItemNichesResponse
import com.client.xvideos.r.model.search.SearchNichesShortResponse
import com.client.xvideos.r.model.tag.TagSuggestion
import com.client.xvideos.r.network.http.ApiClient
import com.client.xvideos.r.network.http.Route
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedApi @Inject constructor(
   db: AppFileDatabase
) {

    val api = ApiClient
    private val mediaCache = db.rCacheMediaResponse

    val explorer = RedApi_Explorer(api)
    val search = RedApi_Search(api)
    val tags = RedApi_Tags(api)

    //--------------------------- GIF methods ---------------------------

    // Здесь был getGif(id) — вызовов не имел, и разбирал ответ не той моделью:
    // /v2/gifs/{id} отдаёт {gif, user, niches}, одну гифку, а метод просил
    // MediaResponse с полями во множественном числе и без значений по
    // умолчанию. Gson в таком случае идёт через Unsafe.allocateInstance, поля
    // остаются null вопреки non-null типам, и первое обращение даёт NPE — тот
    // же дефект, что чинили в 6da42db. Форма ответа записана в
    // docs/redgifs-api.md, если метод понадобится снова.

    /**
     * ## Получить топ GIF-ов за неделю.
     * Работает
     */
    suspend fun getTopThisWeek(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = "/v2/gifs/search?order=top7&count={count}&page={page}&type={type}",
            "count" to count,
            "page" to page,
            "type" to type.value,
        )
        return cacheMediaResponse(route, this, mediaCache)
    }


    suspend fun getTopThisMonth(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = "/v2/gifs/search?order=top28&count={count}&page={page}&type={type}",
            "count" to count,
            "page" to page,
            "type" to type.value
        )
        return cacheMediaResponse(route, this, mediaCache)
    }


    /**
     * ## Топ за всё время.
     *
     * `order=top` — именно он и означает «без ограничения по времени»: рядом
     * `top7` это неделя, `top28` месяц. Значения `alltime` у RedGifs нет,
     * `/v2/gifs/search` отвечает на него 400 BadOrder (проверено 06.08.2026,
     * docs/redgifs-api.md). До появления этого метода выбор «All time» в меню
     * уходил в `else` и молча отдавал неделю.
     */
    suspend fun getTopAllTime(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = "/v2/gifs/search?order=top&count={count}&page={page}&type={type}",
            "count" to count,
            "page" to page,
            "type" to type.value
        )
        return cacheMediaResponse(route, this, mediaCache)
    }


    suspend fun getTopTrending(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = "/v2/gifs/search?order=trending&count={count}&page={page}&type={type}",
            "count" to count,
            "page" to page,
            "type" to type.value
        )
        return cacheMediaResponse(route, this, mediaCache)
    }

    //Последние, новые посты, не нужно кешировать

    suspend fun getTopLatest(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = "/v2/gifs/search?order=latest&count={count}&page={page}&type={type}",
            "count" to count,
            "page" to page,
            "type" to type.value
        )

        Timber.i("!!! getTopLatest ${route.url}")
        // Запрос из сети


        val r = api.request<MediaResponse>(route)
        return r
        //val res: MediaResponse = api.request(route)

//        res.onSuccess { dto ->
//            Timber.i("Got user: $dto")
//        }.onFailure { e ->
//            Timber.e(e, "Request failed")
//        }
//
//
//        return res

    }

    //--------------------------- User/Creator methods ---------------------------


    //https://api.redgifs.com/v1/users/drfunkenfootz_md
    suspend fun readCreator(
        userName: String = "lilijunex",
    ): Result<UserInfo> {
        val route = Route(
            method = "GET",
            path = "/v1/users/{username}",
            "username" to userName,
        )

        val res = api.request<UserInfo>(route)
        return res
    }


    /**
     * ```kotlin
     *  Получить последние 50 элементов
     *  https://api.redgifs.com/v2/users/panteritaaaa/search?order=latest&count=50&page=1
     *  https://api.redgifs.com/v2/users/relative_rub/search?order=latest&count=50&page=1
     * ```
     *
     * Версия с тегами
     * ```kotlin
     *  https://api.redgifs.com/v2/users/entakeeke1a/search?order=new&count=40&tags=Amateur%2CArmpit%2CArmpits
     * ```
     */
    suspend fun searchCreator( userName: String = "lilijunex", page: Int = 1, count: Int = 100, order: Order = Order.LATEST, type: MediaType = MediaType.GIF, tags: List<String> = emptyList() ): Result <CreatorResponse> {

        val route = if (type == MediaType.ALL){

            if (tags.isNotEmpty()) Route(
                method = "GET",
                path = "/v2/users/{username}/search?order={order}&page={page}&count={count}&&tags={tags}",
                "username" to userName,
                "page" to page,
                "count" to count,
                "order" to order.value,
                "tags" to tags.joinToString(",")
            )
            else Route(
                method = "GET",
                path = "/v2/users/{username}/search?page={page}&count={count}&order={order}",
                "username" to userName,
                "page" to page,
                "count" to count,
                "order" to order.value,
            )
        }
        else {
            if (tags.isNotEmpty()) Route(
                method = "GET",
                path = "/v2/users/{username}/search?order={order}&page={page}&count={count}&type={type}&tags={tags}",
                "username" to userName,
                "page" to page,
                "count" to count,
                "order" to order.value,
                "type" to type.value,
                "tags" to tags.joinToString(",")
            )
            else Route(
                method = "GET",
                path = "/v2/users/{username}/search?page={page}&count={count}&order={order}&type={type}",
                "username" to userName,
                "page" to page,
                "count" to count,
                "order" to order.value,
                "type" to type.value
            )
        }

        val res = api.request<CreatorResponse>(route)
        return res
    }


    // Здесь были getTrendingGifs() и getTrendingImages() на /v2/explore/…, а
    // также searchImage() — поиск по картинкам через type=i. Вызовов ни у
    // одного не было. Первые два вдобавок ведут на адреса, которых больше нет:
    // /v2/explore/trending-gifs и /v2/explore/trending-images отвечают 404
    // (проверено 06.08.2026). Комментарий «⭐ Работает ⭐» над одним из них
    // устарел вместе с адресом.

    //--------------------------- Tag methods ---------------------------


    //niches

    suspend fun getNiche(niches: String = "pumped-pussy"): Result<NicheResponse> {
        val route = Route(method = "GET", path = "/v2/niches/{niches}", "niches" to niches)
        return api.request<NicheResponse>(route)
    }

    //https://api.redgifs.com/v2/niches/cowgirl-pov/gifs?count=30&page=1&order=new
    suspend fun getNiches(
        niches: String = "pumped-pussy",
        page: Int = 1,
        count: Int = 100,
        order: Order = Order.LATEST
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = "/v2/niches/{niches}/gifs?page={page}&count={count}&order={order}",
            "niches" to niches,
            "page" to page,
            "count" to count,
            "order" to order.value
        )
        return cacheMediaResponse(route, this, mediaCache)
    }

    //Похожее
    //https://api.redgifs.com/v2/niches/pumped-pussy/related

    suspend fun getNichesRelated(niches: String = "pumped-pussy"): Result <NichesResponse> {
        val route = Route(method = "GET", path = "/v2/niches/{niches}/related", "niches" to niches)
        return api.request<NichesResponse>(route)
    }

    //https://api.redgifs.com/v2/niches/pumped-pussy/top-creators

    suspend fun getNichesTopCreators(niches: String = "pumped-pussy"): Result<TopCreatorsResponse> {
        val route =
            Route(method = "GET", path = "/v2/niches/{niches}/top-creators", "niches" to niches)
        return api.request(route)
    }

    data class TagsContainerGson(@SerializedName("tags") val tags: List<String>)

    //https://api.redgifs.com/v2/niches/pumped-pussy/top-tags

    suspend fun getNichesTopTags(niches: String = "pumped-pussy"): List<String> {
        val route = Route(method = "GET", path = "/v2/niches/{niches}/top-tags", "niches" to niches)
        return api.request<TagsContainerGson>(route).getOrNull()?.tags ?: emptyList()
    }


    //explorer


    //////////////////////////////////// Поиск ////////////////////////////////////


    // Здесь были две перегрузки searchCreators — на /v1/creators/search и на
    // /v2/search/creators — и закомментированная searchCreatorsLong. Вызовов ни
    // у одной не было. Адреса живы, но во второй перегрузке в пути стоял
    // `count={count}`, а параметра `count` в routeParams не было: плейсхолдер
    // уехал бы в запрос буквально. Тот же класс дефекта, что чинили в 9a646e1 и
    // 705c6c4, и снова незамеченный ровно потому, что метод не вызывается.
    //
    // Живой поиск авторов делает searchCreatorsShort в RedApi_Search.

    /**
     * ## Поиск ниш по тексту.
     * https://api.redgifs.com/v2/niches/search?query=Ana
     *
     * Разбор отдан ktor'у. Раньше тело забиралось через `requestText`, а
     * `String?` из `getOrNull()` уходил в `Gson().fromJson(res, listType)` — на
     * `null` Gson возвращает `null`, и присваивание в non-null
     * `SearchNichesShortResponse` роняло проверку Kotlin. То есть при отказе
     * сети отсюда прилетал NPE вместо ошибки сети.
     */
    suspend fun searchNichesShort(text: String): Result<List<SearchItemNichesResponse>> {
        val route = Route(method = "GET", path = "/v2/niches/search?query={text}", "text" to text)
        return api.request<SearchNichesShortResponse>(route).map { it.niches }
    }

    // Здесь был searchTagsShort на том же /v2/search/suggest, что и живой
    // getTagSuggestions ниже. Вызовов не имел, а при отказе сети падал бы NPE:
    // Gson().fromJson(res.getOrNull(), …) с null на входе возвращает null, и
    // присваивание в non-null List роняет проверку Kotlin. Подсказки тегов
    // берёт getTagSuggestions — он возвращает Result и разбирается ktor'ом.


    /**
     * ## Получить подсказки (suggest) по тегам.
     */
    suspend fun getTagSuggestions(query: String): Result <List<TagSuggestion>> {
        val route =
            Route(method = "GET", path = "/v2/search/suggest?query={query}", "query" to query)
        return api.request(route)
    }

}

/** Один Gson на весь модуль: сборка билдера на каждый запрос ничего не давала. */
private val mediaResponseGson: Gson = GsonBuilder().create()

/**
 * Ответ из кеша, а если там пусто — из сети, с укладкой в кеш.
 *
 * Возвращает [Result], а не голый [MediaResponse]. Раньше при отказе сети
 * отсюда уходил `MediaResponse(0, 0, 0, …)` — пустой объект вместо ошибки.
 * Дальше по цепочке `pages = 0` превращались в `nextKey = null`, и Paging
 * получал **успешную пустую страницу**: `LoadState.Error` не наступал, кнопки
 * «повторить» не было, экран просто показывал пустоту. При этом соседний
 * `getTopLatest` шёл через `Result` и ошибку показывал честно — одно и то же
 * приложение вело себя по-разному в зависимости от выбранной сортировки.
 */
private suspend fun cacheMediaResponse(
    route: Route,
    redApi: RedApi,
    cache: FileStringCacheTable
): Result<MediaResponse> {

    // Битая запись — не повод показывать ошибку: выкидываем её и идём дальше,
    // как будто кеша не было. Разбор общего Gson может вернуть и null, если в
    // файле оказался пустой JSON, — это тот же случай.
    val cached = cache.get(route.url)?.let { entry ->
        runCatching { mediaResponseGson.fromJson(entry.content, MediaResponse::class.java) }
            .getOrElse { e ->
                Timber.e(e, "!!! Битая запись кеша ${route.url}")
                null
            }
            ?: run {
                cache.delete(route.url)
                null
            }
    }

    if (cached != null) {
        Timber.i("!!! Берем данные из кеша ${route.url}")
        return Result.success(cached)
    }

    Timber.i("!!! Берем данные из Сети ${route.url}")
    return redApi.api.request<MediaResponse>(route)
        .onSuccess { cache.put(route.url, mediaResponseGson.toJson(it)) }
        .onFailure { Timber.e(it, "!!! Ошибка сети при запросе ${route.url}") }
}

