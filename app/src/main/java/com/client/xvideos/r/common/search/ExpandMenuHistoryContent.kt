package com.client.xvideos.r.common.search

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.ui.theme.XvideosTheme

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

/**
 * Stateless version for better optimization and previews.
 */
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
            modifier = Modifier.height(46.dp).width(46.dp)
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

@Composable
private fun HistoryMenuItem(
    text: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {

        Box() {
            Text(
                text = text, color = Color.Black, fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 0.dp).padding(start = 16.dp).offset(0.75.dp, 0.75.dp),
                fontFamily = Theme.R.fontFamilyDMsanss
            )

            Text(
                text = text, color = Color.White, fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 0.dp).padding(start = 16.dp),
                fontFamily = Theme.R.fontFamilyDMsanss
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Удалить из истории",
                tint = Color.LightGray
            )
        }
    }
}

@Preview(name = "Collapsed State", showBackground = false, backgroundColor = 0xFF303030)
@Composable
fun PreviewExpandMenuHistoryCollapsed() {
    XvideosTheme {
        Surface(color = Theme.R.colorCommonBackground) {
            ExpandMenuHistoryContentStateless(
                expanded = false,
                onExpandedChange = {},
                items = listOf("Query 1", "Query 2")
            )
        }
    }
}

@Preview(name = "Expanded State", showBackground = true, backgroundColor = 0xFF303030)
@Composable
fun PreviewExpandMenuHistoryExpanded() {
    val sampleItems = listOf("Search Query 1", "Search Query 2", "Search Query 3")
    XvideosTheme {
        Surface(color = Theme.R.colorCommonBackground) {
            Column(modifier = Modifier.height(250.dp)) {
                ExpandMenuHistoryContentStateless(
                    expanded = true,
                    onExpandedChange = {},
                    items = sampleItems
                )
            }
        }
    }
}
