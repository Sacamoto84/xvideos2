package com.client.xvideos.l.model

import com.google.gson.annotations.SerializedName

/**
 * Жанр из каталога Luscious — тот, по которому фильтруют список альбомов.
 *
 * Не путать с [Genre] из `AlbumListType`: там короткая ссылка на жанр внутри
 * ответа со списком альбомов (id, title, url), здесь — полная карточка из
 * `MediaCategoriesBootstrap`. Раньше оба назывались `Genre`, лежали в разных
 * пакетах и в местах, где встречались оба, приходилось писать
 * `import ... .Genre as FilterGenre` — теперь имя честное.
 *
 * Живёт в `model`, а не рядом с запросом: на него ссылается `AlbumListFilter`,
 * то есть слой ниже сети.
 */
data class FilterGenre(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("slug")
    val slug: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("uploading_rules")
    val uploadingRules: String,

    @SerializedName("poster_url")
    val posterUrl: String?,

    @SerializedName("acts_as_warning")
    val actsAsWarning: Boolean,

    @SerializedName("acts_as_default")
    val actsAsDefault: Boolean,

    @SerializedName("represents_uncategorized")
    val representsUncategorized: Boolean,

    @SerializedName("url")
    val url: String,

    @SerializedName("parent")
    val parent: String?,

    @SerializedName("only_allows_model")
    val onlyAllowsModel: List<String>?,

    @SerializedName("only_content")
    val onlyContent: OnlyContent?
)

/** Ограничение жанра по типу контента. */
data class OnlyContent(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("url")
    val url: String
)
