package com.client.xvideos.n.net

import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPageResult
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.model.NTagPage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val root = jsonConfig.parseToJsonElement(jsonString).jsonObject

        val id = root["id"]?.jsonPrimitive?.longOrNull ?: 0L
        val mediaId = root["media_id"]?.jsonPrimitive?.let {
            it.longOrNull ?: it.contentOrNull?.toLongOrNull()
        } ?: 0L
        if (id == 0L || mediaId == 0L) return null

        val titlesObj = root["title"]?.jsonObject
        val prettyTitle = titlesObj?.get("pretty")?.jsonPrimitive?.contentOrNull.orEmpty()
        val englishTitle = titlesObj?.get("english")?.jsonPrimitive?.contentOrNull.orEmpty()
        val japaneseTitle = titlesObj?.get("japanese")?.jsonPrimitive?.contentOrNull.orEmpty()
        val title = prettyTitle.ifEmpty { englishTitle.ifEmpty { japaneseTitle } }

        val uploadDate = root["upload_date"]?.jsonPrimitive?.longOrNull ?: 0L
        val numPages = root["num_pages"]?.jsonPrimitive?.intOrNull ?: 0
        val numFavorites = root["num_favorites"]?.jsonPrimitive?.intOrNull ?: 0

        // Поддержка v2 (поля напрямую в root) и v1 (вложенные в images)
        val imagesObj = root["images"]?.jsonObject
        val coverObj = root["cover"]?.jsonObject ?: imagesObj?.get("cover")?.jsonObject
        val thumbObj = root["thumbnail"]?.jsonObject ?: imagesObj?.get("thumbnail")?.jsonObject
        val pagesArray = root["pages"]?.jsonArray ?: imagesObj?.get("pages")?.jsonArray ?: JsonArray(emptyList())

        val cover = parsePage(coverObj, 1)
        val thumbnail = parsePage(thumbObj, 1)

        val pages = pagesArray.mapIndexed { index, pageElement ->
            parsePage(pageElement.jsonObject, index + 1)
        }

        val tagsArray = root["tags"]?.jsonArray ?: JsonArray(emptyList())
        val tags = tagsArray.mapNotNull { tagElement ->
            val tagObj = tagElement.jsonObject
            val tagId = tagObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val type = tagObj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val name = tagObj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val count = tagObj["count"]?.jsonPrimitive?.intOrNull ?: 0
            val url = tagObj["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
            NTag(id = tagId, type = type, name = name, count = count, url = url)
        }

        val relatedArray = root["related"]?.jsonArray
        val relatedGalleries = relatedArray?.mapNotNull { element ->
            val elObj = element.jsonObject
            parseGalleryListItem(elObj) ?: parseGalleryJson(element.toString())?.toPreview()
        }.orEmpty()

        NGallery(
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
    }.getOrNull()

    private fun parsePage(pageObj: JsonObject?, index: Int): NPage {
        if (pageObj == null) return NPage(pageNumber = index)
        val path = pageObj["path"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val thumbPath = pageObj["thumbnail"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val ext = if (path.isNotEmpty()) {
            path.substringAfterLast('.', "jpg")
        } else {
            val type = pageObj["t"]?.jsonPrimitive?.contentOrNull.orEmpty()
            NPage.extFromApiType(type)
        }
        val pageNum = pageObj["number"]?.jsonPrimitive?.intOrNull ?: index
        val width = pageObj["width"]?.jsonPrimitive?.intOrNull
            ?: pageObj["w"]?.jsonPrimitive?.intOrNull ?: 0
        val height = pageObj["height"]?.jsonPrimitive?.intOrNull
            ?: pageObj["h"]?.jsonPrimitive?.intOrNull ?: 0
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
    fun parseGalleryHtml(html: String): NGallery? {
        val doc: Document = Jsoup.parse(html)
        // 1. Поиск в тегах <script>: window._gallery = JSON.parse("...")
        for (script in doc.getElementsByTag("script")) {
            val scriptContent = script.html()
            val parseIndex = scriptContent.indexOf("JSON.parse(\"")
            if (parseIndex >= 0) {
                val start = parseIndex + "JSON.parse(\"".length
                val end = scriptContent.indexOf("\")", start)
                if (end > start) {
                    val rawJson = scriptContent.substring(start, end)
                    val unescaped = unescapeUnicode(rawJson)
                    val gallery = parseGalleryJson(unescaped)
                    if (gallery != null) return gallery
                }
            }
            // 2. В SvelteKit версии данные галереи лежат в JSON внутри data-скрипта
            if (scriptContent.contains("\"media_id\":")) {
                val galleryIndex = scriptContent.indexOf("\"gallery\":")
                if (galleryIndex >= 0) {
                    val candidate = scriptContent.substring(galleryIndex + "\"gallery\":".length)
                    val gallery = parseGalleryJson(candidate)
                    if (gallery != null) return gallery
                }
                val direct = parseGalleryJson(scriptContent)
                if (direct != null) return direct
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
        val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return null
        if (id <= 0L) return null

        val mediaId = obj["media_id"]?.jsonPrimitive?.let {
            it.longOrNull ?: it.contentOrNull?.toLongOrNull()
        } ?: 0L

        val englishTitle = obj["english_title"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val japaneseTitle = obj["japanese_title"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val title = englishTitle.ifEmpty { japaneseTitle }

        val thumbnailPath = obj["thumbnail"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val thumbnailUrl = NGalleryPreview.buildThumbnailUrl(thumbnailPath, mediaId)

        val numPages = obj["num_pages"]?.jsonPrimitive?.intOrNull ?: 0
        val tagIds = obj["tag_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull }
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

    /**
     * Парсит список превью галерей из ответа API v2 (/galleries, /popular, /search).
     */
    fun parseGalleryListJson(jsonString: String): List<NGalleryPreview> = runCatching {
        val parsed = jsonConfig.parseToJsonElement(jsonString)
        val itemsArray: JsonArray = when (parsed) {
            is JsonArray -> parsed
            is JsonObject -> {
                parsed["result"]?.jsonArray
                    ?: parsed["related"]?.jsonArray
                    ?: JsonArray(emptyList())
            }
            else -> JsonArray(emptyList())
        }

        itemsArray.mapNotNull { element ->
            val obj = element.jsonObject
            parseGalleryListItem(obj) ?: parseGalleryJson(element.toString())?.toPreview()
        }
    }.getOrDefault(emptyList())

    fun parseFavoriteState(body: String): Boolean = requireNotNull(
        jsonConfig.parseToJsonElement(body).jsonObject["favorited"]?.jsonPrimitive?.booleanOrNull
    ) { "Неверный ответ при обновлении избранного" }

    /** Пустой result считается успешным только при корректной структуре ответа v2. */
    fun parseGalleryPageV2(body: String, page: Int): NGalleryPageResult {
        val root = jsonConfig.parseToJsonElement(body) as? JsonObject
        val items = root?.get("result") as? JsonArray
        val totalPages = root?.get("num_pages")?.jsonPrimitive?.intOrNull
        require(items != null && totalPages != null && totalPages >= 0) {
            "Неверный формат списка альбомов"
        }
        return NGalleryPageResult(
            items = items.map { item ->
                requireNotNull((item as? JsonObject)?.let(::parseGalleryListItem)) {
                    "Не удалось прочитать альбом"
                }
            },
            page = page,
            totalPages = totalPages.coerceAtLeast(1)
        )
    }

    /**
     * Парсит подсказки из ответа API v2 (/tags/search).
     */
    fun parseTagsJson(jsonString: String): List<NTag> =
        parseTagItems(jsonConfig.parseToJsonElement(jsonString).jsonArray)

    fun parseTagPage(jsonString: String): NTagPage {
        val root = jsonConfig.parseToJsonElement(jsonString).jsonObject
        val items = requireNotNull(root["result"] as? JsonArray) { "Неверный формат списка тегов" }
        val pages = requireNotNull(root["num_pages"]?.jsonPrimitive?.intOrNull) { "Нет числа страниц тегов" }
        return NTagPage(parseTagItems(items), pages.coerceAtLeast(1))
    }

    private fun parseTagItems(items: JsonArray): List<NTag> =
        items.map { element ->
            val obj = element.jsonObject
            val id = requireNotNull(obj["id"]?.jsonPrimitive?.longOrNull) { "Не удалось прочитать тег" }
            val type = obj["type"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val count = obj["count"]?.jsonPrimitive?.intOrNull ?: 0
            val url = obj["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
            NTag(id = id, type = type, name = name, count = count, url = url)
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
            root["num_pages"]?.jsonPrimitive?.intOrNull
                ?: root["total_pages"]?.jsonPrimitive?.intOrNull
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
        return galleriesElements.mapNotNull { element -> parseGalleryElement(element) }
    }

    /**
     * Парсит превью галерей и общее количество страниц из HTML-страницы со списком.
     */
    fun parseSearchHtmlPage(html: String, defaultPage: Int = 1): NGalleryPageResult {
        val galleries = parseSearchHtml(html)
        val totalPages = parseHtmlTotalPages(html).coerceAtLeast(1)
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
}
