package com.client.xvideos.l.ui.screens.explorer.tab.albumSearch

import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId

internal fun createAlbumSearchFilter(section: Landing_page_albumSection, query: String): AlbumListFilter {
    val albumType = when (section.title) {
        "Manga" -> AlbumType.Manga
        "Picture Sets" -> AlbumType.Pictures
        else -> AlbumType.Pictures
    }

    return AlbumListFilter(
        display = "search_score",
        album_type = albumType,
        content_id = ContentId.All,
        searchQuery = query.trim()
    )
}

internal fun hasNoSearchResults(result: Landing_page_albumType?): Boolean {
    if (result == null) return false
    val sections = result.sections
    return sections.isNullOrEmpty() || sections.all { it.items.isEmpty() }
}
