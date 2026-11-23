package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
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
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.r.common.downloader.DownloadSort

internal enum class DownloadView {
    DETAILS,
    GRID,
}

private data class DownloadMenuOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector,
)

private val viewOptions = listOf(
    DownloadMenuOption(DownloadView.DETAILS, "Подробный список", Icons.Outlined.ViewAgenda),
    DownloadMenuOption(DownloadView.GRID, "Сетка", Icons.Outlined.GridView),
)

private val sortOptions = listOf(
    DownloadMenuOption(DownloadSort.DATE, "По дате (сначала новые)", Icons.Outlined.CalendarToday),
    DownloadMenuOption(DownloadSort.SIZE_ASCENDING, "По размеру (сначала меньшие)", Icons.Outlined.ArrowUpward),
    DownloadMenuOption(DownloadSort.SIZE_DESCENDING, "По размеру (сначала большие)", Icons.Outlined.ArrowDownward),
)

@Composable
internal fun DownloadViewMenu(view: DownloadView, onViewChange: (DownloadView) -> Unit) {
    DownloadOptionsMenu(
        title = "Вид загрузок",
        icon = viewOptions.first { it.value == view }.icon,
        options = viewOptions,
        selected = view,
        onSelect = onViewChange,
    )
}

@Composable
internal fun DownloadSortMenu(sort: DownloadSort, onSortChange: (DownloadSort) -> Unit) {
    DownloadOptionsMenu(
        title = "Сортировка загрузок",
        icon = Icons.AutoMirrored.Filled.Sort,
        options = sortOptions,
        selected = sort,
        onSelect = onSortChange,
    )
}

@Composable
private fun <T> DownloadOptionsMenu(
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
