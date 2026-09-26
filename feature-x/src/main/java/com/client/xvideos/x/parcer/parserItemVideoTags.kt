package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import java.util.TreeSet
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

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

fun parserItemVideoTags(html: String): TagsModel {
    if (html.isBlank()) return TagsModel.EMPTY
    return parserItemVideoTags(Jsoup.parse(html))
}
