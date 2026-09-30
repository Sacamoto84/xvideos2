package com.client.xvideos.r.ui.explorer.tab.search.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SearchTopBar(
    searchText: String,
    isLoading: Boolean,
    onSearchTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClearSearch: () -> Unit = remember(onSearchTextChange) {
        { onSearchTextChange("") }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск авторов...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Поиск", tint = Color.Gray)
            },
            trailingIcon = {
                if (searchText.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить", tint = Color.Gray)
                    }
                }
            },
            singleLine = true
        )
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = Color(0xFFE5A00D)
            )
        }
    }
}

@Preview
@Composable
private fun SearchTopBarPreview() {
    SearchTopBar(
        searchText = "Ana",
        isLoading = false,
        onSearchTextChange = {}
    )
}
