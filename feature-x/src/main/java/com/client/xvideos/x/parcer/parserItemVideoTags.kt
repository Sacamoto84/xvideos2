package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun parserItemVideoTags(html: String): TagsModel {
    // Парсим документ один раз (раньше Jsoup.parse вызывался дважды, а часть результата не использовалась).
    val document: Document = Jsoup.parse(html)

    val listMain = mutableListOf<TagsMainUploaderPornstar>()
    document.select("li.main-uploader").forEach {
        val href = it.selectFirst("a[href]")?.attr("href") ?: "Unknown"
        val name = it.selectFirst("span.name")?.ownText() ?: "Unknown"
        val count = it.selectFirst("span.count")?.text() ?: "0"
        listMain.add(TagsMainUploaderPornstar(href = href, name = name, count = count))
    }

    val listPornstar = mutableListOf<TagsMainUploaderPornstar>()
    document.select("li.model").forEach {
        val href = it.selectFirst("a[href]")?.attr("href") ?: "Unknown"
        val name = it.selectFirst("span.name")?.ownText() ?: "Unknown"
        val count = it.selectFirst("span.count")?.text() ?: "0"
        listPornstar.add(TagsMainUploaderPornstar(href = href, name = name, count = count))
    }

    val tags = document.select("li a.is-keyword").map { it.text() }

    return TagsModel(listMain, listPornstar, tags)
}
