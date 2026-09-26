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
import com.client.xvideos.r.network.json.RJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Шаблон пути для получения топа GIF за неделю (7 дней). */
private const val PATH_TOP7 = "/v2/gifs/search?order=top7&count={count}&page={page}&type={type}"
/** Шаблон пути для получения топа GIF за месяц (28 дней). */
private const val PATH_TOP28 = "/v2/gifs/search?order=top28&count={count}&page={page}&type={type}"
/** Шаблон пути для получения топа GIF за всё время. */
private const val PATH_TOP = "/v2/gifs/search?order=top&count={count}&page={page}&type={type}"
/** Шаблон пути для получения трендовых GIF. */
private const val PATH_TRENDING = "/v2/gifs/search?order=trending&count={count}&page={page}&type={type}"
/** Шаблон пути для получения самых свежих (последних) GIF. */
private const val PATH_LATEST = "/v2/gifs/search?order=latest&count={count}&page={page}&type={type}"
/** Шаблон пути для поиска GIF пользователя с фильтрацией по тегам. */
private const val PATH_USER_SEARCH_TAGS = "/v2/users/{username}/search?order={order}&page={page}&count={count}&tags={tags}"
/** Шаблон пути для поиска GIF пользователя без фильтрации по типу медиа и тегам. */
private const val PATH_USER_SEARCH = "/v2/users/{username}/search?page={page}&count={count}&order={order}"
/** Шаблон пути для поиска GIF пользователя с фильтром по типу медиа и тегам. */
private const val PATH_USER_SEARCH_TYPE_TAGS = "/v2/users/{username}/search?order={order}&page={page}&count={count}&type={type}&tags={tags}"
/** Шаблон пути для поиска GIF пользователя с фильтром по типу медиа. */
private const val PATH_USER_SEARCH_TYPE = "/v2/users/{username}/search?page={page}&count={count}&order={order}&type={type}"

/**
 * Главный фасад API сервиса RedGifs.
 *
 * Предоставляет методы для получения:
 * - Популярных и трендовых лент медиа (неделя, месяц, всё время, тренды, новые) с кэшированием в [mediaCache];
 * - Профилей авторов и их опубликованных работ;
 * - Ниш (тематических разделов), связанных ниш и топовых авторов в них;
 * - Подсказок поиска по тегам и нишам.
 *
 * Делегирует узкоспециализированные запросы в:
 * - [explorer] ([RedApi_Explorer]): просмотр каталога ниш;
 * - [search] ([RedApi_Search]): полнотекстовый поиск медиа и авторов;
 * - [tags] ([RedApi_Tags]): справочник тегов.
 *
 * @property db Локальная файловая БД для кэширования HTTP-ответов.
 */
@Singleton
class RedApi @Inject constructor(
   db: AppFileDatabase
) {

    /** Сетевой HTTP-клиент с авторизацией. */
    val api = ApiClient

    /** Таблица файлового дискового кэша для [MediaResponse]. */
    private val mediaCache = db.rCacheMediaResponse

    /** API подраздела каталога ниш. */
    val explorer = RedApi_Explorer(api)

    /** API полнотекстового поиска гифок и авторов. */
    val search = RedApi_Search(api)

    /** API справочника и подсказок тегов. */
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
     * Получить топ GIF за последнюю неделю (`order=top7`).
     * Ответ кэшируется на диске через [cacheMediaResponse].
     *
     * @param count Количество элементов на страницу.
     * @param page Номер страницы (1-based).
     * @param type Тип медиа (GIF, image и т.д.).
     */
    suspend fun getTopThisWeek(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = PATH_TOP7,
            "count" to count,
            "page" to page,
            "type" to type.value,
        )
        return cacheMediaResponse(route, this, mediaCache)
    }

    /**
     * Получить топ GIF за последний месяц (`order=top28`).
     * Ответ кэшируется на диске через [cacheMediaResponse].
     *
     * @param count Количество элементов на страницу.
     * @param page Номер страницы (1-based).
     * @param type Тип медиа (GIF, image и т.д.).
     */
    suspend fun getTopThisMonth(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = PATH_TOP28,
            "count" to count,
            "page" to page,
            "type" to type.value
        )
        return cacheMediaResponse(route, this, mediaCache)
    }

    /**
     * Получить топ GIF за всё время (`order=top`).
     *
     * `order=top` — именно он означает «без ограничения по времени»: рядом
     * `top7` это неделя, `top28` месяц. Значения `alltime` у RedGifs нет,
     * `/v2/gifs/search` отвечает на него 400 BadOrder (проверено 06.08.2026,
     * docs/redgifs-api.md). До появления этого метода выбор «All time» в меню
     * уходил в `else` и молча отдавал неделю.
     *
     * @param count Количество элементов на страницу.
     * @param page Номер страницы (1-based).
     * @param type Тип медиа (GIF, image и т.д.).
     */
    suspend fun getTopAllTime(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = PATH_TOP,
            "count" to count,
            "page" to page,
            "type" to type.value
        )
        return cacheMediaResponse(route, this, mediaCache)
    }

    /**
     * Получить трендовые GIF (`order=trending`).
     * Ответ кэшируется на диске через [cacheMediaResponse].
     *
     * @param count Количество элементов на страницу.
     * @param page Номер страницы (1-based).
     * @param type Тип медиа (GIF, image и т.д.).
     */
    suspend fun getTopTrending(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = PATH_TRENDING,
            "count" to count,
            "page" to page,
            "type" to type.value
        )
        return cacheMediaResponse(route, this, mediaCache)
    }

    /**
     * Получить самые свежие опубликованные посты (`order=latest`).
     * Намеренно не кэшируется, так как выдача постоянно обновляется.
     *
     * @param count Количество элементов на страницу.
     * @param page Номер страницы (1-based).
     * @param type Тип медиа (GIF, image и т.д.).
     */
    suspend fun getTopLatest(
        count: Int,                      // количество элементов на страницу.
        page: Int,                       // номер страницы (1-based).
        type: MediaType = MediaType.GIF, // тип медиа (GIF, image и т.д.).
    ): Result<MediaResponse> {
        val route = Route(
            method = "GET",
            path = PATH_LATEST,
            "count" to count,
            "page" to page,
            "type" to type.value
        )

        Timber.d("getTopLatest ${route.url}")
        return api.request<MediaResponse>(route)
    }

    //--------------------------- User/Creator methods ---------------------------

    /**
     * Загрузить подробную информацию о пользователе/авторе по его никнейму.
     *
     * @param userName Имя пользователя (username).
     */
    suspend fun readCreator(
        userName: String,
    ): Result<UserInfo> {
        val route = Route(
            method = "GET",
            path = "/v1/users/{username}",
            "username" to userName,
        )

        return api.request<UserInfo>(route)
    }

    /**
     * Поиск и пагинация опубликованных GIF конкретного создателя.
     *
     * Поддерживает:
     * - Фильтрацию по типу контента ([MediaType]);
     * - Фильтрацию по списку тегов ([tags]);
     * - Различные порядки сортировки ([Order]).
     *
     * @param userName Имя создателя.
     * @param page Номер страницы (1-based).
     * @param count Количество элементов на страницу.
     * @param order Порядок сортировки.
     * @param type Тип контента (GIF, Image, All).
     * @param tags Список тегов для сужения выдачи.
     */
    suspend fun searchCreator(
        userName: String,
        page: Int = 1,
        count: Int = 100,
        order: Order = Order.LATEST,
        type: MediaType = MediaType.GIF,
        tags: List<String> = emptyList()
    ): Result<CreatorResponse> {

        val route = if (type == MediaType.ALL) {

            if (tags.isNotEmpty()) Route(
                method = "GET",
                path = PATH_USER_SEARCH_TAGS,
                "username" to userName,
                "page" to page,
                "count" to count,
                "order" to order.value,
                "tags" to tags.joinToString(",")
            )
            else Route(
                method = "GET",
                path = PATH_USER_SEARCH,
                "username" to userName,
                "page" to page,
                "count" to count,
                "order" to order.value,
            )
        } else {
            if (tags.isNotEmpty()) Route(
                method = "GET",
                path = PATH_USER_SEARCH_TYPE_TAGS,
                "username" to userName,
                "page" to page,
                "count" to count,
                "order" to order.value,
                "type" to type.value,
                "tags" to tags.joinToString(",")
            )
            else Route(
                method = "GET",
                path = PATH_USER_SEARCH_TYPE,
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

    //--------------------------- Tag methods ---------------------------

    /**
     * Получить детальную информацию о конкретной нише по её идентификатору.
     *
     * @param niches Идентификатор или слаг ниши.
     */
    suspend fun getNiche(niches: String): Result<NicheResponse> {
        val trimmed = niches.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Niche identifier cannot be blank"))
        val route = Route(method = "GET", path = "/v2/niches/{niches}", "niches" to trimmed)
        return api.request<NicheResponse>(route)
    }

    /**
     * Загрузить страницу гифок, входящих в указанную нишу.
     * Ответ кэшируется через [cacheMediaResponse].
     *
     * @param niches Идентификатор ниши.
     * @param page Номер страницы.
     * @param count Количество элементов на страницу.
     * @param order Порядок сортировки.
     */
    suspend fun getNiches(
        niches: String,
        page: Int = 1,
        count: Int = 100,
        order: Order = Order.LATEST
    ): Result<MediaResponse> {
        val trimmed = niches.trim()
        if (trimmed.isEmpty()) return Result.success(MediaResponse())
        val route = Route(
            method = "GET",
            path = "/v2/niches/{niches}/gifs?page={page}&count={count}&order={order}",
            "niches" to trimmed,
            "page" to page,
            "count" to count,
            "order" to order.value
        )
        return cacheMediaResponse(route, this, mediaCache)
    }

    /**
     * Загрузить список похожих/связанных ниш для заданной ниши.
     *
     * @param niches Идентификатор ниши.
     */
    suspend fun getNichesRelated(niches: String): Result<NichesResponse> {
        val trimmed = niches.trim()
        if (trimmed.isEmpty()) return Result.success(NichesResponse())
        val route = Route(method = "GET", path = "/v2/niches/{niches}/related", "niches" to trimmed)
        return api.request<NichesResponse>(route)
    }

    /**
     * Получить топ создателей в указанной нише.
     *
     * @param niches Идентификатор ниши.
     */
    suspend fun getNichesTopCreators(niches: String): Result<TopCreatorsResponse> {
        val trimmed = niches.trim()
        if (trimmed.isEmpty()) return Result.success(TopCreatorsResponse())
        val route =
            Route(method = "GET", path = "/v2/niches/{niches}/top-creators", "niches" to trimmed)
        return api.request(route)
    }

    /**
     * Контейнер ответа для списка тегов ниши.
     *
     * @property tags Список строк тегов.
     */
    @Serializable
    data class TagsContainer(
        @SerialName("tags") val tags: List<String> = emptyList()
    )

    /**
     * Получить популярные теги в заданной нише.
     *
     * @param niches Идентификатор ниши.
     * @return Список имен тегов.
     */
    suspend fun getNichesTopTags(niches: String): List<String> {
        val trimmed = niches.trim()
        if (trimmed.isEmpty()) return emptyList()
        val route = Route(method = "GET", path = "/v2/niches/{niches}/top-tags", "niches" to trimmed)
        return api.request<TagsContainer>(route).getOrNull()?.tags ?: emptyList()
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
     * Поиск ниш по тексту (`/v2/niches/search?query=...`).
     *
     * Разбор отдан Ktor'у. Раньше тело забиралось через `requestText`, а
     * `String?` из `getOrNull()` уходил в `Gson().fromJson(res, listType)` — на
     * `null` Gson возвращает `null`, и присваивание в non-null
     * `SearchNichesShortResponse` роняло проверку Kotlin. То есть при отказе
     * сети отсюда прилетал NPE вместо ошибки сети.
     *
     * @param text Текст поискового запроса.
     */
    suspend fun searchNichesShort(text: String): Result<List<SearchItemNichesResponse>> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())
        val route = Route(method = "GET", path = "/v2/niches/search?query={text}", "text" to trimmed)
        return api.request<SearchNichesShortResponse>(route).map { it.niches }
    }

    // Здесь был searchTagsShort на том же /v2/search/suggest, что и живой
    // getTagSuggestions ниже. Вызовов не имел, а при отказе сети падал бы NPE:
    // Gson().fromJson(res.getOrNull(), …) с null на входе возвращает null, и
    // присваивание в non-null List роняет проверку Kotlin. Подсказки тегов
    // берёт getTagSuggestions — он возвращает Result и разбирается ktor'ом.

    /**
     * Получить подсказки (suggestions) по тегам для автодополнения строки поиска.
     *
     * @param query Начало поискового слова.
     */
    suspend fun getTagSuggestions(query: String): Result <List<TagSuggestion>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())
        val route =
            Route(method = "GET", path = "/v2/search/suggest?query={query}", "query" to trimmed)
        return api.request(route)
    }

}

/**
 * Ответ из файлового кеша, а если там пусто — из сети, с сохранением в кеш.
 *
 * Возвращает [Result], а не голый [MediaResponse]. Раньше при отказе сети
 * отсюда уходил `MediaResponse(0, 0, 0, …)` — пустой объект вместо ошибки.
 * Дальше по цепочке `pages = 0` превращались в `nextKey = null`, и Paging
 * получал успешную пустую страницу: `LoadState.Error` не наступал, кнопки
 * «повторить» не было, экран просто показывал пустоту. При этом соседний
 * `getTopLatest` шёл через `Result` и ошибку показывал честно — одно и то же
 * приложение вело себя по-разному в зависимости от выбранной сортировки.
 *
 * @param route Запрашиваемый маршрут.
 * @param redApi Экземпляр [RedApi] для выполнения сетевого запроса.
 * @param cache Таблица файлового кэша.
 */
private suspend fun cacheMediaResponse(
    route: Route,
    redApi: RedApi,
    cache: FileStringCacheTable
): Result<MediaResponse> {

    // Битая запись — не повод показывать ошибку: выкидываем её и идём дальше,
    // как будто кеша не было.
    val cached = cache.get(route.url)?.let { entry ->
        runCatching { RJson.decodeFromString<MediaResponse>(entry.content) }
            .getOrElse { e ->
                Timber.w(e, "Corrupted cache entry for ${route.url}")
                null
            }
            ?: run {
                cache.delete(route.url)
                null
            }
    }

    if (cached != null) {
        Timber.d("Loading from cache: ${route.url}")
        return Result.success(cached)
    }

    Timber.d("Fetching from network: ${route.url}")
    return redApi.api.request<MediaResponse>(route)
        .onSuccess {
            runCatching {
                cache.put(route.url, RJson.encodeToString(it))
            }.onFailure { e ->
                Timber.w(e, "Не удалось сохранить ответ в кэш: ${route.url}")
            }
        }
        .onFailure { Timber.w(it, "Network error during request: ${route.url}") }
}
