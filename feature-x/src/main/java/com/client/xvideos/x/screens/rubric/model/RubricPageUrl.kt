package com.client.xvideos.x.screens.rubric.model

import com.client.xvideos.x.urlStart

/**
 * Адрес страницы [index] категории (`/c/<фильтры>/<slug>-<id>/N`), как в ссылках
 * пагинации сайта. Страница 0 — первая, `/0` сайт принимает. Префикс
 * ориентации не нужен: ленту задаёт [id].
 */
fun buildRubricPageUrl(id: Int, slug: String, index: Int, filters: XRubricFilters): String =
    "$urlStart/c/${filters.pathPrefix}$slug-$id/$index"
