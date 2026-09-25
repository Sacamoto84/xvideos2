package com.client.xvideos.l.net.graphQl

import com.client.xvideos.l.model.FilterGenre
import com.client.xvideos.l.net.json.LJson
import com.client.xvideos.l.repository.Repository
import com.client.xvideos.l.repository.RepositoryUriConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber

private const val mediaCategoriesBootstrap =
    """{"operationName":"MediaCategoriesBootstrap","query":"\n    query MediaCategoriesBootstrap {\n  media_categories {\n    genres {\n      id\n      title\n      slug\n      description\n      uploading_rules\n      poster_url\n      acts_as_warning\n      acts_as_default\n      represents_uncategorized\n      url\n      parent {\n        id\n      }\n      only_allows_model\n      only_content {\n        id\n        title\n        url\n      }\n    }\n    filter_settings {\n      user_id\n      has_custom_filters\n      uses_default_warnings\n      audience_ids\n      genres_blocked_ids\n      genres_subscribed_ids\n      preferred_language_ids\n      default_dashboard_content_id\n    }\n    languages {\n      id\n      title\n      url\n    }\n    content_types {\n      id\n      title\n      url\n    }\n    audiences {\n      id\n      title\n      description\n      poster_url\n      url\n    }\n  }\n}\n    ","variables":{}}"""

val mediaCategoriesFlow = MutableStateFlow<MediaCategories?>(null)

suspend fun refreshMediaCategories(repository: Repository, forceRefresh: Boolean = false) {
    Timber.d("refreshMediaCategories (forceRefresh=$forceRefresh)")

    val config = if (forceRefresh) RepositoryUriConfig.DIRECT else RepositoryUriConfig.CACHE_ROM
    val res = repository.openURI(mediaCategoriesBootstrap, config = config)

    if (res.isFailure) return

    val raw = res.getOrNull().orEmpty()
    val response = runCatching {
        LJson.decodeFromString<MediaCategoriesBootstrapResponse>(raw)
    }.getOrElse { e ->
        Timber.w(e, "Failed to decode MediaCategoriesBootstrapResponse")
        return
    }

    withContext(Dispatchers.Main) {
        mediaCategoriesFlow.value = response.data.mediaCategories
    }
}

// Основной класс для всего ответа
@Serializable
data class MediaCategoriesBootstrapResponse(
    @SerialName("data")
    val data: ApiData = ApiData()
)

// Класс для данных
@Serializable
data class ApiData(
    @SerialName("media_categories")
    val mediaCategories: MediaCategories = MediaCategories()
)

// Класс для медиа категорий
@Serializable
data class MediaCategories(
    @SerialName("genres")
    val genres: List<FilterGenre> = emptyList(),

    @SerialName("filter_settings")
    val filterSettings: FilterSettings = FilterSettings(),

    @SerialName("languages")
    val languages: List<Language> = emptyList(),

    @SerialName("content_types")
    val contentTypes: List<ContentType> = emptyList(),

    @SerialName("audiences")
    val audiences: List<Audience> = emptyList()
)

// Жанр и его ограничение по контенту переехали в model.FilterGenre: на них
// ссылается AlbumListFilter, то есть слой ниже сети.

// Класс для настроек фильтров
@Serializable
data class FilterSettings(
    @SerialName("user_id")
    val userId: Long = 0L,

    @SerialName("has_custom_filters")
    val hasCustomFilters: Boolean = false,

    @SerialName("uses_default_warnings")
    val usesDefaultWarnings: Boolean = false,

    @SerialName("audience_ids")
    val audienceIds: List<String> = emptyList(),

    @SerialName("genres_blocked_ids")
    val genresBlockedIds: List<String> = emptyList(),

    @SerialName("genres_subscribed_ids")
    val genresSubscribedIds: List<String> = emptyList(),

    @SerialName("preferred_language_ids")
    val preferredLanguageIds: List<String> = emptyList(),

    @SerialName("default_dashboard_content_id")
    val defaultDashboardContentId: String = ""
)

// Класс для языков
@Serializable
data class Language(
    @SerialName("id")
    val id: String = "",

    @SerialName("title")
    val title: String = "",

    @SerialName("url")
    val url: String = ""
)

// Класс для типов контента
@Serializable
data class ContentType(
    @SerialName("id")
    val id: String = "",

    @SerialName("title")
    val title: String = "",

    @SerialName("url")
    val url: String = ""
)

// Класс для аудиторий
@Serializable
data class Audience(
    @SerialName("id")
    val id: String = "",

    @SerialName("title")
    val title: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("poster_url")
    val posterUrl: String? = null,

    @SerialName("url")
    val url: String = ""
)
