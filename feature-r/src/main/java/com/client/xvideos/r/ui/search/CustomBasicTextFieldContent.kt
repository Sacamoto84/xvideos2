package com.client.xvideos.r.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.common.search.SuggestionItem
import com.client.xvideos.r.ui.search.molecule.SearchInputRow
import com.client.xvideos.r.ui.search.molecule.SuggestionList
import kotlinx.coroutines.delay

/**
 * Оптимизированный Stateless компонент поисковой строки.
 */
@Composable
fun CustomBasicTextFieldContent(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    suggestions: () -> List<SuggestionItem>,
    onSuggestionClick: (SuggestionItem) -> Unit,
    onClearClick: () -> Unit,
    onUndoClick: () -> Unit,
    onDone: (String) -> Unit,
    modifier: Modifier = Modifier,
    expandMenuHistory: @Composable () -> Unit,
    onFocused: (Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    // Авто-сброс фокуса при закрытии клавиатуры
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (!imeVisible && isFocused) focusManager.clearFocus()
    }

    // Задержка появления подсказок для плавности
    var showSuggestions by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) {
        if (isFocused) delay(300) else delay(100)
        showSuggestions = isFocused

        onFocused(isFocused)
    }

    Column(
        modifier = modifier
            .padding(top = if (isFocused) 4.dp else 0.dp)
            .fillMaxWidth()
            .background(Theme.background, RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Theme.R.colorBorderSelect else Theme.R.colorBorderGray,
                shape = RoundedCornerShape(8.dp)
            ),
    ) {
        AnimatedVisibility(
            visible = showSuggestions && suggestions().isNotEmpty(),
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(tween(400)),
            exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(tween(400)),
        ) {
            SuggestionList(
                suggestions = suggestions,
                query = value.text,
                onSuggestionClick = onSuggestionClick
            )
        }

        SearchInputRow(
            value = value,
            onValueChange = onValueChange,
            onFocusChanged = { isFocused = it },
            onClearClick = onClearClick,
            onUndoClick = onUndoClick,
            onDone = {
                onDone(it)
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            expandMenuHistory = expandMenuHistory
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun CustomBasicTextFieldContentPreview() {
    CustomBasicTextFieldContent(
        value = TextFieldValue("search"),
        onValueChange = {},
        suggestions = { emptyList() },
        onSuggestionClick = {},
        onClearClick = {},
        onUndoClick = {},
        onDone = {},
        expandMenuHistory = {},
        onFocused = {}
    )
}
