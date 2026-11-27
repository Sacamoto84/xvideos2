package com.client.xvideos.n.ui.model

import com.client.xvideos.n.model.NAlbumFilter

internal val nAlbumFilterLanguages = listOf(
    "" to "Все языки",
    "english" to "English",
    "russian" to "Русский",
    "japanese" to "日本語",
)

internal val NAlbumFilter.hasValidPageRange: Boolean
    get() = minPages != 0 && maxPages != 0 && (minPages ?: 1) <= (maxPages ?: Int.MAX_VALUE)
