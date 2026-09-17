package com.client.xvideos.r.common.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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

@Immutable
@Serializable
data class SuggestionItem(
    @SerialName("text") val text: String = "",
    @SerialName("count") val count: Long = 0,
)

@Stable
abstract class ISearchTemplate(
    val scope: CoroutineScope,
    val dao : IDaoSearchTemplate
) {


    /**
     * Отображаемый текст
     */
    val searchText = MutableStateFlow(TextFieldValue(""))

    /**
     * Текст по которому будет идти запрос на сервер
     */
    val searchTextDone = MutableStateFlow("")

    val searchTextSuggestions = MutableStateFlow<List<SuggestionItem>>(emptyList())

    val stack = ArrayDeque<String>()

    companion object {
        const val MAX_STACK_SIZE = 50
    }

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

    val focused = MutableStateFlow(false)

    // Отрисовка переехала в ui/search/RSearchField.kt: класс держит состояние,
    // а composable принимают его параметром. Пока они были членами класса,
    // слой состояния тянул за собой Compose и три content-файла, и вынести те
    // в ui было нельзя — домен начал бы импортировать UI.

    val history: StateFlow<List<String>> = dao.observeAllTexts().stateIn( scope = scope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList() )

    suspend fun add(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) dao.insertAndTrim(trimmed)
    }

    suspend fun delete(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) dao.deleteByTexts(trimmed)
    }

    suspend fun clear() = dao.deleteAll()

}
