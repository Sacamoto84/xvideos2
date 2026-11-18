package com.client.xvideos.n.model

/**
 * Параметры фильтрации каталога манги (язык и тип сортировки).
 */
data class NAlbumFilter(
    val language: String = "",
    val sort: NSortType = NSortType.RECENT
) {
    val isDefault: Boolean
        get() = language.isEmpty() && sort == NSortType.RECENT

    val isFiltered: Boolean
        get() = !isDefault

    val searchQuery: String
        get() = when {
            language.isNotEmpty() -> "language:$language"
            else -> ""
        }

    companion object {
        val DEFAULT = NAlbumFilter()
    }
}
