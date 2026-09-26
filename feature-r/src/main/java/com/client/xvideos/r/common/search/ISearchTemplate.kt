package com.client.xvideos.r.common.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.client.xvideos.r.model.tag.TagInfo
import com.client.xvideos.r.model.tag.TagSuggestion

/**
 * Пауза ввода, после которой запрашиваются подсказки.
 *
 * Раньше запрос уходил на каждое изменение текста: сборщик висел прямо на
 * `searchText`, без `debounce` и без отмены предыдущего запроса. Набранное слово
 * из пяти букв — до пяти обращений к сети подряд, и каждое ждали до конца.
 */
internal const val SUGGESTIONS_DEBOUNCE_MS = 300L

/**
 * Элемент подсказки (автодополнения) в строке поиска.
 *
 * @property text Текст подсказки.
 * @property count Количество материалов с этим запросом/тегом.
 */
@Immutable
@Serializable
data class SuggestionItem(
    @SerialName("text") val text: String = "",
    @SerialName("count") val count: Long = 0,
) {
    val isValid: Boolean get() = text.isNotBlank()
    val hasCount: Boolean get() = count > 0
    val normalizedText: String get() = text.trim().lowercase()

    fun matches(query: String?): Boolean =
        if (query.isNullOrBlank()) false else text.contains(query.trim(), ignoreCase = true)

    fun matchesQuery(query: String?): Boolean =
        if (query.isNullOrBlank()) true else matches(query)

    /** Форматирует количество материалов в компактный вид (k, M). */
    fun formatCount(): String {
        return when {
            count >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000L -> String.format(java.util.Locale.US, "%.1fk", count / 1_000.0)
            else -> count.toString()
        }
    }

    /** Преобразует подсказку в [TagInfo]. */
    fun toTagInfo(): TagInfo = TagInfo(name = text.trim(), count = count)

    /** Преобразует подсказку в [TagSuggestion]. */
    fun toTagSuggestion(type: String = "tag"): TagSuggestion =
        TagSuggestion(text = text.trim(), gifs = count, type = type)

    companion object {
        val EMPTY = SuggestionItem()

        fun fromTagInfo(tagInfo: TagInfo): SuggestionItem =
            SuggestionItem(text = tagInfo.name, count = tagInfo.count)

        fun fromTagSuggestion(suggestion: TagSuggestion): SuggestionItem =
            SuggestionItem(text = suggestion.text, count = suggestion.gifs)
    }
}

/**
 * Базовый абстрактный стейт-холдер строки поиска в разделах RedGifs.
 *
 * Инкапсулирует:
 * - Отображаемый текст [searchText] в виде [TextFieldValue] (для сохранения курсора);
 * - Зафиксированный подтвержденный поисковый запрос [searchTextDone];
 * - Список подсказок [searchTextSuggestions];
 * - Историю запросов [history] и стек навигации назад [stack];
 * - Управление фокусом поискового поля [focused].
 *
 * @property scope Скоп для корутин.
 * @property dao Доступ к персистентному хранилищу истории поиска.
 */
@Stable
abstract class ISearchTemplate(
    val scope: CoroutineScope,
    val dao : IDaoSearchTemplate
) {

    /**
     * Отображаемый в поле ввода текст с состоянием курсора и выделения.
     */
    val searchText = MutableStateFlow(TextFieldValue(""))

    /**
     * Текст, по которому запущен актуальный запрос на сервер.
     */
    val searchTextDone = MutableStateFlow("")

    /** Реактивный список подсказок автодополнения. */
    val searchTextSuggestions = MutableStateFlow<List<SuggestionItem>>(emptyList())

    /** Стек истории поиска для возврата к предыдущим запросам по Back. */
    val stack = ArrayDeque<String>()

    /** Проверяет, пуста ли строка ввода. */
    val isSearchTextEmpty: Boolean get() = searchText.value.text.isBlank()

    /** Проверяет, запущен ли активный подтвержденный поиск. */
    val isSearchActive: Boolean get() = searchTextDone.value.isNotBlank()

    /** Количество текущих подсказок автодополнения. */
    val suggestionsCount: Int get() = searchTextSuggestions.value.size

    /** Проверяет наличие хотя бы одной поисковой подсказки. */
    val hasSuggestions: Boolean get() = suggestionsCount > 0

    /** Текущий введенный текст поисковой строки. */
    val currentSearchText: String get() = searchText.value.text

    /** Обновляет отображаемый текст в поле ввода. */
    fun updateSearchText(text: String) {
        searchText.value = TextFieldValue(text)
    }

    /** Очищает строку ввода. */
    fun clearSearchText() {
        searchText.value = TextFieldValue("")
    }

    /** Количество элементов в стеке навигации истории поиска. */
    val stackSize: Int get() = synchronized(stack) { stack.size }

    /** Очищает стек навигации поиска. */
    fun clearStack() {
        synchronized(stack) {
            stack.clear()
        }
    }

    companion object {
        /** Максимальная глубина стека поисковых переходов. */
        const val MAX_STACK_SIZE = 50
    }

    /**
     * Помещает подтвержденный запрос в стек истории навигации.
     */
    fun pushHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        synchronized(stack) {
            if (stack.lastOrNull() == trimmed) return
            if (stack.size >= MAX_STACK_SIZE) {
                stack.removeFirst()
            }
            stack.addLast(trimmed)
        }
    }

    /**
     * Извлекает предыдущий поисковый запрос из стека истории.
     */
    fun popHistory(currentQuery: String): String? {
        val trimmed = currentQuery.trim()
        synchronized(stack) {
            if (stack.isEmpty()) return null
            if (stack.lastOrNull() == trimmed) {
                stack.removeLast()
            }
            return if (stack.isNotEmpty()) stack.last() else ""
        }
    }

    /** Флаг активности фокуса на поисковой строке. */
    val focused = MutableStateFlow(false)

    // Отрисовка переехала в ui/search/RSearchField.kt: класс держит состояние,
    // а composable принимают его параметром. Пока они были членами класса,
    // слой состояния тянул за собой Compose и три content-файла, и вынести те
    // в ui было нельзя — домен начал бы импортировать UI.

    /** Поток сохраненной истории предыдущих поисков. */
    val history: StateFlow<List<String>> = dao.observeAllTexts().stateIn( scope = scope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList() )

    /** Добавляет строку в историю поиска. */
    suspend fun add(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) dao.insertAndTrim(trimmed)
    }

    /** Удаляет строку из истории поиска. */
    suspend fun delete(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) dao.deleteByTexts(trimmed)
    }

    /** Очищает историю поиска. */
    suspend fun clear() = dao.deleteAll()

}
