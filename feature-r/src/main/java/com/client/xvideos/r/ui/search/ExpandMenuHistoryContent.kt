package com.client.xvideos.r.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.ui.search.molecule.ExpandMenuHistoryContentStateless

/**
 * Stateful version of the history menu.
 */
@Composable
fun ExpandMenuHistoryContent(
    items: () -> List<String>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    ExpandMenuHistoryContentStateless(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        items = items(),
        modifier = modifier,
        onClick = {
            onClick(it)
            expanded = false
        },
        onDeleteClick = onDeleteClick
    )
}

@Preview
@Composable
private fun ExpandMenuHistoryContentPreview() {
    ExpandMenuHistoryContent(
        items = { listOf("Query 1", "Query 2") }
    )
}
