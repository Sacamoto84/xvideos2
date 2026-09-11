package com.client.xvideos.l.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import java.io.Serializable

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
@kotlinx.serialization.Serializable
data class FilterGenre(
    @SerializedName("id")
    @SerialName("id")
    val id: String = "",

    @SerializedName("title")
    @SerialName("title")
    val title: String = "",

    @SerializedName("slug")
    @SerialName("slug")
    val slug: String = "",

    @SerializedName("description")
    @SerialName("description")
    val description: String = "",

    @SerializedName("uploading_rules")
    @SerialName("uploading_rules")
    val uploadingRules: String = "",

    @SerializedName("poster_url")
    @SerialName("poster_url")
    val posterUrl: String? = null,

    @SerializedName("acts_as_warning")
    @SerialName("acts_as_warning")
    val actsAsWarning: Boolean = false,

    @SerializedName("acts_as_default")
    @SerialName("acts_as_default")
    val actsAsDefault: Boolean = false,

    @SerializedName("represents_uncategorized")
    @SerialName("represents_uncategorized")
    val representsUncategorized: Boolean = false,

    @SerializedName("url")
    @SerialName("url")
    val url: String = "",

    @SerializedName("parent")
    @SerialName("parent")
    val parent: String? = null,

    @SerializedName("only_allows_model")
    @SerialName("only_allows_model")
    val onlyAllowsModel: List<String>? = null,

    @SerializedName("only_content")
    @SerialName("only_content")
    val onlyContent: OnlyContent? = null
) : Serializable

/** Ограничение жанра по типу контента. `Serializable` вслед за [FilterGenre]. */
@kotlinx.serialization.Serializable
data class OnlyContent(
    @SerializedName("id")
    @SerialName("id")
    val id: String = "",

    @SerializedName("title")
    @SerialName("title")
    val title: String = "",

    @SerializedName("url")
    @SerialName("url")
    val url: String = ""
) : Serializable
