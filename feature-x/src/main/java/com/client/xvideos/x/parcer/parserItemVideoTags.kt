package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import java.util.TreeSet
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Извлекает блок тегов, авторов и участвующих моделей из DOM страницы видеоролика.
 *
 * @param document Разобранный HTML-документ Jsoup.
 * @return Объект [TagsModel] с упорядоченными тегами и списками авторов/моделей.
 */
fun parserItemVideoTags(document: Document): TagsModel {
    val mainElements = document.select("li.main-uploader")
    val modelElements = document.select("li.model")
    val tagElements = document.select("li a.is-keyword")

    if (mainElements.isEmpty() && modelElements.isEmpty() && tagElements.isEmpty()) {
        return TagsModel.EMPTY
    }

    val listMain = ArrayList<TagsMainUploaderPornstar>(mainElements.size)
    for (el in mainElements) {
        el.parseUploaderOrModel()?.let { listMain.add(it) }
    }

    val listPornstar = ArrayList<TagsMainUploaderPornstar>(modelElements.size)
    for (el in modelElements) {
        el.parseUploaderOrModel()?.let { listPornstar.add(it) }
    }

    val tagsSet = TreeSet<String>()
    for (el in tagElements) {
        val text = el.text().trim()
        if (text.isNotEmpty()) {
            tagsSet.add(text)
        }
    }

    if (listMain.isEmpty() && listPornstar.isEmpty() && tagsSet.isEmpty()) {
        return TagsModel.EMPTY
    }

    val finalTags = if (tagsSet.isEmpty()) emptyList() else tagsSet.toList()
    return TagsModel(listMain, listPornstar, finalTags)
}

/**
 * Разбирает HTML-элемент `<li>` автора или модели, извлекая имя, ссылку и счетчик.
 */
private fun Element.parseUploaderOrModel(): TagsMainUploaderPornstar? {
    val href = selectFirst("a[href]")?.attr("href")?.trim().orEmpty()
    val nameSpan = selectFirst("span.name")
    val name = nameSpan?.ownText()?.trim()?.takeIf { it.isNotEmpty() }
        ?: nameSpan?.text()?.trim().orEmpty()
    val count = selectFirst("span.count")?.text()?.trim() ?: "0"
    return if (name.isNotEmpty()) {
        TagsMainUploaderPornstar(href = href, name = name, count = count)
    } else {
        null
    }
}

/**
 * Извлекает блок тегов, авторов и участвующих моделей из сырой HTML строки страницы видеоролика.
 *
 * @param html Текст HTML страницы.
 * @return [TagsModel] с найденными элементами.
 */
fun parserItemVideoTags(html: String): TagsModel {
    if (html.isBlank()) return TagsModel.EMPTY
    return parserItemVideoTags(Jsoup.parse(html))
}

/**
 * Безопасно разбирает HTML-строку страницы видеоролика или возвращает [TagsModel.EMPTY].
 */
fun parserItemVideoTagsOrEmpty(html: String?): TagsModel =
    if (html.isNullOrBlank()) TagsModel.EMPTY else parserItemVideoTags(html)

/**
 * Быстрая проверка наличия блоков тегов в HTML без полного парсинга DOM.
 */
fun hasVideoTags(html: String?): Boolean =
    if (html.isNullOrBlank()) false else html.contains("main-uploader") || html.contains("is-keyword") || html.contains("class=\"model\"")

/**
 * Быстро извлекает только список текстовых тегов-ключевых слов из разметки страницы видео.
 */
fun parseKeywordsOnly(html: String): List<String> =
    parserItemVideoTags(html).tags

/**
 * Извлекает только список участвующих порнозвезд/моделей.
 */
fun parsePornstarsOnly(html: String): List<TagsMainUploaderPornstar> =
    parserItemVideoTags(html).pornstars

/**
 * Извлекает только список основных загрузчиков/каналов видео.
 */
fun parseUploadersOnly(html: String): List<TagsMainUploaderPornstar> =
    parserItemVideoTags(html).mainUploader


