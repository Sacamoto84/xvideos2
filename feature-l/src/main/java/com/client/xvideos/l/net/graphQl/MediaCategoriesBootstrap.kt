package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber

private val mediaCategoriesBootstrap =
    """{"operationName":"MediaCategoriesBootstrap","query":"\n    query MediaCategoriesBootstrap {\n  media_categories {\n    genres {\n      id\n      title\n      slug\n      description\n      uploading_rules\n      poster_url\n      acts_as_warning\n      acts_as_default\n      represents_uncategorized\n      url\n      parent {\n        id\n      }\n      only_allows_model\n      only_content {\n        id\n        title\n        url\n      }\n    }\n    filter_settings {\n      user_id\n      has_custom_filters\n      uses_default_warnings\n      audience_ids\n      genres_blocked_ids\n      genres_subscribed_ids\n      preferred_language_ids\n      default_dashboard_content_id\n    }\n    languages {\n      id\n      title\n      url\n    }\n    content_types {\n      id\n      title\n      url\n    }\n    audiences {\n      id\n      title\n      description\n      poster_url\n      url\n    }\n  }\n}\n    ","variables":{}}"""

var mediaCategoriesFlow = MutableStateFlow<MediaCategories?>(null)

suspend fun refreshMediaCategories(repository: Repository) {
    Timber.i("!!! refreshMediaCategories")

    val q = mediaCategoriesBootstrap
    val res = repository.openURI(q, config = RepositoryUriConfig.CACHE_ROM)

    if (res.isFailure) return

    val raw = res.getOrNull().orEmpty()
    val response = runCatching {
        LJson.decodeFromString<MediaCategoriesBootstrapResponse>(raw)
    }.getOrElse {
        val gson = Gson()
        gson.fromJson(raw, MediaCategoriesBootstrapResponse::class.java)
    }

    withContext(Dispatchers.Main) {
        mediaCategoriesFlow.value = MediaCategories(
            response.data.mediaCategories.genres,
            response.data.mediaCategories.filterSettings,
            response.data.mediaCategories.languages,
            response.data.mediaCategories.contentTypes,
            response.data.mediaCategories.audiences
        )
    }
}

// Основной класс для всего ответа
@Serializable
data class MediaCategoriesBootstrapResponse(
    @SerializedName("data")
    @SerialName("data")
    val data: ApiData = ApiData()
)

// Класс для данных
@Serializable
data class ApiData(
    @SerializedName("media_categories")
    @SerialName("media_categories")
    val mediaCategories: MediaCategories = MediaCategories()
)

// Класс для медиа категорий
@Serializable
data class MediaCategories(
    @SerializedName("genres")
    @SerialName("genres")
    val genres: List<FilterGenre> = emptyList(),

    @SerializedName("filter_settings")
    @SerialName("filter_settings")
    val filterSettings: FilterSettings = FilterSettings(),

    @SerializedName("languages")
    @SerialName("languages")
    val languages: List<Language> = emptyList(),

    @SerializedName("content_types")
    @SerialName("content_types")
    val contentTypes: List<ContentType> = emptyList(),

    @SerializedName("audiences")
    @SerialName("audiences")
    val audiences: List<Audience> = emptyList()
)

// Жанр и его ограничение по контенту переехали в model.FilterGenre: на них
// ссылается AlbumListFilter, то есть слой ниже сети.

// Класс для настроек фильтров
@Serializable
data class FilterSettings(
    @SerializedName("user_id")
    @SerialName("user_id")
    val userId: Long = 0L,

    @SerializedName("has_custom_filters")
    @SerialName("has_custom_filters")
    val hasCustomFilters: Boolean = false,

    @SerializedName("uses_default_warnings")
    @SerialName("uses_default_warnings")
    val usesDefaultWarnings: Boolean = false,

    @SerializedName("audience_ids")
    @SerialName("audience_ids")
    val audienceIds: List<String> = emptyList(),

    @SerializedName("genres_blocked_ids")
    @SerialName("genres_blocked_ids")
    val genresBlockedIds: List<String> = emptyList(),

    @SerializedName("genres_subscribed_ids")
    @SerialName("genres_subscribed_ids")
    val genresSubscribedIds: List<String> = emptyList(),

    @SerializedName("preferred_language_ids")
    @SerialName("preferred_language_ids")
    val preferredLanguageIds: List<String> = emptyList(),

    @SerializedName("default_dashboard_content_id")
    @SerialName("default_dashboard_content_id")
    val defaultDashboardContentId: String = ""
)

// Класс для языков
@Serializable
data class Language(
    @SerializedName("id")
    @SerialName("id")
    val id: String = "",

    @SerializedName("title")
    @SerialName("title")
    val title: String = "",

    @SerializedName("url")
    @SerialName("url")
    val url: String = ""
)

// Класс для типов контента
@Serializable
data class ContentType(
    @SerializedName("id")
    @SerialName("id")
    val id: String = "",

    @SerializedName("title")
    @SerialName("title")
    val title: String = "",

    @SerializedName("url")
    @SerialName("url")
    val url: String = ""
)

// Класс для аудиторий
@Serializable
data class Audience(
    @SerializedName("id")
    @SerialName("id")
    val id: String = "",

    @SerializedName("title")
    @SerialName("title")
    val title: String = "",

    @SerializedName("description")
    @SerialName("description")
    val description: String = "",

    @SerializedName("poster_url")
    @SerialName("poster_url")
    val posterUrl: String? = null,

    @SerializedName("url")
    @SerialName("url")
    val url: String = ""
)
