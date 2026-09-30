package com.client.xvideos.r.ui.search.molecule

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.search.atom.HistoryMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandMenuHistoryContentStateless(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<String>,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {}
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        IconButton(
            modifier = Modifier
                .height(46.dp)
                .width(46.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = { onExpandedChange(!expanded) }
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "История поиска",
                tint = if (expanded) Color.White else Color(0xFF757575),
                modifier = Modifier.size(30.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = Theme.R.colorBottomBarDivider,
            shadowElevation = 8.dp
        ) {
            val reversedItems = remember(items) { items.reversed() }
            reversedItems.forEach { item ->
                HistoryMenuItem(
                    text = item,
                    onClick = { onClick(item) },
                    onDeleteClick = { onDeleteClick(item) }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ExpandMenuHistoryContentStatelessPreview() {
    ExpandMenuHistoryContentStateless(
        expanded = false,
        onExpandedChange = {},
        items = listOf("Query 1", "Query 2")
    )
}
