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
)

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
