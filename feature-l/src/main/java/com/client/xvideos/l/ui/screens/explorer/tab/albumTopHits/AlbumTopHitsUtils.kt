package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.albumTypeOrNull
import com.client.xvideos.l.model.lAlbumListLinkParams

/**
 * Строит [AlbumListFilter] из URL категории top-hits, например:
 * `/albums/list/?album_type=manga&audience_ids=%2B1%2B2&display=date_trending&tagged=%2Bcollared&page=1`
 *
 * Параметры, которых нет в URL, берутся из дефолтов [AlbumListFilter].
 */
internal fun albumListFilterFromTopHitsUrl(url: String): AlbumListFilter {
    val params = lAlbumListLinkParams(url)

    val default = AlbumListFilter()

    val albumType = params.albumTypeOrNull() ?: default.album_type

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
