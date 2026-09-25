package com.client.xvideos.screenSettings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.settings.ThumbnailsSize

private val SELECTOR_HORIZONTAL_PADDING = 72.dp
private val SELECTOR_VERTICAL_PADDING = 4.dp
private val SELECTOR_BOX_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(horizontal = SELECTOR_HORIZONTAL_PADDING, vertical = SELECTOR_VERTICAL_PADDING)

@Composable
fun ThumbnailSizeSelector(
    currentValue: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val items = ThumbnailsSize.displayNames

    val onOpen = remember { { expanded = true } }
    val onDismiss = remember { { expanded = false } }

    Box(modifier = modifier.then(SELECTOR_BOX_MODIFIER)) {
        Button(onClick = onOpen) {
            Text(currentValue)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            items.forEach { name ->
                key(name) {
                    val handleSelect = remember(name, onSelected) {
                        {
                            onSelected(name)
                            expanded = false
                        }
                    }
                    val itemContent: @Composable () -> Unit = remember(name) { { Text(name) } }
                    DropdownMenuItem(
                        text = itemContent,
                        onClick = handleSelect
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun ThumbnailSizeSelectorPreview() = SettingsPreview {
    ThumbnailSizeSelector(
        currentValue = "Medium",
        onSelected = {}
    )
}
