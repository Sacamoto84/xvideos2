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

/**
 * Распарсенные данные одной страницы каталога актрис/моделей.
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
) : Serializable {
    companion object {
        val EMPTY = ActressesIndexCatalog()
    }
}

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
    val dropdownSearchQuery: String = "",
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
