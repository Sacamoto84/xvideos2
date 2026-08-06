package com.client.xvideos.r.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.common.search.ISearchTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Поле поиска с подсказками, историей и отменой ввода.
 *
 * Раньше это был `@Composable`-метод внутри `ISearchTemplate` — класса
 * состояния. Из-за него слой состояния тянул за собой три content-файла с
 * Compose, а перенести их в `ui` было нельзя: домен стал бы импортировать UI.
 * Теперь состояние приходит параметром, и отрисовка живёт там, где должна.
 */
@Composable
fun RSearchTextField(
    search: ISearchTemplate,
    modifier: Modifier = Modifier,
) {
    val searchTagSuggestions by search.searchTextSuggestions.collectAsStateWithLifecycle()
    val historyItems by search.history.collectAsState()
    val text by search.searchText.collectAsStateWithLifecycle()

    CustomBasicTextFieldContent(
        modifier = modifier,
        value = text,
        onValueChange = { search.searchText.value = it },
        suggestions = { searchTagSuggestions },
        onSuggestionClick = { suggestion ->
            search.searchText.value =
                TextFieldValue(text = suggestion.text, selection = TextRange(suggestion.text.length))
            search.searchTextDone.value = suggestion.text

            search.scope.launch(Dispatchers.Main) {
                if (suggestion.text.isNotEmpty()) {
                    search.add(suggestion.text)
                    search.stack.addLast(suggestion.text)
                }
            }
        },
        onClearClick = {
            search.searchText.value = TextFieldValue(text = "", selection = TextRange(0))
        },
        onUndoClick = {
            if (search.stack.isNotEmpty()) {
                val last = search.stack.removeLast()
                search.searchText.value = TextFieldValue(text = last, selection = TextRange(last.length))
            }
        },
        onDone = {
            search.searchTextDone.value = it
            search.scope.launch(Dispatchers.Main) {
                if (it.isNotEmpty()) {
                    search.add(it)
                    search.stack.addLast(it)
                }
            }
        },
        expandMenuHistory = {
            ExpandMenuHistoryContent(
                items = { historyItems },
                onClick = {
                    search.searchText.value = TextFieldValue(text = it, selection = TextRange(it.length))
                    search.searchTextDone.value = it
                },
                onDeleteClick = { search.scope.launch(Dispatchers.Main) { search.delete(it) } }
            )
        },
        onFocused = { search.focused.value = it }
    )
}

