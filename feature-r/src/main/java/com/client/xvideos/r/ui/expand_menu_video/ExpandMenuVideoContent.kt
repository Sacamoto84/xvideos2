package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandMenuVideoContent(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.then(modifier)
    ) {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = {}
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Действия с публикацией",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = Theme.ExpandMenu.backgroundColor,
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun ExpandMenuVideoContentPreview() {
    ExpandMenuVideoContent(
        expanded = true,
        onExpandedChange = {},
        onDismissRequest = {}
    ) {
        DropdownMenuItem(text = { Text("Download") }, onClick = {})
        DropdownMenuItem(text = { Text("Share") }, onClick = {})
        DropdownMenuItem(text = { Text("Block") }, onClick = {})
        DropdownMenuItem(text = { Text("Like") }, onClick = {})
        DropdownMenuItem(text = { Text("Подписаться") }, onClick = {})
        DropdownMenuItem(text = { Text("Add to Collection") }, onClick = {})
    }
}
