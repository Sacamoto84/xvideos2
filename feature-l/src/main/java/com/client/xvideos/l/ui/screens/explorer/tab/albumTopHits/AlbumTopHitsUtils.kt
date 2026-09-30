package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.enum.AlbumType
import java.net.URLDecoder

/**
 * Строит [AlbumListFilter] из URL категории top-hits, например:
 * `/albums/list/?album_type=manga&audience_ids=%2B1%2B2&display=date_trending&tagged=%2Bcollared&page=1`
 *
 * Параметры, которых нет в URL, берутся из дефолтов [AlbumListFilter].
 */
internal fun albumListFilterFromTopHitsUrl(url: String): AlbumListFilter {
    val params: Map<String, String> = url
        .substringAfter('?', "")
        .split('&')
        .mapNotNull { pair ->
            val separatorIndex = pair.indexOf('=')
            if (separatorIndex <= 0) return@mapNotNull null
            val name = URLDecoder.decode(pair.substring(0, separatorIndex), "UTF-8")
            val value = URLDecoder.decode(pair.substring(separatorIndex + 1), "UTF-8")
            name to value
        }
        .toMap()

    val default = AlbumListFilter()

    val albumType = params["album_type"]
        ?.let { v -> AlbumType.entries.find { it.value == v } }
        ?: default.album_type

    // "tagged" приходит как "+tag1+tag2" → ["tag1", "tag2"]
    val tagPlus = params["tagged"].orEmpty()
        .split('+')
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return AlbumListFilter(
        display = params["display"] ?: default.display,
        album_type = albumType,
        audienceIds = params["audience_ids"] ?: default.audienceIds,
        languageIds = params["language_ids"] ?: default.languageIds,
        tagPlus = tagPlus
    )
}
