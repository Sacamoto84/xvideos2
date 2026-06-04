package com.client.xvideos.r.common.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.common.saved.SavedRed
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SuggestionItem(
    @SerializedName("text") val text: String,  //
    @SerializedName("count") val count: Long,  //
)

abstract class ISearchTemplate(
    val scope: CoroutineScope,
    val dao : IDaoSearchTemplate
) {


    /**
     * Отображаемый текст
     */
    var searchText = MutableStateFlow(TextFieldValue(""))

    /**
     * Текст по которому будет идти запрос на сервер
     */
    var searchTextDone = MutableStateFlow("")



    var searchTextSuggestions = MutableStateFlow<List<SuggestionItem>>(emptyList())

    val stack = ArrayDeque<String>()

    val focused = MutableStateFlow(false)





    @Composable
    fun ExpandMenuHelper(
        modifier: Modifier = Modifier,
        savedRed: SavedRed
    ) {
        ExpandMenuHelperContent(
            tags = savedRed.tagsList,
            onTagClick = { tag ->
                searchText.value = TextFieldValue( text =  tag.name, selection = TextRange( tag.name.length) )
                searchTextDone.value = tag.name
            },
            modifier = modifier
        )
    }



    @Composable
    fun CustomBasicTextField(
        modifier: Modifier = Modifier,
    ) {
        val searchTagSuggestions by searchTextSuggestions.collectAsStateWithLifecycle()
        val historyItems by history.collectAsState()
        val text by searchText.collectAsStateWithLifecycle()

        CustomBasicTextFieldContent(
            modifier = modifier,
            value = text,
            onValueChange = { searchText.value = it },
            suggestions = { searchTagSuggestions },
            onSuggestionClick = { suggestion ->
                searchText.value = TextFieldValue( text = suggestion.text, selection = TextRange(suggestion.text.length) )
                searchTextDone.value = suggestion.text

                scope.launch(Dispatchers.Main) {
                    if ( suggestion.text.isNotEmpty() ) {
                        add(suggestion.text)
                        stack.addLast(suggestion.text)
                    }
                }

            },
            onClearClick = {
                searchText.value = TextFieldValue( text = "", selection = TextRange("".length) )
            },
            onUndoClick = {
                if (stack.isNotEmpty()) {
                    val last = stack.removeLast()
                    searchText.value = TextFieldValue( text = last, selection = TextRange(last.length) )
                }
            },
            onDone = {
                searchTextDone.value = it
                scope.launch(Dispatchers.Main) {
                    if ( it.isNotEmpty() ) {
                        add(it)
                        stack.addLast(it)
                    }
                }
            },
            expandMenuHistory = {
                ExpandMenuHistoryContent(items = { historyItems },
                    onClick = {
                        searchText.value = TextFieldValue( text = it, selection = TextRange(it.length) )
                        searchTextDone.value = it },
                    onDeleteClick = { scope.launch(Dispatchers.Main) { delete(it) } }
                )
            },
            onFocused = {
                focused.value = it
            }
        )

    }

    val history: StateFlow<List<String>> = dao.observeAllTexts().stateIn( scope = scope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList() )

    suspend fun add(text: String ) = dao.insertAndTrim(text)
    suspend fun delete( text: String ) = dao.deleteByTexts(text)
    suspend fun clear() = dao.deleteAll()

}
