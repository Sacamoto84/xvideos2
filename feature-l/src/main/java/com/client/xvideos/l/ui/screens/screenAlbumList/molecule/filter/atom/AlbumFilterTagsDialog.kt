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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.theme.Theme

private val DIALOG_SHAPE_16 = RoundedCornerShape(16.dp)
private val LIST_SHAPE_8 = RoundedCornerShape(8.dp)
private val CHIP_SHAPE_6 = RoundedCornerShape(6.dp)
private val BORDER_WIDTH_1 = 1.dp
private const val DIALOG_WIDTH_FRACTION = 0.92f
private val DIALOG_MAX_WIDTH = 440.dp
private val DIALOG_PADDING = 16.dp
private val ROW_VERTICAL_ALIGNMENT_CENTER = Alignment.CenterVertically
private val ROW_ARRANGEMENT_SPACE_BETWEEN = Arrangement.SpaceBetween
private val CHIPS_SPACED_BY_6 = Arrangement.spacedBy(6.dp)
private val DIALOG_PROPERTIES = DialogProperties(usePlatformDefaultWidth = false)
private val ACTION_BUTTON_SIZE = 36.dp
private val ACTION_ICON_PADDING = 6.dp

@Composable
fun AlbumFilterTagsDialog(
    tagsPlus: List<String>,
    tagsMinus: List<String>,
    selectableTags: List<String>,
    tagCountByTerm: Map<String, Int>,
    onAddPlus: (String) -> Unit,
    onAddMinus: (String) -> Unit,
    onRemovePlus: (String) -> Unit,
    onRemoveMinus: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val configuration = LocalConfiguration.current
    val maxListHeight = (configuration.screenHeightDp * 0.6f).dp.coerceIn(240.dp, 520.dp)
    val headerStyle = androidx.compose.runtime.remember(palette.textPrimary) {
        Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold)
    }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = ROW_ARRANGEMENT_SPACE_BETWEEN,
                    verticalAlignment = ROW_VERTICAL_ALIGNMENT_CENTER
                ) {
                    Text(
                        text = "Tags",
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

                // Active selections (chips bar)
                if (tagsPlus.isNotEmpty() || tagsMinus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TagsSelectedChipsBar(
                        tagsPlus = tagsPlus,
                        tagsMinus = tagsMinus,
                        onRemovePlus = onRemovePlus,
                        onRemoveMinus = onRemoveMinus
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Available tags list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .clip(LIST_SHAPE_8)
                        .border(BORDER_WIDTH_1, palette.border, LIST_SHAPE_8)
                        .background(palette.panelBlack)
                        .padding(vertical = 4.dp)
                ) {
                    if (selectableTags.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All tags selected",
                                    color = palette.textSecondary,
                                    style = Theme.L.Type.rowTitle
                                )
                            }
                        }
                    }

                    itemsIndexed(selectableTags, key = { index, item -> "${item}#$index" }) { _, item ->
                        SelectableTagRow(
                            item = item,
                            count = tagCountByTerm[item],
                            onAddPlus = { onAddPlus(item) },
                            onAddMinus = { onAddMinus(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagsSelectedChipsBar(
    tagsPlus: List<String>,
    tagsMinus: List<String>,
    onRemovePlus: (String) -> Unit,
    onRemoveMinus: (String) -> Unit
) {
    val palette = StyleGenresTags.Palette
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = CHIPS_SPACED_BY_6
    ) {
        itemsIndexed(tagsPlus, key = { index, tag -> "plus_${tag}#$index" }) { _, tag ->
            Row(
                modifier = Modifier
                    .clip(CHIP_SHAPE_6)
                    .border(BORDER_WIDTH_1, palette.selectedBorder, CHIP_SHAPE_6)
                    .background(palette.selected)
                    .clickable { onRemovePlus(tag) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+ $tag",
                    color = palette.selectedText,
                    style = Theme.L.Type.rowValue.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = palette.selectedBorder,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        itemsIndexed(tagsMinus, key = { index, tag -> "minus_${tag}#$index" }) { _, tag ->
            Row(
                modifier = Modifier
                    .clip(CHIP_SHAPE_6)
                    .border(BORDER_WIDTH_1, palette.excludedBorder, CHIP_SHAPE_6)
                    .background(palette.excluded)
                    .clickable { onRemoveMinus(tag) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val annotatedTag = buildAnnotatedString {
                    withStyle(SpanStyle(color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) { append("NOT") }
                    append(" $tag")
                }
                Text(
                    text = annotatedTag,
                    color = palette.excludedText,
                    style = Theme.L.Type.rowValue.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = palette.excludedBorder,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectableTagRow(
    item: String,
    count: Int?,
    onAddPlus: () -> Unit,
    onAddMinus: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val titleStyle = androidx.compose.runtime.remember(palette.textPrimary) {
        Theme.L.Type.rowTitle.copy(
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
    val countStyle = androidx.compose.runtime.remember(palette.textSecondary) {
        Theme.L.Type.rowTitle.copy(
            color = palette.textSecondary,
            fontWeight = FontWeight.Bold
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = ROW_VERTICAL_ALIGNMENT_CENTER,
        horizontalArrangement = ROW_ARRANGEMENT_SPACE_BETWEEN
    ) {
        Row(
            verticalAlignment = ROW_VERTICAL_ALIGNMENT_CENTER,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Include $item",
                tint = palette.selectedBorder,
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .size(ACTION_BUTTON_SIZE)
                    .clip(CHIP_SHAPE_6)
                    .border(BORDER_WIDTH_1, palette.selectedBorder, CHIP_SHAPE_6)
                    .background(palette.field)
                    .clickable { onAddPlus() }
                    .padding(ACTION_ICON_PADDING)
            )

            Spacer(Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Exclude $item",
                tint = palette.excludedBorder,
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .size(ACTION_BUTTON_SIZE)
                    .clip(CHIP_SHAPE_6)
                    .border(BORDER_WIDTH_1, palette.excludedBorder, CHIP_SHAPE_6)
                    .background(palette.field)
                    .clickable { onAddMinus() }
                    .padding(ACTION_ICON_PADDING)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = item,
                color = palette.textPrimary,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (count != null && count >= 0) {
            Text(
                text = count.toString(),
                color = palette.textSecondary,
                style = countStyle,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}
