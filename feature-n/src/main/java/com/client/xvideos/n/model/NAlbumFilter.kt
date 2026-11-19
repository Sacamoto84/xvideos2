package com.client.xvideos.n.model

data class NAlbumFilter(
    val language: String = "",
    val sort: NSortType = NSortType.RECENT,
    val tags: List<NTagCondition> = emptyList(),
    val minPages: Int? = null,
    val maxPages: Int? = null,
    val minFavorites: Int? = null,
    val uploadedWithin: NUploadPeriod = NUploadPeriod.ANY
) {
    val isDefault: Boolean
        get() = this == DEFAULT

    val isFiltered: Boolean
        get() = !isDefault

    fun buildQuery(text: String = ""): String = buildList {
        val query = text.trim()
        if (query.isNotEmpty()) add(query)
        if (language.isNotEmpty() && !query.contains("language:", ignoreCase = true)) {
            add("language:$language")
        }
        tags.forEach { add(it.searchToken) }
        minPages?.let { add("pages:>=$it") }
        maxPages?.let { add("pages:<=$it") }
        minFavorites?.let { add("favorites:>=$it") }
        if (uploadedWithin != NUploadPeriod.ANY) add("uploaded:<${uploadedWithin.value}")
    }.joinToString(" ")

    companion object {
        val DEFAULT = NAlbumFilter()
    }
}

data class NTagCondition(val tag: NTag, val excluded: Boolean) {
    val searchToken: String
        get() {
            val type = tag.type.ifBlank { "tag" }
            val name = tag.name.replace("\\", "\\\\").replace("\"", "\\\"")
            val prefix = if (excluded) "-" else ""
            return "$prefix$type:\"$name\""
        }
}

enum class NUploadPeriod(val value: String, val title: String) {
    ANY("", "За всё время"),
    DAY("1d", "За сутки"),
    WEEK("7d", "За неделю"),
    MONTH("1m", "За месяц")
}

enum class NTagType(val value: String, val title: String) {
    TAG("tag", "Теги"),
    ARTIST("artist", "Авторы"),
    GROUP("group", "Группы"),
    PARODY("parody", "Серии"),
    CHARACTER("character", "Персонажи"),
    CATEGORY("category", "Категории"),
    LANGUAGE("language", "Языки")
}

enum class NTagSort(val value: String, val title: String) {
    POPULAR("popular", "По популярности"),
    NAME("name", "По имени")
}

data class NTagPage(val tags: List<NTag>, val totalPages: Int)
