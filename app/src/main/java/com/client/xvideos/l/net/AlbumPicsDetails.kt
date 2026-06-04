package com.client.xvideos.l.net

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lBestThumbnailImageUrl
import com.client.xvideos.l.net.graphQl.GraphQlRequest
import com.client.xvideos.l.repository.LRepositoryProtectionUiState
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

data class LAlbumPageLoadIssue(
    val page: Int,
    val message: String,
    val htmlChallenge: Boolean,
    val failedAtMs: Long = System.currentTimeMillis()
)

data class LAlbumPicsBundleSnapshot(
    val pics: List<PicsDetails>,
    val totalPages: Int?
)

/**
 * Информация о картинках по id альбома
 */
class AlbumPicsDetails(
    val id: Int,
    val repository: Repository
) {

    private companion object {
        val gson = Gson()
        const val PAGE_REQUEST_DELAY_MS = 250L
    }

    val pics = mutableStateListOf<PicsDetails>()

    var totalPages: Int? = null

    var percentLoad by mutableFloatStateOf(0f)

    var isPageRequestInFlight by mutableStateOf(false)
        private set

    var isRetryingFailedPages by mutableStateOf(false)
        private set

    val failedPages = mutableStateListOf<LAlbumPageLoadIssue>()

    val protectionUiState: LRepositoryProtectionUiState
        get() = repository.protectionUiState

    private val loadedPages = mutableMapOf<Int, List<PicsDetails>>()

    private data class PageLoadResult(
        val page: Int,
        val totalPages: Int,
        val items: List<PicsDetails>
    )

    private suspend fun loadPage(
        page: Int,
        config: RepositoryUriConfig = RepositoryUriConfig.CACHE_RAM
    ): Result<PageLoadResult> {
        withContext(Dispatchers.Main) {
            isPageRequestInFlight = true
        }

        return try {
            openPage(page, config)
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                isPageRequestInFlight = false
            }
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
            Timber.w(pageError, "!!! AlbumPicsDetails $id page $page HTML challenge response")
            return Result.failure(pageError ?: IllegalStateException("Server returned HTML instead of JSON"))
        }

        Timber.w(pageError, "!!! AlbumPicsDetails $id page $page load error")
        return pageResponse
    }

    private fun parsePage(page: Int, response: String): PageLoadResult {
        val list = mutableListOf<PicsDetails>()

        val json = JsonParser.parseString(response).asJsonObject
        val get = json["data"]
            ?.asJsonObjectOrNull()
            ?.get("picture")
            ?.asJsonObjectOrNull()
            ?.get("list")
            ?.asJsonObjectOrNull()
            ?: error("AlbumPicsDetails response missing data.picture.list")

        get["errors"]
            ?.takeIf { !it.isJsonNull }
            ?.let { error("AlbumPicsDetails response errors: ${it.toString().take(300)}") }

        val info = get["info"]?.asJsonObjectOrNull()
        val pages = info.readInt("total_pages")?.coerceAtLeast(1) ?: 1

        val itemsArray = get["items"]?.takeIf { it.isJsonArray }?.asJsonArray
            ?: error("AlbumPicsDetails response missing data.picture.list.items")

        itemsArray.forEachIndexed { index, element ->
            runCatching {
                gson.fromJson(element, PicsDetails::class.java)
            }.onSuccess { pic ->
                if (pic != null && pic.hasAnyMediaUrl()) {
                    list.add(pic)
                } else {
                    Timber.w("!!! AlbumPicsDetails $id page $page item $index has no media urls")
                }
            }.onFailure {
                Timber.w(it, "!!! AlbumPicsDetails $id page $page item $index parse error")
            }
        }

        return PageLoadResult(page, pages, list)
    }

    suspend fun contentUrls(
        pageCacheConfig: RepositoryUriConfig = RepositoryUriConfig.CACHE_RAM
    ) = withContext(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            pics.clear()
            failedPages.clear()
            loadedPages.clear()
            totalPages = null
            percentLoad = 0f
            isPageRequestInFlight = false
            isRetryingFailedPages = false
        }

        val firstPage = loadPage(1, pageCacheConfig).getOrElse {
            Timber.w(it, "!!! AlbumPicsDetails $id page 1 error")
            recordPageIssue(1, it)
            withContext(Dispatchers.Main) {
                percentLoad = 1f
            }
            return@withContext
        }

        val pages = firstPage.totalPages
        appendPage(firstPage, pages)

        for (page in 2..pages) {
            val pageResult = loadPage(page, pageCacheConfig).getOrElse {
                Timber.w(it, "!!! AlbumPicsDetails $id page $page error")
                recordPageIssue(page, it)
                PageLoadResult(page, pages, emptyList())
            }
            appendPage(pageResult, pages)
            delay(PAGE_REQUEST_DELAY_MS)
        }

        withContext(Dispatchers.Main) {
            percentLoad = 1f
        }
    }

    suspend fun restoreFromBundleCache(
        items: List<PicsDetails>,
        cachedTotalPages: Int?
    ) = withContext(Dispatchers.Default) {
        val corrected = normalizePictureUrls(items)
        withContext(Dispatchers.Main) {
            pics.clear()
            failedPages.clear()
            loadedPages.clear()
            val pages = cachedTotalPages?.coerceAtLeast(1) ?: 1
            totalPages = pages
            percentLoad = 1f
            isPageRequestInFlight = false
            isRetryingFailedPages = false
            loadedPages[1] = corrected
            pics.addAll(corrected)
        }
    }

    suspend fun bundleSnapshotOrNull(): LAlbumPicsBundleSnapshot? = withContext(Dispatchers.Main) {
        if (failedPages.isNotEmpty() || percentLoad < 1f || pics.isEmpty()) {
            return@withContext null
        }
        LAlbumPicsBundleSnapshot(
            pics = pics.toList(),
            totalPages = totalPages
        )
    }

    private suspend fun appendPage(page: PageLoadResult, pages: Int) {
        val corrected = normalizePictureUrls(page.items)
        withContext(Dispatchers.Main) {
            totalPages = pages
            percentLoad = page.page.toFloat() / pages
            // Быстрый путь для последовательной загрузки (стр. 1,2,3,...): дописываем
            // только новую страницу в хвост вместо полной пересборки всего списка
            // (иначе это O(n²) и полная рекомпозиция на каждой странице).
            val isContiguousTail = !loadedPages.containsKey(page.page) &&
                    page.page == loadedPages.size + 1 &&
                    (1..loadedPages.size).all { loadedPages.containsKey(it) }
            loadedPages[page.page] = corrected
            if (isContiguousTail) {
                pics.addAll(corrected)
            } else {
                // Заполнение пропуска / ретрай страницы — пересобираем по порядку.
                pics.clear()
                for (loadedPage in 1..pages) {
                    pics.addAll(loadedPages[loadedPage].orEmpty())
                }
            }
        }
    }

    suspend fun retryFailedPages() = withContext(Dispatchers.Default) {
        val pagesToRetry = withContext(Dispatchers.Main) {
            failedPages.map { it.page }.distinct().sorted()
        }
        if (pagesToRetry.isEmpty()) return@withContext

        withContext(Dispatchers.Main) {
            isRetryingFailedPages = true
        }

        try {
            val knownTotalPages = totalPages ?: pagesToRetry.maxOrNull() ?: 1
            pagesToRetry.forEach { page ->
                val pageResult = loadPage(page).getOrElse {
                    Timber.w(it, "!!! AlbumPicsDetails $id page $page retry error")
                    recordPageIssue(page, it)
                    delay(PAGE_REQUEST_DELAY_MS)
                    return@forEach
                }
                appendPage(pageResult, pageResult.totalPages.coerceAtLeast(knownTotalPages))
                delay(PAGE_REQUEST_DELAY_MS)
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                percentLoad = 1f
                isRetryingFailedPages = false
            }
        }
    }

    private suspend fun recordPageIssue(page: Int, error: Throwable?) {
        val message = error?.message ?: "Unknown L album page error"
        val issue = LAlbumPageLoadIssue(
            page = page,
            message = message,
            htmlChallenge = error.isHtmlChallengeResponse()
        )
        withContext(Dispatchers.Main) {
            failedPages.removeAll { it.page == page }
            failedPages.add(issue)
            failedPages.sortBy { it.page }
        }
    }

    private suspend fun clearPageIssue(page: Int) {
        withContext(Dispatchers.Main) {
            failedPages.removeAll { it.page == page }
        }
    }

    private fun PicsDetails.hasAnyMediaUrl(): Boolean {
        return !url_to_original.isNullOrBlank() ||
                !url_to_video.isNullOrBlank() ||
                thumbnails?.any { !it.url.isNullOrBlank() } == true
    }

    private fun com.google.gson.JsonElement.asJsonObjectOrNull(): JsonObject? {
        return takeIf { it.isJsonObject }?.asJsonObject
    }

    private fun JsonObject?.readInt(name: String): Int? {
        return runCatching {
            this?.get(name)?.takeIf { !it.isJsonNull }?.asInt
        }.getOrNull()
    }

    private fun Throwable?.isHtmlChallengeResponse(): Boolean {
        val message = this?.message ?: return false
        return message.startsWith("Server returned HTML instead of JSON")
    }

    private fun normalizePictureUrls(l: List<PicsDetails>): List<PicsDetails> {
        return l.map { item ->
            val thumbnailUrl = item.lBestThumbnailImageUrl()
            if (!thumbnailUrl.isNullOrBlank()) {
                item.copy(url_to_original = thumbnailUrl)
            } else {
                item
            }
        }
    }

}
