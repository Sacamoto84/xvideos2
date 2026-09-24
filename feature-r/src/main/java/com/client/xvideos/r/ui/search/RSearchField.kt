package com.client.xvideos.r.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.common.search.ISearchTemplate
import com.client.xvideos.r.common.search.SuggestionItem
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
    val historyItems by search.history.collectAsStateWithLifecycle()
    val text by search.searchText.collectAsStateWithLifecycle()

    val onValueChange: (TextFieldValue) -> Unit = remember(search) {
        { newValue ->
            search.searchText.value = newValue
            // Пустая строка — единственное значение, которое применяется без
            // подтверждения. Подтверждать нечего: «ничего не искать» — это не
            // запрос, а его отсутствие.
            //
            // Иначе выйти из фильтрации было нельзя: стёр текст руками, а лента
            // и список ниш остались отфильтрованы прежним запросом — они читают
            // searchTextDone, а он менялся только по onDone.
            if (newValue.text.isBlank()) {
                search.searchTextDone.value = ""
            }
        }
    }

    val suggestionsProvider: () -> List<SuggestionItem> = remember(searchTagSuggestions) {
        { searchTagSuggestions }
    }

    val onSuggestionClick: (SuggestionItem) -> Unit = remember(search) {
        { suggestion ->
            search.searchText.value =
                TextFieldValue(text = suggestion.text, selection = TextRange(suggestion.text.length))
            search.searchTextDone.value = suggestion.text
            search.pushHistory(suggestion.text)

            if (suggestion.text.isNotEmpty()) {
                search.scope.launch(Dispatchers.IO) {
                    search.add(suggestion.text)
                }
            }
        }
    }

    val onClearClick: () -> Unit = remember(search) {
        {
            search.searchText.value = TextFieldValue(text = "", selection = TextRange(0))
            // Кнопка «стереть» снимает и фильтр. Раньше она чистила только
            // отображаемый текст, и запрос продолжал действовать: поле пустое,
            // а выдача отфильтрована — вернуться к полному списку было нечем.
            search.searchTextDone.value = ""
        }
    }

    val onUndoClick: () -> Unit = remember(search) {
        {
            val prev = search.popHistory(search.searchTextDone.value)
            if (prev != null) {
                search.searchText.value = TextFieldValue(text = prev, selection = TextRange(prev.length))
                search.searchTextDone.value = prev
            }
        }
    }

    val onDone: (String) -> Unit = remember(search) {
        { query ->
            search.searchTextDone.value = query
            search.pushHistory(query)
            if (query.isNotEmpty()) {
                search.scope.launch(Dispatchers.IO) {
                    search.add(query)
                }
            }
        }
    }

    val onFocused: (Boolean) -> Unit = remember(search) {
        { search.focused.value = it }
    }

    val onHistoryClick: (String) -> Unit = remember(search) {
        { query ->
            search.searchText.value = TextFieldValue(text = query, selection = TextRange(query.length))
            search.searchTextDone.value = query
        }
    }

    val onHistoryDelete: (String) -> Unit = remember(search) {
        { query -> search.scope.launch(Dispatchers.IO) { search.delete(query) } }
    }

    val historyItemsProvider = remember(historyItems) { { historyItems } }

    val expandMenuHistory: @Composable () -> Unit = remember(historyItemsProvider, onHistoryClick, onHistoryDelete) {
        {
            ExpandMenuHistoryContent(
                items = historyItemsProvider,
                onClick = onHistoryClick,
                onDeleteClick = onHistoryDelete
            )
        }
    }

    CustomBasicTextFieldContent(
        modifier = modifier,
        value = text,
        onValueChange = onValueChange,
        suggestions = suggestionsProvider,
        onSuggestionClick = onSuggestionClick,
        onClearClick = onClearClick,
        onUndoClick = onUndoClick,
        onDone = onDone,
        expandMenuHistory = expandMenuHistory,
        onFocused = onFocused
    )
}

