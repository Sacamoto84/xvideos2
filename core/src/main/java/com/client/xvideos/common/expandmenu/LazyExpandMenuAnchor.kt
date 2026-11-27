package com.client.xvideos.common.expandmenu

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme.ExpandMenu.backgroundColor

/**
 * Кнопка «три точки» с выпадающим меню, которое монтируется только после
 * первого открытия.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyExpandMenuAnchor(
    modifier: Modifier = Modifier,
    menuWidth: IntrinsicSize = IntrinsicSize.Max,
    onOpen: () -> Unit = {},
    menuContent: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var mounted by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    if (!mounted) {
        IconButton(
            modifier = modifier.size(48.dp),
            onClick = {
                onOpen()
                mounted = true
            }
        ) { MenuDots() }
        return
    }

    // Открываем не в момент монтирования, а следующим кадром: якорю нужно
    // сначала встать на своё место, иначе всплывающее меню успевает мигнуть
    // в углу экрана, пока его координаты ещё не посчитаны.
    LaunchedEffect(Unit) { expanded = true }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (it) onOpen()
            expanded = it
        },
        modifier = modifier
    ) {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = {}
        ) { MenuDots() }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(menuWidth),
            containerColor = backgroundColor
        ) {
            menuContent { expanded = false }
        }
    }
}
