package com.client.xvideos.r.common.search

import com.client.xvideos.common.theme.Theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.util.toPrettyCount2
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.delay

/**
 * Оптимизированный Stateless компонент поисковой строки.
 */
@Composable
fun CustomBasicTextFieldContent(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,

    suggestions: ()->List<SuggestionItem>,
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
        //Список
        AnimatedVisibility(
            visible = showSuggestions && suggestions().isNotEmpty(),
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(tween(400)),
            exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(tween(400)),
        ) {
            SuggestionList(
                suggestions = suggestions , query = value.text, onSuggestionClick = onSuggestionClick
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

@Composable
private fun SuggestionList(
    suggestions: () -> List<SuggestionItem>,
    query: String,
    onSuggestionClick: (SuggestionItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().height(130.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn( modifier = Modifier.weight(1f).fillMaxWidth() )
        {
            items(
                items = suggestions(),
                key = { it.text } // Ключ для оптимизации списка
            ) { suggestion ->
                SuggestionItem(
                    suggestion = suggestion,
                    query = query,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
        HorizontalDivider(
            color = Theme.R.colorBorderGray.copy(alpha = 0.5f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun SuggestionItem(
    suggestion: SuggestionItem,
    query: String,
    onClick: () -> Unit
) {
    val annotatedString = remember(suggestion.text, query) {
        buildAnnotatedString {
            val text = suggestion.text
            val startIndex = text.indexOf(query, ignoreCase = true)
            if (startIndex != -1 && query.isNotEmpty()) {
                append(text.substring(0, startIndex))
                withStyle(style = SpanStyle(color = Theme.R.colorYellow)) {
                    append(text.substring(startIndex, startIndex + query.length))
                }
                append(text.substring(startIndex + query.length))
            } else {
                append(text)
            }
        }
    }

    Row( modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 12.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween )
    {
        Text( text = annotatedString, fontFamily = Theme.R.fontFamilyDMsanss, fontSize = 18.sp, color = Color.White, maxLines = 1 )
        Text( text = suggestion.count.toPrettyCount2(), fontFamily = Theme.R.fontFamilyDMsanss, fontSize = 16.sp, color = Color.Gray, maxLines = 1 )
    }
}

@Composable
private fun SearchInputRow(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClearClick: () -> Unit,
    onUndoClick: () -> Unit,
    onDone: (String) -> Unit,
    expandMenuHistory: @Composable () -> Unit
) {
    Row( verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 4.dp).height(46.dp) )
    {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 18.sp,
                color = Color.White,
                fontFamily = Theme.R.fontFamilyDMsanss,
                textAlign = TextAlign.Left
            ),
            modifier = Modifier.weight(1f).onFocusChanged { onFocusChanged(it.isFocused) },
            cursorBrush = SolidColor(Theme.R.colorYellow),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Text ),
            keyboardActions = KeyboardActions(onDone = { onDone(value.text) })
        )

        if (value.text.isNotEmpty()) { SearchIconButton(icon = Icons.Default.Clear, onClick = onClearClick) }

        //SearchIconButton(icon = Icons.Default.Undo, onClick = onUndoClick)

        expandMenuHistory()

        //Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun SearchIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Icon( imageVector = icon, contentDescription = null, tint = Color(0xFF757575), modifier = Modifier.size(38.dp).padding(6.dp).clickable(onClick = onClick) )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun PreviewCustomBasicTextFieldContent() {
    val text = remember { TextFieldValue("big t") }

    val suggestions = listOf(
        SuggestionItem("big tits", 123456),
        SuggestionItem("big toys", 789),
        SuggestionItem("big thighs", 4567)
    )

    Box(Modifier.padding(16.dp)) {
        CustomBasicTextFieldContent(
            value = text,
            onValueChange = { },
            suggestions = { suggestions },
            onSuggestionClick = { },
            onClearClick = { },
            onUndoClick = {},
            onDone = {},
            expandMenuHistory = {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(38.dp).padding(6.dp)
                )
            },
            modifier = Modifier,
            onFocused = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun PreviewSuggestionList() {
    val suggestions = listOf(
        SuggestionItem("big tits", 123456),
        SuggestionItem("big toys" ,789),
        SuggestionItem("big thighs",4567)
    )
    XvideosTheme {
        SuggestionList(
            suggestions = { suggestions },
            query = "big",
            onSuggestionClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF212121)
@Composable
fun P_SuggestionItem() {
    val suggestion = SuggestionItem("big tits", 123456)
    XvideosTheme {
        SuggestionItem(
            suggestion = suggestion,
            query = "big",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF212121)
@Composable
fun PreviewSearchInputRow() {
    val text = remember { TextFieldValue("big t") }

    XvideosTheme {
        SearchInputRow(
            value = text,
            onValueChange = { },
            onFocusChanged = {},
            onClearClick = {  },
            onUndoClick = {},
            onDone = {},
            expandMenuHistory = {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(38.dp).padding(6.dp)
                )
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF212121)
@Composable
fun PreviewSearchIconButton() {
    XvideosTheme {
        SearchIconButton(
            icon = Icons.Default.Clear,
            onClick = {}
        )
    }
}
