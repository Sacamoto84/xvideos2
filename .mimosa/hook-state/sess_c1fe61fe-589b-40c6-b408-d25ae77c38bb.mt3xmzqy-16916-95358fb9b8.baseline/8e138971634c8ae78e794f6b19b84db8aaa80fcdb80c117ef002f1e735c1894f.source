package com.client.xvideos.l.ui.screens.screenAlbum

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Audience
import com.client.xvideos.l.model.Genre
import com.client.xvideos.l.model.FilterGenre

internal fun albumListFilterForGenre(genre: Genre): AlbumListFilter {
    return AlbumListFilter(
        genresPlus = listOf(
            FilterGenre(
                id = genre.id,
                title = genre.title,
                slug = genre.url.extractLPathSlug() ?: genre.title.lowercase().replace(" ", "-"),
                description = "",
                uploadingRules = "",
                posterUrl = null,
                actsAsWarning = genre.actsAsWarning,
                actsAsDefault = false,
                representsUncategorized = false,
                url = genre.url,
                parent = null,
                onlyAllowsModel = null,
                onlyContent = null
            )
        )
    )
}

internal fun albumListFilterForAudience(audience: Audience): AlbumListFilter {
    return AlbumListFilter(audienceIds = "+${audience.id}")
}

private fun String.extractLPathSlug(): String? {
    val name = trim('/').substringAfterLast('/').substringBefore('?')
    return name.substringBeforeLast("_").takeIf { it.isNotBlank() }
}

