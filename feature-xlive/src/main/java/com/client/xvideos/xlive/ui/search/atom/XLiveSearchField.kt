package com.client.xvideos.xlive.ui.search.atom

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.common.ui.excludedFromAutofill
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * Поле поиска — вид поля поиска X: лупа слева ищет, крестик справа очищает,
 * «Найти» на клавиатуре тоже ищет. Перед поиском клавиатура прячется, чтобы
 * открыть сетку результата.
 */
@Composable
fun XLiveSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val submit = {
        keyboard?.hide()
        focus.clearFocus()
        onSearch()
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .excludedFromAutofill(),
        placeholder = { Text("Ник или слово из темы", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = {
            HintTooltip(text = "Найти") {
                IconButton(onClick = submit) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Найти", tint = XLiveColors.Accent)
                }
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                HintTooltip(text = "Очистить") {
                    IconButton(onClick = onClear) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Очистить", tint = Color.Gray)
                    }
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1C1C20),
            unfocusedContainerColor = Color(0xFF18181B),
            focusedBorderColor = XLiveColors.Accent,
            unfocusedBorderColor = Color(0xFF333338),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = XLiveColors.Accent,
        ),
        shape = RoundedCornerShape(12.dp),
    )
}

@Preview
@Composable
private fun XLiveSearchFieldPreview() {
    XLiveSearchField(query = "anna", onQueryChange = {}, onSearch = {}, onClear = {})
}
