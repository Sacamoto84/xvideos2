package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun parserItemVideoTags(document: Document): TagsModel {
    val listMain = mutableListOf<TagsMainUploaderPornstar>()
    document.select("li.main-uploader").forEach {
        val href = it.selectFirst("a[href]")?.attr("href")?.trim().orEmpty()
        val name = it.selectFirst("span.name")?.ownText()?.trim()?.takeIf { s -> s.isNotEmpty() }
            ?: it.selectFirst("span.name")?.text()?.trim().orEmpty()
        val count = it.selectFirst("span.count")?.text()?.trim() ?: "0"
        if (name.isNotEmpty()) {
            listMain.add(TagsMainUploaderPornstar(href = href, name = name, count = count))
        }
    }

    val listPornstar = mutableListOf<TagsMainUploaderPornstar>()
    document.select("li.model").forEach {
        val href = it.selectFirst("a[href]")?.attr("href")?.trim().orEmpty()
        val name = it.selectFirst("span.name")?.ownText()?.trim()?.takeIf { s -> s.isNotEmpty() }
            ?: it.selectFirst("span.name")?.text()?.trim().orEmpty()
        val count = it.selectFirst("span.count")?.text()?.trim() ?: "0"
        if (name.isNotEmpty()) {
            listPornstar.add(TagsMainUploaderPornstar(href = href, name = name, count = count))
        }
    }

    val tags = document.select("li a.is-keyword")
        .map { it.text().trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    return TagsModel(listMain, listPornstar, tags)
}

fun parserItemVideoTags(html: String): TagsModel {
    return parserItemVideoTags(Jsoup.parse(html))
}
