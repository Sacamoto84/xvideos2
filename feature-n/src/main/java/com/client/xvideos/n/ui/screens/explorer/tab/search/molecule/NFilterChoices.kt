package com.client.xvideos.n.ui.screens.explorer.tab.search.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.n.ui.screens.explorer.tab.search.atom.NFilterChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> NFilterChoices(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Color.LightGray)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, label) ->
                NFilterChip(label, value == selected, onClick = { onSelect(value) })
            }
        }
    }
}

@Preview
@Composable
private fun NFilterChoicesPreview() {
    NFilterChoices(
        title = "Язык перевода",
        options = listOf("" to "Все", "english" to "English", "russian" to "Русский"),
        selected = "english",
        onSelect = {},
    )
}
