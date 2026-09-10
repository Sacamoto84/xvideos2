package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

private val mediaCategoriesBootstrap =
    """{"operationName":"MediaCategoriesBootstrap","query":"\n    query MediaCategoriesBootstrap {\n  media_categories {\n    genres {\n      id\n      title\n      slug\n      description\n      uploading_rules\n      poster_url\n      acts_as_warning\n      acts_as_default\n      represents_uncategorized\n      url\n      parent {\n        id\n      }\n      only_allows_model\n      only_content {\n        id\n        title\n        url\n      }\n    }\n    filter_settings {\n      user_id\n      has_custom_filters\n      uses_default_warnings\n      audience_ids\n      genres_blocked_ids\n      genres_subscribed_ids\n      preferred_language_ids\n      default_dashboard_content_id\n    }\n    languages {\n      id\n      title\n      url\n    }\n    content_types {\n      id\n      title\n      url\n    }\n    audiences {\n      id\n      title\n      description\n      poster_url\n      url\n    }\n  }\n}\n    ","variables":{}}"""

var mediaCategoriesFlow = MutableStateFlow<MediaCategories?>(null)

suspend fun refreshMediaCategories(repository: Repository) {
    Timber.i("!!! refreshMediaCategories")

    val q = mediaCategoriesBootstrap
    val res = repository.openURI(q, config = RepositoryUriConfig.CACHE_ROM)

    if (res.isFailure) return

    val gson = Gson()
    val response = gson.fromJson(res.getOrNull(), MediaCategoriesBootstrapResponse::class.java)

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
data class MediaCategoriesBootstrapResponse(
    @SerializedName("data")
    val data: ApiData
)

// Класс для данных
data class ApiData(
    @SerializedName("media_categories")
    val mediaCategories: MediaCategories
)

// Класс для медиа категорий
data class MediaCategories(
    @SerializedName("genres")
    val genres: List<FilterGenre>,

    @SerializedName("filter_settings")
    val filterSettings: FilterSettings,

    @SerializedName("languages")
    val languages: List<Language>,

    @SerializedName("content_types")
    val contentTypes: List<ContentType>,

    @SerializedName("audiences")
    val audiences: List<Audience>
)

// Жанр и его ограничение по контенту переехали в model.FilterGenre: на них
// ссылается AlbumListFilter, то есть слой ниже сети.

// Класс для настроек фильтров
data class FilterSettings(
    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("has_custom_filters")
    val hasCustomFilters: Boolean,

    @SerializedName("uses_default_warnings")
    val usesDefaultWarnings: Boolean,

    @SerializedName("audience_ids")
    val audienceIds: List<String>,

    @SerializedName("genres_blocked_ids")
    val genresBlockedIds: List<String>,

    @SerializedName("genres_subscribed_ids")
    val genresSubscribedIds: List<String>,

    @SerializedName("preferred_language_ids")
    val preferredLanguageIds: List<String>,

    @SerializedName("default_dashboard_content_id")
    val defaultDashboardContentId: String
)

// Класс для языков
data class Language(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("url")
    val url: String
)

// Класс для типов контента
data class ContentType(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("url")
    val url: String
)

// Класс для аудиторий
data class Audience(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("poster_url")
    val posterUrl: String?,

    @SerializedName("url")
    val url: String
)
