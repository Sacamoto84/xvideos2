package com.client.xvideos.x.screens.history.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.composables.core.HorizontalSeparator

/**
 * Верхняя панель экрана истории (обычный режим и режим множественного выбора).
 */
@Composable
fun HistoryTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    hasItems: Boolean,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onDeleteBatch: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.L.grey6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Отмена",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = "Выбрано: $selectedCount",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleSelectAll) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                            contentDescription = if (allSelected) "Снять выбор" else "Выбрать все",
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = onDeleteBatch,
                        enabled = selectedCount > 0,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить выбранные",
                            tint = if (selectedCount > 0) Color(0xFFFF5252) else Color.DarkGray,
                        )
                    }
                }
            } else {
                Text(
                    text = "История",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                )
                if (hasItems) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEnterSelectionMode) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Выбрать",
                                tint = Color.LightGray,
                            )
                        }
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Очистить всё",
                                tint = Color.LightGray,
                            )
                        }
                    }
                }
            }
        }
        HorizontalSeparator(color = Color(0xFF9E9E9E))
    }
}

@Preview
@Composable
private fun HistoryTopBarPreview() {
    HistoryTopBar(
        isSelectionMode = false,
        selectedCount = 0,
        allSelected = false,
        hasItems = true,
        onEnterSelectionMode = {},
        onExitSelectionMode = {},
        onToggleSelectAll = {},
        onDeleteBatch = {},
        onClearAll = {},
    )
}
