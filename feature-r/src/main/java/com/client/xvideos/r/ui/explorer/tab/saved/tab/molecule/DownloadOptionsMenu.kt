package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.r.ui.explorer.tab.saved.tab.model.DownloadMenuOption
import com.client.xvideos.r.ui.explorer.tab.saved.tab.model.DownloadView

@Composable
internal fun <T> DownloadOptionsMenu(
    title: String,
    icon: ImageVector,
    options: List<DownloadMenuOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val tint = Theme.ExpandMenu.tintColor

    Box {
        HintTooltip(title) {
            IconButton(onClick = { expanded = true }) {
                Icon(icon, contentDescription = title, tint = Color.White)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Theme.ExpandMenu.backgroundColor,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, style = Theme.ExpandMenu.style) },
                    leadingIcon = { Icon(option.icon, contentDescription = null) },
                    trailingIcon = {
                        if (option.value == selected) {
                            Icon(Icons.Outlined.Check, contentDescription = "Выбран")
                        }
                    },
                    colors = MenuDefaults.itemColors(
                        leadingIconColor = tint,
                        trailingIconColor = tint,
                    ),
                    onClick = {
                        expanded = false
                        onSelect(option.value)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun DownloadOptionsMenuPreview() {
    DownloadOptionsMenu(
        title = "Вид",
        icon = Icons.Outlined.Check,
        options = listOf(DownloadMenuOption(DownloadView.DETAILS, "Список", Icons.Outlined.Check)),
        selected = DownloadView.DETAILS,
        onSelect = {},
    )
}
