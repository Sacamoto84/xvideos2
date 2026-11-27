package com.client.xvideos.n.net

import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.model.NTagPage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Парсер данных nhentai: JSON API (v1 и v2) и HTML страниц.
 */
object NParser {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Парсит детальную информацию о галерее из JSON (ответ API v2/v1 или тело скрипта).
     */
    fun parseGalleryJson(jsonString: String): NGallery? = runCatching {
        jsonConfig.parseToJsonElement(jsonString).asObjectOrNull()?.let { parseGallery(it) }
    }.getOrNull()

    private fun parseGallery(root: JsonObject, includeRelated: Boolean = true): NGallery? {
        val id = root["id"].asPrimitiveOrNull()?.longOrNull ?: 0L
        val mediaId = root["media_id"].asPrimitiveOrNull()?.let {
            it.longOrNull ?: it.contentOrNull?.toLongOrNull()
        } ?: 0L
        if (id == 0L || mediaId == 0L) return null

        val titlesObj = root["title"].asObjectOrNull()
        val prettyTitle = titlesObj?.get("pretty").asPrimitiveOrNull()?.contentOrNull.orEmpty()
        val englishTitle = titlesObj?.get("english").asPrimitiveOrNull()?.contentOrNull.orEmpty()
        val japaneseTitle = titlesObj?.get("japanese").asPrimitiveOrNull()?.contentOrNull.orEmpty()
        val title = prettyTitle.ifEmpty { englishTitle.ifEmpty { japaneseTitle } }

        val uploadDate = root["upload_date"].asPrimitiveOrNull()?.longOrNull ?: 0L
        val numPages = root["num_pages"].asPrimitiveOrNull()?.intOrNull ?: 0
        val numFavorites = root["num_favorites"].asPrimitiveOrNull()?.intOrNull ?: 0

        // Поддержка v2 (поля напрямую в root) и v1 (вложенные в images)
        val imagesObj = root["images"].asObjectOrNull()
        val coverObj = root["cover"].asObjectOrNull() ?: imagesObj?.get("cover").asObjectOrNull()
        val thumbObj = root["thumbnail"].asObjectOrNull() ?: imagesObj?.get("thumbnail").asObjectOrNull()
        val pages = parseGalleryPages(root["pages"] ?: imagesObj?.get("pages"))

        val cover = parsePage(coverObj, 1)
        val thumbnail = parsePage(thumbObj, 1)

        val tags = root["tags"].asArrayOrNull().orEmpty()
            .mapNotNull { runCatching { parseTag(it) }.getOrNull() }
            .distinctBy { it.id }

        val relatedArray = if (includeRelated) root["related"].asArrayOrNull() else null
        val relatedGalleries = relatedArray?.mapNotNull(::parseGalleryPreview).orEmpty().distinctBy { it.id }

        return NGallery(
            id = id,
            mediaId = mediaId,
            title = title,
            englishTitle = englishTitle,
            japaneseTitle = japaneseTitle,
            uploadDate = uploadDate,
            numPages = if (numPages > 0) numPages else pages.size,
            numFavorites = numFavorites,
            cover = cover,
            thumbnail = thumbnail,
            tags = tags,
            pages = pages,
            relatedGalleries = relatedGalleries
        )
    }

    private fun parseGalleryPages(element: JsonElement?): List<NPage> {
        val pages = element.asArrayOrNull().orEmpty().mapIndexed { index, page ->
            parsePage(page.asObjectOrNull(), index + 1)
        }
        val numbers = pages.map { it.pageNumber }
        return if (numbers.any { it <= 0 } || numbers.toSet().size != numbers.size) {
            pages.mapIndexed { index, page -> page.copy(pageNumber = index + 1) }
        } else pages
    }

    private fun parsePage(pageObj: JsonObject?, index: Int): NPage {
        if (pageObj == null) return NPage(pageNumber = index)
        val path = pageObj["path"].asPrimitiveOrNull()?.contentOrNull.orEmpty()
        val thumbPath = pageObj["thumbnail"].asPrimitiveOrNull()?.contentOrNull.orEmpty()
        val ext = if (path.isNotEmpty()) {
            path.substringAfterLast('.', "jpg")
        } else {
            val type = pageObj["t"].asPrimitiveOrNull()?.contentOrNull.orEmpty()
            NPage.extFromApiType(type)
        }
        val pageNum = pageObj["number"].asPrimitiveOrNull()?.intOrNull ?: index
        val width = pageObj["width"].asPrimitiveOrNull()?.intOrNull
            ?: pageObj["w"].asPrimitiveOrNull()?.intOrNull ?: 0
        val height = pageObj["height"].asPrimitiveOrNull()?.intOrNull
            ?: pageObj["h"].asPrimitiveOrNull()?.intOrNull ?: 0
        return NPage(
            pageNumber = pageNum,
            ext = ext,
            width = width,
            height = height,
            path = path,
            thumbnailPath = thumbPath
        )
    }

    /**
     * Извлекает JSON галереи из HTML страницы `/g/{id}/`.
     */
    fun parseGalleryHtml(html: String, expectedGalleryId: Long? = null, requirePages: Boolean = false): NGallery? {
        fun NGallery.matches(): Boolean =
            (expectedGalleryId == null || id == expectedGalleryId) && (!requirePages || pages.isNotEmpty())

        val doc: Document = Jsoup.parse(html)
        // 1. Поиск в тегах <script>: window._gallery = JSON.parse("...")
        for (script in doc.getElementsByTag("script")) {
            val scriptContent = script.html()
            val parseIndex = scriptContent.indexOf("JSON.parse(\"")
            if (parseIndex >= 0) {
                val start = parseIndex + "JSON.parse(\"".length
                var end = start
                while (end < scriptContent.length && scriptContent[end] != '"') {
                    end += if (scriptContent[end] == '\\') 2 else 1
                }
                if (end in (start + 1) until scriptContent.length) {
                    val rawJson = scriptContent.substring(start, end)
                    val unescaped = unescapeUnicode(rawJson)
                    val gallery = parseGalleryJson(unescaped)
                    if (gallery?.matches() == true) return gallery
                }
            }
            // 2. В SvelteKit версии данные галереи лежат в JSON внутри data-скрипта
            if (scriptContent.contains("\"media_id\"")) {
                val root = runCatching { jsonConfig.parseToJsonElement(scriptContent) }.getOrNull() as? JsonObject
                val data = root?.get("data") as? JsonObject
                val embedded = (root?.get("gallery") as? JsonObject) ?: (data?.get("gallery") as? JsonObject)
                val gallery = (embedded ?: root)?.let { runCatching { parseGallery(it) }.getOrNull() }
                if (gallery?.matches() == true) return gallery
            }
        }
        return null
    }

    /**
     * Преобразует NGallery в облегчённый NGalleryPreview.
     */
    private fun NGallery.toPreview(): NGalleryPreview = NGalleryPreview(
        id = id,
        mediaId = mediaId,
        title = title,
        thumbnailUrl = thumbnailUrl,
        numPages = numPages,
        language = languages.firstOrNull()?.name.orEmpty()
    )

    private val knownLanguageTags = mapOf(
        12227L to "English",
        6343L to "Japanese",
        29963L to "Chinese"
    )

    private fun detectLanguage(title: String, tagIds: List<Long>?): String {
        tagIds?.forEach { tagId ->
            knownLanguageTags[tagId]?.let { return it }
        }
        val lower = title.lowercase()
        return when {
            lower.contains("[english]") || lower.contains("(english)") -> "English"
            lower.contains("[russian]") || lower.contains("(russian)") -> "Russian"
            lower.contains("[chinese]") || lower.contains("(chinese)") -> "Chinese"
            lower.contains("[japanese]") || lower.contains("(japanese)") -> "Japanese"
            lower.contains("[spanish]") || lower.contains("(spanish)") -> "Spanish"
            lower.contains("[korean]") || lower.contains("(korean)") -> "Korean"
            else -> ""
        }
    }

    /**
     * Парсит отдельный элемент галереи API v2 (GalleryListItem).
     */
    fun parseGalleryListItem(obj: JsonObject): NGalleryPreview? = runCatching {
        val id = obj["id"].asPrimitiveOrNull()?.longOrNull ?: return null
        if (id <= 0L) return null

        val mediaId = obj["media_id"].asPrimitiveOrNull()?.let {
            it.longOrNull ?: it.contentOrNull?.toLongOrNull()
        } ?: 0L

        val englishTitle = obj["english_title"].asPrimitiveOrNull()?.contentOrNull.orEmpty()
        val japaneseTitle = obj["japanese_title"].asPrimitiveOrNull()?.contentOrNull.orEmpty()
        val title = englishTitle.ifEmpty { japaneseTitle }

        val thumbnailPath = obj["thumbnail"].asPrimitiveOrNull()?.contentOrNull.orEmpty()
        val thumbnailUrl = NGalleryPreview.buildThumbnailUrl(thumbnailPath, mediaId)

        val numPages = obj["num_pages"].asPrimitiveOrNull()?.intOrNull ?: 0
        val tagIds = obj["tag_ids"].asArrayOrNull()?.mapNotNull { it.asPrimitiveOrNull()?.longOrNull }
        val language = detectLanguage(title, tagIds)

        NGalleryPreview(
            id = id,
            mediaId = mediaId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            numPages = numPages,
            language = language
        )
    }.getOrNull()

    private fun parseGalleryPreview(element: JsonElement): NGalleryPreview? {
        val obj = element as? JsonObject ?: return null
        return if (obj["title"] is JsonObject || obj["images"] is JsonObject || obj["thumbnail"] is JsonObject) {
            runCatching { parseGallery(obj, includeRelated = false)?.toPreview() }.getOrNull()
        } else {
            parseGalleryListItem(obj)
        }
    }

    /**
     * Парсит список превью галерей из ответа API v2 (/galleries, /popular, /search).
     */
    fun parseGalleryListJson(jsonString: String): List<NGalleryPreview> = runCatching {
        val parsed = jsonConfig.parseToJsonElement(jsonString)
        val itemsArray: JsonArray = when (parsed) {
            is JsonArray -> parsed
            is JsonObject -> {
                parsed["result"].asArrayOrNull()
                    ?: parsed["related"].asArrayOrNull()
                    ?: JsonArray(emptyList())
            }
            else -> JsonArray(emptyList())
        }

        itemsArray.mapNotNull(::parseGalleryPreview).distinctBy { it.id }
    }.getOrDefault(emptyList())

    fun parseFavoriteState(body: String): Boolean = requireNotNull(
        jsonConfig.parseToJsonElement(body).asObjectOrNull()?.get("favorited").asPrimitiveOrNull()?.booleanOrNull
    ) { "Неверный ответ при обновлении избранного" }

    /** Пустой result считается успешным только при корректной структуре ответа v2. */
    fun parseGalleryPageV2(body: String, page: Int): NGalleryPageResult {
        val root = jsonConfig.parseToJsonElement(body).asObjectOrNull()
        val items = root?.get("result").asArrayOrNull()
        val totalPages = root?.get("num_pages").asPrimitiveOrNull()?.intOrNull
        require(items != null && totalPages != null && totalPages >= 0) {
            "Неверный формат списка альбомов"
        }
        return NGalleryPageResult(
            items = items.map { item ->
                requireNotNull(parseGalleryPreview(item)) {
                    "Не удалось прочитать альбом"
                }
            }.distinctBy { it.id },
            page = page,
            totalPages = totalPages.coerceAtLeast(1)
        )
    }

    /**
     * Парсит подсказки из ответа API v2 (/tags/search).
     */
    fun parseTagsJson(jsonString: String): List<NTag> =
        parseTagItems(requireNotNull(jsonConfig.parseToJsonElement(jsonString).asArrayOrNull()) { "Неверный формат списка тегов" })

    fun parseTagPage(jsonString: String): NTagPage {
        val root = jsonConfig.parseToJsonElement(jsonString).asObjectOrNull()
        val items = requireNotNull(root?.get("result").asArrayOrNull()) { "Неверный формат списка тегов" }
        val pages = requireNotNull(root?.get("num_pages").asPrimitiveOrNull()?.intOrNull) { "Нет числа страниц тегов" }
        return NTagPage(parseTagItems(items), pages.coerceAtLeast(1))
    }

    private fun parseTagItems(items: JsonArray): List<NTag> =
        items.map(::parseTag).distinctBy { it.id }

    private fun parseTag(element: JsonElement): NTag {
        val obj = requireNotNull(element.asObjectOrNull()) { "Не удалось прочитать тег" }
        val id = requireNotNull(obj["id"].asPrimitiveOrNull()?.longOrNull) { "Не удалось прочитать тег" }
        val typeEl = obj["type"]
        val nameEl = obj["name"]
        val countEl = obj["count"]
        val urlEl = obj["url"]
        require(
            (typeEl == null || typeEl is JsonPrimitive) &&
                (nameEl == null || nameEl is JsonPrimitive) &&
                (countEl == null || countEl is JsonPrimitive) &&
                (urlEl == null || urlEl is JsonPrimitive)
        ) { "Не удалось прочитать тег" }
        val type = typeEl?.contentOrNull.orEmpty()
        val name = nameEl?.contentOrNull.orEmpty()
        val count = countEl?.intOrNull ?: 0
        val url = urlEl?.contentOrNull.orEmpty()
        return NTag(id = id, type = type, name = name, count = count, url = url)
    }

    /**
     * Парсит список превью галерей из ответа поискового JSON API.
     */
    fun parseSearchJson(jsonString: String): List<NGalleryPreview> = parseGalleryListJson(jsonString)

    /**
     * Парсит страницу галерей с общим количеством доступных страниц из ответа JSON API v2.
     */
    fun parseGalleryPageJson(jsonString: String, defaultPage: Int = 1): NGalleryPageResult = runCatching {
        val galleries = parseGalleryListJson(jsonString)
        val root = jsonConfig.parseToJsonElement(jsonString)
        val numPages = if (root is JsonObject) {
            root["num_pages"].asPrimitiveOrNull()?.intOrNull
                ?: root["total_pages"].asPrimitiveOrNull()?.intOrNull
                ?: 1
        } else {
            1
        }
        NGalleryPageResult(
            items = galleries,
            page = defaultPage,
            totalPages = numPages.coerceAtLeast(1)
        )
    }.getOrDefault(NGalleryPageResult(page = defaultPage, totalPages = 1))

    /**
     * Парсит превью галерей из HTML-страницы со списком (поиск или главная).
     */
    fun parseSearchHtml(html: String): List<NGalleryPreview> {
        val doc: Document = Jsoup.parse(html)
        val galleriesElements = doc.getElementsByClass("gallery")
        return galleriesElements.mapNotNull(::parseGalleryElement).distinctBy { it.id }
    }

    /**
     * Парсит превью галерей и общее количество страниц из HTML-страницы со списком.
     */
    fun parseSearchHtmlPage(html: String, defaultPage: Int = 1): NGalleryPageResult {
        val galleries = parseSearchHtml(html)
        val totalPages = parseHtmlTotalPages(html).coerceAtLeast(defaultPage.coerceAtLeast(1))
        return NGalleryPageResult(
            items = galleries,
            page = defaultPage,
            totalPages = totalPages
        )
    }

    private fun parseHtmlTotalPages(html: String): Int = runCatching {
        val doc: Document = Jsoup.parse(html)
        val lastLink = doc.select("section.pagination a.last").firstOrNull()
        if (lastLink != null) {
            val href = lastLink.attr("href")
            val pageNum = href.substringAfter("page=").substringBefore('&').toIntOrNull()
            if (pageNum != null && pageNum > 0) return@runCatching pageNum
        }
        val pageLinks = doc.select("section.pagination a.page")
        val maxFromLinks = pageLinks.mapNotNull { it.text().trim().toIntOrNull() }.maxOrNull()
        maxFromLinks ?: 1
    }.getOrDefault(1)

    private fun parseGalleryElement(element: Element): NGalleryPreview? = runCatching {
        val aTag = element.getElementsByTag("a").firstOrNull() ?: return null
        val href = aTag.attr("href") // /g/123456/
        val id = href.trim('/').substringAfterLast('/').toLongOrNull() ?: return null

        val img = element.getElementsByTag("img").firstOrNull() ?: return null
        val src = if (img.hasAttr("data-src")) img.attr("data-src") else img.attr("src")
        // URL вида: https://t.nhentai.net/galleries/987654/thumb.jpg
        val mediaId = src.substringAfter("/galleries/").substringBefore('/').toLongOrNull() ?: 0L

        val caption = element.getElementsByClass("caption").firstOrNull()?.text().orEmpty()

        NGalleryPreview(
            id = id,
            mediaId = mediaId,
            title = caption,
            thumbnailUrl = src,
            numPages = 0,
            language = ""
        )
    }.getOrNull()

    private fun unescapeUnicode(input: String): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                when (input[i + 1]) {
                    'u' -> {
                        if (i + 5 < input.length) {
                            val hex = input.substring(i + 2, i + 6)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 6
                                continue
                            }
                        }
                        sb.append(c)
                        i++
                    }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    else -> { sb.append(c); i++ }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray

    private fun JsonElement?.asPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive
}
