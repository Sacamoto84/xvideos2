package com.client.xvideos.l.net

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.isAnimatedMedia
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lBestThumbnailImageUrl
import com.client.xvideos.l.net.graphQl.GraphQlRequest
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.HTML_INSTEAD_OF_JSON_PREFIX
import com.client.xvideos.l.repository.LRepositoryProtectionUiState
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.client.xvideos.l.repository.toLUserMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * Описание проблемы/сбоя при загрузке конкретной страницы картинок альбома.
 *
 * @property page Номер сбойной страницы.
 * @property message Текст ошибки.
 * @property htmlChallenge `true`, если сервер вернул Cloudflare HTML-челлендж вместо JSON.
 * @property failedAtMs Метка времени сбоя в миллисекундах.
 */
@Immutable
data class LAlbumPageLoadIssue(
    val page: Int,
    val message: String,
    val htmlChallenge: Boolean,
    val failedAtMs: Long = System.currentTimeMillis()
) {
    val hasMessage: Boolean get() = message.isNotBlank()
}

/**
 * Снапшот картинок альбома для сохранения в кэш бандлов.
 *
 * @property pics Полный список разобранных элементов [PicsDetails].
 * @property totalPages Общее число страниц.
 */
@Immutable
data class LAlbumPicsBundleSnapshot(
    val pics: List<PicsDetails>,
    val totalPages: Int?
) {
    val isEmpty: Boolean get() = pics.isEmpty()
    val isNotEmpty: Boolean get() = pics.isNotEmpty()
    val count: Int get() = pics.size
}

/**
 * Менеджер пагинированной порционной загрузки картинок альбома Luscious.
 *
 * Управляет:
 * - Последовательной загрузкой чанков страниц картинок ([contentUrls]).
 * - Отслеживанием прогресса [percentLoad].
 * - Управлением сбойными страницами [failedPages] и их повторной попыткой ([retryFailedPages]).
 * - Восстановлением из дискового кэша бандлов ([restoreFromBundleCache]).
 *
 * Загрузка, повтор и восстановление идут строго по очереди: альбом полон,
 * только когда получена каждая его страница, и решает это одна процедура за раз.
 *
 * @property id Идентификатор альбома.
 * @property repository Репозиторий сетевых запросов.
 * @param dispatcher Поток разбора и сборки списка; подменяется в тестах.
 */
@Stable
class AlbumPicsDetails(
    val id: Int,
    val repository: Repository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private companion object {
        const val PAGE_REQUEST_DELAY_MS = 250L
    }

    /** Реактивный список загруженных картинок для отображения в сетке/пейджере. */
    val pics = mutableStateListOf<PicsDetails>()

    /** Общее расчетное число страниц картинок в альбоме. */
    var totalPages: Int? = null

    /** Прогресс загрузки картинок альбома от 0f до 1f. */
    var percentLoad by mutableFloatStateOf(0f)

    /** Флаг активного сетевого запроса страницы. */
    var isPageRequestInFlight by mutableStateOf(false)
        private set

    /** Флаг выполнения повторной загрузки сбойных страниц. */
    var isRetryingFailedPages by mutableStateOf(false)
        private set

    /** Список страниц, завершившихся ошибкой. */
    val failedPages = mutableStateListOf<LAlbumPageLoadIssue>()

    /** Реактивный статус сетевой защиты (Cloudflare/Captcha). */
    val protectionUiState: StateFlow<LRepositoryProtectionUiState>
        get() = repository.protectionUiState

    /** Успешно полученные страницы. Сбойных здесь нет — они в [failedPages]. */
    private val loadedPages = mutableMapOf<Int, List<PicsDetails>>()

    /**
     * Получена каждая страница альбома. Читается и пишется только под [stateMutex].
     *
     * Раньше полноту определяли по `percentLoad == 1f`, а единицу ставил и повтор
     * сбойных страниц — в том числе после сбоя первой страницы, когда остальные
     * никто не запрашивал, и посреди идущей загрузки.
     */
    private var loadComplete = false

    /**
     * Очередь процедур: [contentUrls], [retryFailedPages], [restoreFromBundleCache].
     * Повтор, нажатый во время загрузки, ждёт её конца и затем добирает всё,
     * чего не хватает.
     */
    private val loadMutex = Mutex()

    /** Растёт с каждой новой загрузкой: повтор прежней загрузки по нему узнаёт, что устарел. */
    private val loadGeneration = AtomicInteger()

    /**
     * Раньше роль этого мьютекса играл `withContext(Dispatchers.Main)`: он и
     * упорядочивал доступ к [loadedPages] между загрузкой и ретраем, и делал
     * изменения списка видимыми одним куском. Побочно вся пересборка списка
     * (O(n) на альбом в сотни картинок) выполнялась на UI-потоке.
     *
     * Теперь взаимное исключение обеспечивает мьютекс, атомарность публикации —
     * [Snapshot.withMutableSnapshot], а сама работа идёт на фоновом потоке.
     * Snapshot-состояние Compose допускает запись из любого потока.
     */
    private val stateMutex = Mutex()

    private data class PageLoadResult(
        val page: Int,
        val totalPages: Int,
        val items: List<PicsDetails>
    )

    /** Запрашивает страницу. Сбой сам попадает в [failedPages], успех убирает прежнюю запись о нём. */
    private suspend fun loadPage(
        page: Int,
        config: RepositoryUriConfig = RepositoryUriConfig.CACHE_RAM
    ): Result<PageLoadResult> {
        isPageRequestInFlight = true

        return try {
            openPage(page, config)
        } finally {
            // Присваивание не приостанавливается, поэтому отмена его не пропустит
            // и обёртка NonCancellable больше не нужна.
            isPageRequestInFlight = false
        }
    }

    private suspend fun openPage(
        page: Int,
        config: RepositoryUriConfig
    ): Result<PageLoadResult> {
        val request = GraphQlRequest.pictureListInsideAlbum(id, page)
        val pageResponse = repository.openURI(
            request,
            config = config
        ).mapCatching { parsePage(page, it) }

        if (pageResponse.isSuccess) {
            val pageResult = pageResponse.getOrThrow()
            clearPageIssue(page)
            return Result.success(pageResult)
        }

        val pageError = pageResponse.exceptionOrNull()
        recordPageIssue(page, pageError)
        if (pageError.isHtmlChallengeResponse()) {
            Timber.w(pageError, "AlbumPicsDetails $id page $page HTML challenge response")
            return Result.failure(pageError ?: IllegalStateException(HTML_INSTEAD_OF_JSON_PREFIX))
        }

        Timber.w(pageError, "AlbumPicsDetails $id page $page load error")
        return pageResponse
    }

    private fun parsePage(page: Int, response: String): PageLoadResult {
        val list = mutableListOf<PicsDetails>()

        val json = LJson.parseToJsonElement(response).jsonObject
        val get = json["data"]
            ?.asJsonObjectOrNull()
            ?.get("picture")
            ?.asJsonObjectOrNull()
            ?.get("list")
            ?.asJsonObjectOrNull()
            ?: error("AlbumPicsDetails response missing data.picture.list")

        get["errors"]
            ?.takeIf { it !is JsonNull }
            ?.let { error("AlbumPicsDetails response errors: ${it.toString().take(300)}") }

        val info = get["info"]?.asJsonObjectOrNull()
        val totalPagesFromInfo = info.readInt("total_pages")
        val totalItems = info.readInt("total_items")
        val itemsPerPage = info.readInt("items_per_page")
        val pages = calculateAlbumPages(totalPagesFromInfo, totalItems, itemsPerPage)

        val itemsArray = get["items"]?.takeIf { it is JsonArray }?.jsonArray
            ?: error("AlbumPicsDetails response missing data.picture.list.items")

        var skippedNoMediaCount = 0
        var parseErrorCount = 0
        itemsArray.forEachIndexed { index, element ->
            runCatching {
                LJson.decodeFromJsonElement<PicsDetails>(element)
            }.onSuccess { pic ->
                if (pic.hasAnyMediaUrl()) {
                    list.add(pic)
                } else {
                    skippedNoMediaCount++
                    Timber.w("AlbumPicsDetails $id page $page item $index has no media urls: $element")
                }
            }.onFailure {
                parseErrorCount++
                Timber.w(it, "AlbumPicsDetails $id page $page item $index parse error: $element")
            }
        }

        Timber.d(
            "AlbumPicsDetails [$id] Page $page/$pages chunk parsed: " +
            "valid=${list.size}, raw=${itemsArray.size}, " +
            "server info total_items=$totalItems, items_per_page=$itemsPerPage" +
            (if (skippedNoMediaCount > 0 || parseErrorCount > 0) " (skipped: noMedia=$skippedNoMediaCount, parseError=$parseErrorCount)" else "")
        )

        return PageLoadResult(page, pages, list)
    }

    /**
     * Запускает последовательную подгрузку всех страниц картинок альбома.
     *
     * @param pageCacheConfig Конфигурация кэширования ответов страниц.
     */
    suspend fun contentUrls(
        pageCacheConfig: RepositoryUriConfig = RepositoryUriConfig.CACHE_RAM
    ) = withContext(dispatcher) {
        val generation = loadGeneration.incrementAndGet()
        loadMutex.withLock {
            stateMutex.withLock {
                Snapshot.withMutableSnapshot {
                    pics.clear()
                    failedPages.clear()
                    loadedPages.clear()
                    loadComplete = false
                    totalPages = null
                    percentLoad = 0f
                    isPageRequestInFlight = false
                }
            }

            Timber.d("AlbumPicsDetails [$id] Starting chunked load (config=$pageCacheConfig)")
            loadMissingPages(pageCacheConfig, generation)
            Timber.d("AlbumPicsDetails [$id] Chunked load complete: ${pics.size} items loaded, failedPages count: ${failedPages.size}")
        }
    }

    /**
     * Загружает страницы, которых ещё нет: сбойные и те, до которых загрузка не
     * дошла. Вызывать под [loadMutex].
     *
     * Число страниц сообщает первая: без неё остальные не запросить. Поэтому
     * повтор после сбоя первой страницы — это загрузка альбома с начала, а не
     * одной страницы.
     */
    private suspend fun loadMissingPages(config: RepositoryUriConfig, generation: Int) {
        requestMissingPages(config, generation)
        // Отменённая загрузка сюда не доходит и полной не становится.
        stateMutex.withLock {
            val pages = totalPages
            loadComplete = pages != null && failedPages.isEmpty() && loadedPages.size == pages
            // Без первой страницы показывать нечего: полосу прячем, сбой виден в панели.
            if (loadComplete || pages == null) percentLoad = 1f
        }
    }

    private suspend fun requestMissingPages(config: RepositoryUriConfig, generation: Int) {
        if (!isPageLoaded(1)) {
            val firstPage = loadPage(1, config).getOrElse { return }
            appendPage(firstPage, firstPage.totalPages)
        }

        val pages = totalPages ?: return
        for (page in 2..pages) {
            // Началась новая загрузка: эта работает со списком, который та сейчас очистит.
            if (generation != loadGeneration.get()) return
            if (isPageLoaded(page)) continue
            loadPage(page, config).onSuccess { appendPage(it, pages) }
            delay(PAGE_REQUEST_DELAY_MS)
        }
    }

    private suspend fun isPageLoaded(page: Int): Boolean = stateMutex.withLock { loadedPages.containsKey(page) }

    /**
     * Восстанавливает список картинок из закэшированного бандла без выполнения сетевых запросов.
     */
    suspend fun restoreFromBundleCache(
        items: List<PicsDetails>,
        cachedTotalPages: Int?
    ) = withContext(dispatcher) {
        val corrected = normalizePictureUrls(items)
        loadGeneration.incrementAndGet()
        loadMutex.withLock {
            stateMutex.withLock {
                Snapshot.withMutableSnapshot {
                    pics.clear()
                    failedPages.clear()
                    loadedPages.clear()
                    val pages = cachedTotalPages?.coerceAtLeast(1) ?: 1
                    totalPages = pages
                    percentLoad = 1f
                    isPageRequestInFlight = false
                    loadedPages[1] = corrected
                    // В кэш попадает только полный альбом.
                    loadComplete = true
                    pics.addAll(corrected)
                }
            }
        }
    }

    /**
     * Формирует снимок полностью загруженного набора картинок, если все страницы получены без ошибок.
     */
    suspend fun bundleSnapshotOrNull(): LAlbumPicsBundleSnapshot? = stateMutex.withLock {
        if (!loadComplete || pics.isEmpty()) {
            return@withLock null
        }
        LAlbumPicsBundleSnapshot(
            pics = pics.toList(),
            totalPages = totalPages
        )
    }

    private suspend fun appendPage(page: PageLoadResult, pages: Int) {
        val corrected = normalizePictureUrls(page.items)
        stateMutex.withLock {
            // Быстрый путь для последовательной загрузки (стр. 1,2,3,...): дописываем
            // только новую страницу в хвост вместо полной пересборки всего списка
            // (иначе это O(n²) и полная рекомпозиция на каждой странице). Пропуск
            // на месте сбойной страницы хвосту не мешает: порядок задают номера.
            val isTail = loadedPages.keys.all { it < page.page }
            loadedPages[page.page] = corrected

            // Заполнение пропуска / ретрай страницы — пересобираем по порядку.
            // Собираем результат до входа в снапшот, чтобы под ним осталась
            // только публикация.
            val merged = if (isTail) null else {
                val totalLoaded = loadedPages.values.sumOf { it.size }
                val mergedList = ArrayList<PicsDetails>(totalLoaded)
                for (i in 1..pages) {
                    val pagePics = loadedPages[i]
                    if (pagePics != null) {
                        mergedList.addAll(pagePics)
                    }
                }
                mergedList
            }

            val progress = (loadedPages.size.toFloat() / pages.coerceAtLeast(1)).coerceIn(0f, 1f)

            Snapshot.withMutableSnapshot {
                totalPages = pages
                percentLoad = progress
                if (merged == null) {
                    pics.addAll(corrected)
                } else {
                    // Одной атомарной заменой: иначе лента успевает мигнуть пустой.
                    pics.replaceWith(merged)
                }
            }
        }
    }

    /**
     * Повторяет загрузку страниц из [failedPages] и добирает те, до которых
     * загрузка не дошла. Если загрузка ещё идёт, ждёт её конца.
     */
    suspend fun retryFailedPages() = withContext(dispatcher) {
        if (stateMutex.withLock { failedPages.isEmpty() }) return@withContext

        val generation = loadGeneration.get()
        isRetryingFailedPages = true
        try {
            loadMutex.withLock {
                // Пока ждали, началась новая загрузка: она всё запросит сама.
                if (generation == loadGeneration.get()) {
                    loadMissingPages(RepositoryUriConfig.CACHE_RAM, generation)
                }
            }
        } finally {
            isRetryingFailedPages = false
        }
    }

    private suspend fun recordPageIssue(page: Int, error: Throwable?) {
        // Текст для экрана: сырое сообщение при странице защиты несёт HTML-разметку.
        val issue = LAlbumPageLoadIssue(
            page = page,
            message = error.toLUserMessage(),
            htmlChallenge = error.isHtmlChallengeResponse()
        )
        stateMutex.withLock {
            Snapshot.withMutableSnapshot {
                failedPages.removeAll { it.page == page }
                failedPages.add(issue)
                failedPages.sortBy { it.page }
            }
        }
    }

    private suspend fun clearPageIssue(page: Int) {
        stateMutex.withLock {
            failedPages.removeAll { it.page == page }
        }
    }

    private fun PicsDetails.hasAnyMediaUrl(): Boolean {
        return !url_to_original.isNullOrBlank() ||
                !url_to_video.isNullOrBlank() ||
                thumbnails?.any { !it.url.isNullOrBlank() } == true
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        return this as? JsonObject
    }

    private fun JsonObject?.readInt(name: String): Int? {
        return runCatching {
            this?.get(name)?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull
        }.getOrNull()
    }

    private fun Throwable?.isHtmlChallengeResponse(): Boolean {
        val message = this?.message ?: return false
        return message.startsWith(HTML_INSTEAD_OF_JSON_PREFIX)
    }
}

/**
 * Нормализует URL оригиналов и миниатюр для списка картинок [l].
 */
internal fun normalizePictureUrls(l: List<PicsDetails>): List<PicsDetails> {
    if (l.isEmpty()) return emptyList()
    var modified = false
    val result = ArrayList<PicsDetails>(l.size)
    for (item in l) {
        val isAnimated = item.isAnimatedMedia()
        val thumbnailUrl = item.lBestThumbnailImageUrl()

        val origIsAnimatedMedia = item.url_to_original?.let { orig ->
            val clean = orig.substringBefore('?').substringBefore('#')
            clean.endsWith(".gif", ignoreCase = true) || orig.isLVideoFileUrl()
        } == true

        val normalizedOriginal = when {
            origIsAnimatedMedia -> item.url_to_original
            !thumbnailUrl.isNullOrBlank() -> thumbnailUrl
            else -> item.url_to_original
        }

        if (item.is_animated == isAnimated && item.url_to_original == normalizedOriginal) {
            result.add(item)
        } else {
            modified = true
            result.add(
                item.copy(
                    is_animated = isAnimated,
                    url_to_original = normalizedOriginal
                )
            )
        }
    }
    return if (modified) result else l
}

/**
 * Вычисляет корректное число страниц альбома на основе информации о количестве картинок.
 */
internal fun calculateAlbumPages(totalPagesFromInfo: Int?, totalItems: Int?, itemsPerPage: Int?): Int {
    val calculatedPages = if (totalItems != null && itemsPerPage != null && itemsPerPage > 0) {
        ((totalItems + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    } else {
        1
    }
    return maxOf(totalPagesFromInfo ?: calculatedPages, calculatedPages).coerceAtLeast(1)
}
