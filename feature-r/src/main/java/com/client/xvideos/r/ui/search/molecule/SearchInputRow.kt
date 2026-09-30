package com.client.xvideos.r.ui.search.molecule

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.IncognitoKeyboard
import com.client.xvideos.r.ui.search.atom.SearchIconButton

@Composable
fun SearchInputRow(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClearClick: () -> Unit,
    onUndoClick: () -> Unit,
    onDone: (String) -> Unit,
    expandMenuHistory: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(start = 12.dp, end = 4.dp)
            .height(46.dp)
    ) {
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
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            cursorBrush = SolidColor(Theme.R.colorYellow),
            keyboardOptions = IncognitoKeyboard.options(imeAction = ImeAction.Done, keyboardType = KeyboardType.Text),
            keyboardActions = KeyboardActions(onDone = { onDone(value.text) })
        )

        if (value.text.isNotEmpty()) {
            SearchIconButton(
                icon = Icons.Default.Clear,
                onClick = onClearClick,
                contentDescription = "Очистить поле поиска"
            )
        }

        expandMenuHistory()
    }
}

@Preview
@Composable
private fun SearchInputRowPreview() {
    SearchInputRow(
        value = TextFieldValue("search text"),
        onValueChange = {},
        onFocusChanged = {},
        onClearClick = {},
        onUndoClick = {},
        onDone = {},
        expandMenuHistory = {}
    )
}
