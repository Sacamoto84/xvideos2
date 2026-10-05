package com.client.xvideos.l.ui.screens.albumLandingTag

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.albumTypeOrNull
import com.client.xvideos.l.model.contentIdOrNull
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.model.lAlbumListLinkParams

/**
 * Фильтр «показать все» для раздела [section] страницы тега [tag].
 *
 * Тип альбома и контента задаёт ссылка раздела. Заголовок — запасной путь для
 * ответа без параметров в ссылке: это текст для человека, сайт может его
 * переименовать, и тогда сравнение с ним открыло бы не тот набор альбомов.
 */
internal fun createAlbumTagFilter(section: Landing_page_albumSection, tag: String): AlbumListFilter {
    val link = lAlbumListLinkParams(section.url)
    val title = section.title

    val albumType = link.albumTypeOrNull() ?: when (title) {
        "Hentai Manga" -> AlbumType.Manga
        else -> AlbumType.Pictures
    }

    val contentId = link.contentIdOrNull() ?: when (title) {
        "Hentai Pictures" -> ContentId.Hentai
        "Porn Pictures" -> ContentId.RealPeople
        else -> ContentId.All
    }

    return AlbumListFilter(
        display = "date_trending",
        album_type = albumType,
        content_id = contentId,
        tagPlus = listOf(tag)
    )
}
