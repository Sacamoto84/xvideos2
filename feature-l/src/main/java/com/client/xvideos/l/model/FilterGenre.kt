package com.client.xvideos.l.model

import androidx.compose.runtime.Immutable
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
 *
 * @property id Идентификатор жанра.
 * @property title Название жанра.
 * @property slug URL-слаг жанра.
 * @property description Описание жанра.
 * @property uploadingRules Правила загрузки контента по жанру.
 * @property posterUrl URL постера/обложки жанра.
 * @property actsAsWarning Помечает ли жанр альбом как содержащий предупреждение.
 * @property actsAsDefault Выбран ли по умолчанию.
 * @property representsUncategorized Представляет ли категорию «без категории».
 * @property url Относительный путь к странице жанра.
 * @property parent Идентификатор родительской категории.
 * @property onlyAllowsModel Ограничение по моделям.
 * @property onlyContent Ограничение по типу контента.
 */
@Immutable
@kotlinx.serialization.Serializable
data class FilterGenre(
    @SerialName("id")
    val id: String = "",

    @SerialName("title")
    val title: String = "",

    @SerialName("slug")
    val slug: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("uploading_rules")
    val uploadingRules: String = "",

    @SerialName("poster_url")
    val posterUrl: String? = null,

    @SerialName("acts_as_warning")
    val actsAsWarning: Boolean = false,

    @SerialName("acts_as_default")
    val actsAsDefault: Boolean = false,

    @SerialName("represents_uncategorized")
    val representsUncategorized: Boolean = false,

    @SerialName("url")
    val url: String = "",

    @SerialName("parent")
    val parent: String? = null,

    @SerialName("only_allows_model")
    val onlyAllowsModel: List<String>? = null,

    @SerialName("only_content")
    val onlyContent: OnlyContent? = null
) : Serializable {
    companion object {
        val EMPTY = FilterGenre()
    }
}

/** Ограничение жанра по типу контента. `Serializable` вслед за [FilterGenre]. */
@Immutable
@kotlinx.serialization.Serializable
data class OnlyContent(
    @SerialName("id")
    val id: String = "",

    @SerialName("title")
    val title: String = "",

    @SerialName("url")
    val url: String = ""
) : Serializable {
    val isValid: Boolean get() = id.isNotBlank() && title.isNotBlank()

    companion object {
        val EMPTY = OnlyContent()
    }
}

/**
 * Валиден ли жанр (непустой id и title).
 */
val FilterGenre.isValid: Boolean get() = id.isNotBlank() && title.isNotBlank()

/**
 * Название жанра для отображения (title или slug).
 */
val FilterGenre.displayTitle: String get() = title.ifBlank { slug }

/** Псевдоним имени жанра. */
val FilterGenre.name: String get() = title

/** Наличие ссылки на обложку жанра. */
val FilterGenre.hasPoster: Boolean get() = !posterUrl.isNullOrBlank()

/** Наличие непустого описания жанра. */
val FilterGenre.hasDescription: Boolean get() = description.isNotBlank()

/** Наличие идентификатора родительской категории. */
val FilterGenre.hasParent: Boolean get() = !parent.isNullOrBlank()

/** Наличие правил загрузки контента. */
val FilterGenre.hasUploadingRules: Boolean get() = uploadingRules.isNotBlank()

/** Проверяет равенство жанров по идентификатору. */
fun FilterGenre.isSameGenre(other: FilterGenre?): Boolean =
    other != null && id.isNotBlank() && id == other.id

/** Наличие непустого URL контента. */
val OnlyContent.hasUrl: Boolean get() = url.isNotBlank()

