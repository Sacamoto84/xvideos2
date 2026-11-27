package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun <T> NTagCatalogMenu(
    description: String,
    selected: String,
    options: List<Pair<T, String>>,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$description, $selected" }
        ) {
            Text(selected, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (item, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelect(item)
                    expanded = false
                })
            }
        }
    }
}

@Preview
@Composable
private fun NTagCatalogMenuPreview() {
    NTagCatalogMenu(
        description = "Сортировка",
        selected = "Популярные",
        options = listOf("popular" to "Популярные", "name" to "По имени"),
        onSelect = {},
    )
}
