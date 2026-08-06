package com.client.xvideos.r.common.search

import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.TextFieldValue
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

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





    // Отрисовка переехала в ui/search/RSearchField.kt: класс держит состояние,
    // а composable принимают его параметром. Пока они были членами класса,
    // слой состояния тянул за собой Compose и три content-файла, и вынести те
    // в ui было нельзя — домен начал бы импортировать UI.

    val history: StateFlow<List<String>> = dao.observeAllTexts().stateIn( scope = scope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList() )

    suspend fun add(text: String ) = dao.insertAndTrim(text)
    suspend fun delete( text: String ) = dao.deleteByTexts(text)
    suspend fun clear() = dao.deleteAll()

}
