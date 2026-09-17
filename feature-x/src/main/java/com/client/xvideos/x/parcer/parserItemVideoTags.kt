package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

fun parserItemVideoTags(document: Document): TagsModel {
    val listMain = document.select("li.main-uploader").mapNotNull { it.parseUploaderOrModel() }
    val listPornstar = document.select("li.model").mapNotNull { it.parseUploaderOrModel() }

    val tags = document.select("li a.is-keyword")
        .map { it.text().trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sorted()

    return TagsModel(listMain, listPornstar, tags)
}

private fun Element.parseUploaderOrModel(): TagsMainUploaderPornstar? {
    val href = selectFirst("a[href]")?.attr("href")?.trim().orEmpty()
    val name = selectFirst("span.name")?.ownText()?.trim()?.takeIf { it.isNotEmpty() }
        ?: selectFirst("span.name")?.text()?.trim().orEmpty()
    val count = selectFirst("span.count")?.text()?.trim() ?: "0"
    return if (name.isNotEmpty()) {
        TagsMainUploaderPornstar(href = href, name = name, count = count)
    } else {
        null
    }
}

fun parserItemVideoTags(html: String): TagsModel {
    if (html.isBlank()) return TagsModel()
    return parserItemVideoTags(Jsoup.parse(html))
}
