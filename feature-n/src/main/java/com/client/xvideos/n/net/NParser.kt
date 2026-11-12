package com.client.xvideos.n.net

import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPreview
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NTag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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
 * Парсер данных nhentai: JSON API и HTML страниц.
 */
object NParser {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Парсит детальную информацию о галерее из JSON (ответ API или тело скрипта).
     */
    fun parseGalleryJson(jsonString: String): NGallery? = runCatching {
        val root = jsonConfig.parseToJsonElement(jsonString).jsonObject

        val id = root["id"]?.jsonPrimitive?.longOrNull ?: 0L
        val mediaId = root["media_id"]?.jsonPrimitive?.longOrNull ?: 0L
        if (id == 0L || mediaId == 0L) return null

        val titlesObj = root["title"]?.jsonObject
        val prettyTitle = titlesObj?.get("pretty")?.jsonPrimitive?.contentOrNull.orEmpty()
        val englishTitle = titlesObj?.get("english")?.jsonPrimitive?.contentOrNull.orEmpty()
        val japaneseTitle = titlesObj?.get("japanese")?.jsonPrimitive?.contentOrNull.orEmpty()
        val title = prettyTitle.ifEmpty { englishTitle.ifEmpty { japaneseTitle } }

        val uploadDate = root["upload_date"]?.jsonPrimitive?.longOrNull ?: 0L
        val numPages = root["num_pages"]?.jsonPrimitive?.intOrNull ?: 0
        val numFavorites = root["num_favorites"]?.jsonPrimitive?.intOrNull ?: 0

        val imagesObj = root["images"]?.jsonObject
        val cover = parsePage(imagesObj?.get("cover")?.jsonObject, 1)
        val thumbnail = parsePage(imagesObj?.get("thumbnail")?.jsonObject, 1)

        val pagesArray = imagesObj?.get("pages")?.jsonArray ?: JsonArray(emptyList())
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
            pages = pages
        )
    }.getOrNull()

    private fun parsePage(pageObj: JsonObject?, index: Int): NPage {
        if (pageObj == null) return NPage(pageNumber = index)
        val type = pageObj["t"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val ext = NPage.extFromApiType(type)
        val width = pageObj["w"]?.jsonPrimitive?.intOrNull ?: 0
        val height = pageObj["h"]?.jsonPrimitive?.intOrNull ?: 0
        return NPage(pageNumber = index, ext = ext, width = width, height = height)
    }

    /**
     * Извлекает JSON галереи из HTML страницы `/g/{id}/`.
     */
    fun parseGalleryHtml(html: String): NGallery? {
        val doc: Document = Jsoup.parse(html)
        // Поиск в тегах <script>: window._gallery = JSON.parse("...")
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
        }
        return null
    }

    /**
     * Парсит список превью галерей из ответа поискового JSON API.
     */
    fun parseSearchJson(jsonString: String): List<NGalleryPreview> = runCatching {
        val root = jsonConfig.parseToJsonElement(jsonString).jsonObject
        val resultsArray = root["result"]?.jsonArray ?: return emptyList()
        resultsArray.mapNotNull { element ->
            val gallery = parseGalleryJson(element.toString()) ?: return@mapNotNull null
            NGalleryPreview(
                id = gallery.id,
                mediaId = gallery.mediaId,
                title = gallery.title,
                thumbnailUrl = gallery.thumbnailUrl,
                numPages = gallery.numPages,
                language = gallery.languages.firstOrNull()?.name.orEmpty()
            )
        }
    }.getOrDefault(emptyList())

    /**
     * Парсит превью галерей из HTML-страницы со списком (поиск или главная).
     */
    fun parseSearchHtml(html: String): List<NGalleryPreview> {
        val doc: Document = Jsoup.parse(html)
        val galleriesElements = doc.getElementsByClass("gallery")
        return galleriesElements.mapNotNull { element -> parseGalleryElement(element) }
    }

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
