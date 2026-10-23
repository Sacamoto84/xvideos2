package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable
import java.io.Serializable

/**
 * Элемент каталога актрис/порнозвезд/моделей.
 *
 * @property slug Идентификатор модели (например, `"sweetie-fox1"`).
 * @property name Отображаемое имя модели (например, `"Sweetie Fox"`).
 * @property rankText Номер в текущем рейтинге (например, `"#1"`).
 * @property avatarUrl Ссылка на аватар/превью модели.
 * @property country Название страны (например, `"Россия"`).
 * @property countryCode Двухбуквенный код страны (например, `"ru"`).
 * @property videoCount Строка с числом видео (например, `"867 видео"`).
 * @property profileUrl Относительная или абсолютная ссылка на профиль.
 */
@kotlinx.serialization.Serializable
@Immutable
data class ActressesIndexItem(
    val slug: String = "",
    val name: String = "",
    val rankText: String = "",
    val avatarUrl: String = "",
    val country: String = "",
    val countryCode: String = "",
    val videoCount: String = "",
    val profileUrl: String = "",
) : Serializable {

    val displayName: String get() = name.ifBlank { slug }

    /**
     * Профиль модели, а не канала. Каталог открывается и из рейтингов канала, поэтому
     * тип берётся из ссылки карточки: `/channels/…` — канал, остальное — модель.
     */
    val isModel: Boolean
        get() = !profileUrl.contains("/channels/", ignoreCase = true)

    /** Флаг страны по [countryCode] (общий [getFlagEmojiOrNull]); пустая строка, если код не распознан. */
    val flagEmoji: String
        get() = getFlagEmojiOrNull(countryCode.trim()).orEmpty()
}

/**
 * Опция фильтра в каталоге (например, страна или период сортировки).
 *
 * @property title Отображаемое название пункта.
 * @property urlPath Ссылка/путь для перехода (например, `"/porn-actresses-index/from/russia/ever"`).
 * @property isActive Выбран ли пункт в данный момент.
 */
@kotlinx.serialization.Serializable
@Immutable
data class ActressesIndexFilterOption(
    val title: String = "",
    val urlPath: String = "",
    val isActive: Boolean = false,
) : Serializable {
    fun matches(query: String): Boolean =
        query.isBlank() || title.contains(query.trim(), ignoreCase = true)
}

/**
 * Тип выпадающего списка фильтра.
 */
enum class ActressesIndexDropdownType : Serializable {
    GEO,
    PROFILE_TYPE,
    TIME_SORT,
}

/**
 * Группа фильтра с активным заголовком и списком опций.
 */
@kotlinx.serialization.Serializable
@Immutable
data class ActressesIndexFilterGroup(
    val type: ActressesIndexDropdownType = ActressesIndexDropdownType.GEO,
    val activeTitle: String = "",
    val options: List<ActressesIndexFilterOption> = emptyList(),
) : Serializable {
    val selectedOption: ActressesIndexFilterOption? get() = options.firstOrNull { it.isActive }
    val displayTitle: String get() = selectedOption?.title?.ifBlank { activeTitle } ?: activeTitle
}

/** Кого ищет поиск по имени в каталоге: рейтинг моделей — модели, рейтинг каналов — каналы. */
enum class CatalogProfileKind : Serializable {
    MODELS,
    CHANNELS,
}

/**
 * Страна для поиска «конкретной страны» каталога.
 *
 * @property code Код страны (`"EE"`) — по нему Android даёт название на языке устройства.
 * @property token Часть адреса рейтинга страны (`"estonia"`), подставляется в шаблон каталога.
 */
@Immutable
data class CatalogCountry(
    val code: String,
    val token: String,
) : Serializable

/**
 * Найденный по имени профиль (модель или канал) из общего поиска сайта.
 *
 * @property slug Идентификатор профиля для экрана канала.
 * @property subscribers Число подписчиков текстом сайта, может быть пустым.
 */
@Immutable
data class CatalogProfileHit(
    val name: String,
    val slug: String,
    val avatarUrl: String,
    val subscribers: String,
    val isModel: Boolean,
) : Serializable

/**
 * Распарсенные данные одной страницы каталога актрис/моделей.
 *
 * @property profileTypeFilter Второй список: типы моделей, а у рейтинга каналов — основная
 *   категория («Гетеросексуальный» / «Геи» / «Трансы»), списка типов там нет.
 * @property profileKind Кого ищет поиск по имени во втором списке.
 * @property countries Все страны, по которым есть рейтинг (из настроек страницы).
 * @property countryUrlTemplate Адрес рейтинга страны с `%country_token%` вместо страны.
 */
@Immutable
data class ActressesIndexCatalog(
    val totalCountTitle: String = "",
    val subtitle: String = "",
    val geoFilter: ActressesIndexFilterGroup = ActressesIndexFilterGroup(type = ActressesIndexDropdownType.GEO),
    val profileTypeFilter: ActressesIndexFilterGroup = ActressesIndexFilterGroup(type = ActressesIndexDropdownType.PROFILE_TYPE),
    val timeSortFilter: ActressesIndexFilterGroup = ActressesIndexFilterGroup(type = ActressesIndexDropdownType.TIME_SORT),
    val items: List<ActressesIndexItem> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val hasNextPage: Boolean = false,
    val nextPageUrl: String = "",
    val profileKind: CatalogProfileKind = CatalogProfileKind.MODELS,
    val countries: List<CatalogCountry> = emptyList(),
    val countryUrlTemplate: String = "",
) : Serializable {
    companion object {
        val EMPTY = ActressesIndexCatalog()
    }
}

/**
 * Поиск в открытом выпадающем списке каталога.
 *
 * @property query Введённый текст.
 * @property countryHits Найденные страны (список стран).
 * @property profileHits Найденные по имени модели или каналы (второй список).
 * @property isLoading Запрос поиска по имени ещё идёт.
 * @property error Поиск по имени не удался (сеть).
 */
@Immutable
data class CatalogDropdownSearch(
    val query: String = "",
    val countryHits: List<ActressesIndexFilterOption> = emptyList(),
    val profileHits: List<CatalogProfileHit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) : Serializable

/**
 * Состояние пользовательского интерфейса экрана каталога рейтингов актрис.
 */
@Immutable
data class ActressesIndexUiState(
    val currentUrlPath: String = "",
    val title: String = "",
    val catalog: ActressesIndexCatalog = ActressesIndexCatalog.EMPTY,
    val items: List<ActressesIndexItem> = emptyList(),
    val activeDropdown: ActressesIndexDropdownType? = null,
    val dropdownSearch: CatalogDropdownSearch = CatalogDropdownSearch(),
    val isLoadingInitial: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isEndReached: Boolean = false,
    val error: String? = null,
    /** Следующая страница не загрузилась: автоподгрузка ждёт «Повторить». */
    val loadMoreError: String? = null,
) : Serializable {
    val isEmpty: Boolean get() = !isLoadingInitial && items.isEmpty() && error == null

    /** Можно подгружать следующую страницу; после сбоя — только по «Повторить». */
    val canLoadMore: Boolean
        get() = !isLoadingInitial && !isLoadingMore && !isEndReached && loadMoreError == null
}
