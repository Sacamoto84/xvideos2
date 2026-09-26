package com.client.xvideos.l.model.enum

/**
 * Категории целевой аудитории альбомов Luscious.
 *
 * @property id Числовой идентификатор категории в API.
 * @property title Отображаемое название аудитории.
 * @property description Текстовое описание аудитории.
 * @property posterUrl URL постера категории.
 * @property url Относительный URL страницы аудитории на сайте.
 */
enum class AudiencesType(
    val id: Int,
    val title: String,
    val description: String,
    val posterUrl: String?,
    val url: String
) {
    GAY(
        id = 2,
        title = "Gay / Yaoi",
        description = "For people who like men with men.",
        posterUrl = null,
        url = "/audiences/gay_2/"
    ),
    LESBIAN(
        id = 3,
        title = "Lesbian / Yuri",
        description = "For people who like women with women.",
        posterUrl = null,
        url = "/audiences/lesbian_3/"
    ),
    SOLO_GIRL(
        id = 6,
        title = "Solo Girl",
        description = "Features individual women without a partner.",
        posterUrl = null,
        url = "/audiences/solo-girl_6/"
    ),
    SOLO_GUY(
        id = 12,
        title = "Solo Guy",
        description = "",
        posterUrl = null,
        url = "/audiences/solo-male_12/"
    ),
    STRAIGHT(
        id = 1,
        title = "Straight Sex",
        description = "For people who like sex between men and women.",
        posterUrl = null,
        url = "/audiences/straight_1/"
    ),
    TRANS(
        id = 5,
        title = "Trans",
        description = "Features transsexual women in solo action.",
        posterUrl = null,
        url = "/audiences/trans_5/"
    ),
    TRANS_X_GIRL(
        id = 10,
        title = "Trans x Girl",
        description = "Features transsexual women engaged in sex with females.",
        posterUrl = null,
        url = "/audiences/trans-x-girl_10/"
    ),
    TRANS_X_GUY(
        id = 9,
        title = "Trans x Guy",
        description = "Features transsexual women engaged in sex with men.",
        posterUrl = null,
        url = "/audiences/trans-x-guy_9/"
    ),
    TRANS_X_TRANS(
        id = 8,
        title = "Trans x Trans",
        description = "Features transsexual women engaged in sex with fellow t-girls.",
        posterUrl = null,
        url = "/audiences/trans-x-trans_8/"
    );

    val hasDescription: Boolean get() = description.isNotBlank()
    val hasPoster: Boolean get() = !posterUrl.isNullOrBlank()
    val isSolo: Boolean get() = this == SOLO_GIRL || this == SOLO_GUY
    val isTrans: Boolean get() = this == TRANS || this == TRANS_X_GIRL || this == TRANS_X_GUY || this == TRANS_X_TRANS

    fun matchesTitle(titleQuery: String?): Boolean =
        !titleQuery.isNullOrBlank() && title.contains(titleQuery, ignoreCase = true)

    /** Переход к следующей категории аудитории циклически. */
    fun next(): AudiencesType {
        val nextOrdinal = (ordinal + 1) % entries.size
        return entries[nextOrdinal]
    }

    /** Переход к предыдущей категории аудитории циклически. */
    fun prev(): AudiencesType {
        val prevOrdinal = if (ordinal == 0) entries.size - 1 else ordinal - 1
        return entries[prevOrdinal]
    }

    companion object {
        val DEFAULT = STRAIGHT

        val allTitles: List<String> = entries.map { it.title }

        fun fromId(id: Int): AudiencesType? = entries.find { it.id == id }
        fun fromIdOrNull(id: Int?): AudiencesType? = if (id != null) fromId(id) else null
        fun fromIdOrDefault(id: Int?, default: AudiencesType = DEFAULT): AudiencesType =
            fromIdOrNull(id) ?: default

        fun fromUrl(url: String): AudiencesType? = entries.find { it.url.equals(url, ignoreCase = true) }
        fun fromUrlOrNull(url: String?): AudiencesType? = if (!url.isNullOrBlank()) fromUrl(url) else null
        fun fromUrlOrDefault(url: String?, default: AudiencesType = DEFAULT): AudiencesType =
            fromUrlOrNull(url) ?: default

        fun fromTitleOrNull(title: String?): AudiencesType? =
            if (!title.isNullOrBlank()) entries.firstOrNull { it.title.equals(title, ignoreCase = true) } else null

        fun fromTitleOrDefault(title: String?, default: AudiencesType = DEFAULT): AudiencesType =
            fromTitleOrNull(title) ?: default

        fun fromStringOrNull(value: String?): AudiencesType? =
            value?.toIntOrNull()?.let { fromId(it) } ?: fromTitleOrNull(value) ?: fromUrlOrNull(value)

        fun fromOrdinalOrDefault(ordinal: Int, default: AudiencesType = DEFAULT): AudiencesType =
            entries.getOrNull(ordinal) ?: default
    }
}
