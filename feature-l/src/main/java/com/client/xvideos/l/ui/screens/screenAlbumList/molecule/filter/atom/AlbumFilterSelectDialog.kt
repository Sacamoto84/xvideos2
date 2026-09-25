package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.theme.Theme

private val DIALOG_SHAPE_16 = RoundedCornerShape(16.dp)
private val LIST_SHAPE_8 = RoundedCornerShape(8.dp)
private val ROW_SHAPE_6 = RoundedCornerShape(6.dp)
private val BORDER_WIDTH_1 = 1.dp
private const val DIALOG_WIDTH_FRACTION = 0.9f
private val DIALOG_MAX_WIDTH = 420.dp
private val DIALOG_PADDING = 16.dp
private val ROW_VERTICAL_ALIGNMENT_CENTER = Alignment.CenterVertically
private val ROW_ARRANGEMENT_SPACE_BETWEEN = Arrangement.SpaceBetween
private val DIALOG_PROPERTIES = DialogProperties(usePlatformDefaultWidth = false)

@Composable
fun <T> AlbumFilterSelectDialog(
    title: String,
    items: List<T>,
    selectedItem: T?,
    itemTitle: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    val palette = StyleGenresTags.Palette
    val configuration = LocalConfiguration.current
    val maxListHeight = (configuration.screenHeightDp * 0.6f).dp.coerceIn(240.dp, 480.dp)
    val headerStyle = remember(palette.textPrimary) {
        Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold)
    }

    val selectedIndex = items.indexOf(selectedItem)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (selectedIndex > 0) selectedIndex else 0
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DIALOG_PROPERTIES
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(DIALOG_WIDTH_FRACTION)
                .widthIn(max = DIALOG_MAX_WIDTH)
                .clip(DIALOG_SHAPE_16)
                .border(BORDER_WIDTH_1, palette.border, DIALOG_SHAPE_16)
                .background(palette.surface)
                .padding(DIALOG_PADDING)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Заголовок и кнопка закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = ROW_ARRANGEMENT_SPACE_BETWEEN,
                    verticalAlignment = ROW_VERTICAL_ALIGNMENT_CENTER
                ) {
                    Text(
                        text = title,
                        color = palette.textPrimary,
                        style = headerStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Список вариантов
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .clip(LIST_SHAPE_8)
                        .border(BORDER_WIDTH_1, palette.border, LIST_SHAPE_8)
                        .background(palette.panelBlack)
                        .padding(vertical = 4.dp)
                ) {
                    items(items, key = { itemTitle(it) }) { item ->
                        val isSelected = (item == selectedItem)
                        SelectDialogRow(
                            title = itemTitle(item),
                            isSelected = isSelected,
                            onClick = { onSelect(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectDialogRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val borderColor = if (isSelected) palette.selectedBorder else Color.Transparent
    val backgroundColor = if (isSelected) palette.selected else Color.Transparent
    val textColor = if (isSelected) palette.selectedText else palette.textPrimary
    val titleStyle = remember(textColor, isSelected) {
        Theme.L.Type.rowTitle.copy(
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .clip(ROW_SHAPE_6)
            .border(BORDER_WIDTH_1, borderColor, ROW_SHAPE_6)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = ROW_VERTICAL_ALIGNMENT_CENTER,
        horizontalArrangement = ROW_ARRANGEMENT_SPACE_BETWEEN
    ) {
        Text(
            text = title,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = titleStyle,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = palette.selectedBorder
            )
        }
    }
}
