package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.XRubric
import org.jsoup.Jsoup

/** `/c/<slug>-<id>`: id — число после последнего дефиса, slug — всё до него. */
private val RUBRIC_HREF_REGEX = Regex("""^/c/([^/]+)-(\d+)$""")

/**
 * Категории из меню сайта (`#main-cats-sub-list`) в порядке сайта.
 *
 * Языковые ленты, ленты ориентаций и «Все ярлыки» в том же меню пропускаются:
 * берутся только ссылки `/c/…`. Повтор id отбрасывается.
 */
fun parserRubricMenu(html: String): List<XRubric> =
    Jsoup.parse(html)
        .select("#main-cats-sub-list a[href]")
        .mapNotNull { link ->
            val match = RUBRIC_HREF_REGEX.matchEntire(link.attr("href").trim()) ?: return@mapNotNull null
            val title = link.text().trim()
            val id = match.groupValues[2].toIntOrNull()
            if (title.isEmpty() || id == null) null else XRubric(id, match.groupValues[1], title)
        }
        .distinctBy { it.id }
