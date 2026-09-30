package com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.molecule

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.IncognitoKeyboard

@Composable
fun AlbumSearchInputField(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val handleClear = remember(onSearchTextChange) { { onSearchTextChange("") } }
    val handleSearch: () -> Unit = remember(onSearch, keyboard) {
        {
            onSearch()
            keyboard?.hide()
        }
    }
    val keyboardActions = remember(handleSearch) { KeyboardActions(onSearch = { handleSearch() }) }
    val searchTextStyle = remember { Theme.L.Type.body.copy(color = Theme.L.textColor) }

    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        singleLine = true,
        label = { Text("Search") },
        textStyle = searchTextStyle,
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = handleClear) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить поле поиска", tint = Theme.L.textColor)
                }
            } else {
                IconButton(onClick = handleSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Искать", tint = Theme.L.textColor)
                }
            }
        },
        keyboardOptions = IncognitoKeyboard.options(imeAction = ImeAction.Search),
        keyboardActions = keyboardActions
    )
}

@Preview
@Composable
private fun AlbumSearchInputFieldPreview() {
    AlbumSearchInputField(
        searchText = "Sample query",
        onSearchTextChange = {},
        onSearch = {}
    )
}
