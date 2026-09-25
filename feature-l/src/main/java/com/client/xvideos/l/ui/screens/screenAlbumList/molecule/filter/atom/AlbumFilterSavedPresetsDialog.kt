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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.SavedAlbumFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DIALOG_SHAPE = RoundedCornerShape(16.dp)
private val PRESET_LIST_SHAPE = RoundedCornerShape(8.dp)
private val EMPTY_STATE_SHAPE = RoundedCornerShape(8.dp)
private val PRESET_CARD_SHAPE = RoundedCornerShape(8.dp)
private val APPLY_BUTTON_SHAPE = RoundedCornerShape(6.dp)

private const val DIALOG_WIDTH_FRACTION = 0.92f
private val DIALOG_MAX_WIDTH = 460.dp
private val DIALOG_PADDING = 16.dp
private val DIALOG_BORDER_WIDTH = 1.dp
private val EMPTY_STATE_HEIGHT = 140.dp
private val EMPTY_STATE_PADDING = 16.dp
private val CARD_PADDING = 10.dp
private val DELETE_BUTTON_SIZE = 36.dp
private val DELETE_ICON_SIZE = 20.dp
private val APPLY_HORIZONTAL_PADDING = 12.dp
private val APPLY_VERTICAL_PADDING = 5.dp

private const val CD_CLOSE = "Close"
private const val CD_DELETE_PRESET = "Delete preset"
private const val TEXT_APPLY = "Apply"
private const val TEXT_EMPTY_PRESETS = "No saved presets yet.\nConfigure filters and tap 'Save'."
private const val CONTENT_TYPE_PRESET_ITEM = "saved_preset_card"

private val ROW_ARRANGEMENT_SPACE_BETWEEN = Arrangement.SpaceBetween
private val ROW_VERTICAL_ALIGNMENT_CENTER = Alignment.CenterVertically
private val ROW_ARRANGEMENT_END = Arrangement.End
private val BOX_ALIGNMENT_CENTER = Alignment.Center
private val LAZY_COLUMN_SPACED_BY_6 = Arrangement.spacedBy(6.dp)
private val LAZY_COLUMN_PADDING = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 4.dp)
private val DIALOG_PROPERTIES = DialogProperties(usePlatformDefaultWidth = false)
private val COLOR_WHITE = androidx.compose.ui.graphics.Color.White
private val TEXT_ALIGN_CENTER = androidx.compose.ui.text.style.TextAlign.Center

@Composable
fun AlbumFilterSavedPresetsDialog(
    onSelectPreset: (AlbumListFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = StyleGenresTags.Palette
    val configuration = LocalConfiguration.current
    val maxListHeight = (configuration.screenHeightDp * 0.6f).dp.coerceIn(240.dp, 520.dp)

    val presets by AlbumFilterPresetManager.presets.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val headerStyle = remember(palette.textPrimary) {
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
                .clip(DIALOG_SHAPE)
                .border(DIALOG_BORDER_WIDTH, palette.border, DIALOG_SHAPE)
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
                        text = "Saved Filters (${presets.size})",
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
                            contentDescription = CD_CLOSE,
                            tint = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (presets.isEmpty()) {
                    SavedPresetsEmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxListHeight)
                            .clip(PRESET_LIST_SHAPE)
                            .border(DIALOG_BORDER_WIDTH, palette.border, PRESET_LIST_SHAPE)
                            .background(palette.panelBlack)
                            .padding(LAZY_COLUMN_PADDING),
                        verticalArrangement = LAZY_COLUMN_SPACED_BY_6
                    ) {
                        items(
                            items = presets,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PRESET_ITEM }
                        ) { preset ->
                            val summary = AlbumFilterPresetManager.formatFilterSummary(preset.filter)
                            val dateStr = dateFormat.format(Date(preset.createdAt))

                            SavedPresetCard(
                                preset = preset,
                                dateStr = dateStr,
                                summary = summary,
                                onSelect = {
                                    onSelectPreset(preset.filter)
                                    onDismiss()
                                },
                                onDelete = {
                                    AlbumFilterPresetManager.deletePreset(context, preset.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPresetsEmptyState() {
    val palette = StyleGenresTags.Palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(EMPTY_STATE_HEIGHT)
            .clip(EMPTY_STATE_SHAPE)
            .background(palette.panelBlack)
            .padding(EMPTY_STATE_PADDING),
        contentAlignment = BOX_ALIGNMENT_CENTER
    ) {
        Text(
            text = TEXT_EMPTY_PRESETS,
            color = palette.textSecondary,
            style = Theme.L.Type.rowTitle,
            textAlign = TEXT_ALIGN_CENTER
        )
    }
}

@Composable
private fun SavedPresetCard(
    preset: SavedAlbumFilter,
    dateStr: String,
    summary: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val titleStyle = remember { Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.Bold) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PRESET_CARD_SHAPE)
            .border(DIALOG_BORDER_WIDTH, palette.border, PRESET_CARD_SHAPE)
            .background(palette.field)
            .clickable(onClick = onSelect)
            .padding(CARD_PADDING)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = ROW_ARRANGEMENT_SPACE_BETWEEN,
            verticalAlignment = ROW_VERTICAL_ALIGNMENT_CENTER
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    color = palette.textPrimary,
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateStr,
                    color = palette.textSecondary,
                    style = Theme.L.Type.rowSubtitle
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(DELETE_BUTTON_SIZE)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = CD_DELETE_PRESET,
                    tint = palette.excludedBorder,
                    modifier = Modifier.size(DELETE_ICON_SIZE)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = summary,
            color = palette.textSecondary,
            style = Theme.L.Type.rowSubtitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = ROW_ARRANGEMENT_END
        ) {
            Box(
                modifier = Modifier
                    .clip(APPLY_BUTTON_SHAPE)
                    .border(DIALOG_BORDER_WIDTH, palette.accent, APPLY_BUTTON_SHAPE)
                    .background(palette.accentDark)
                    .padding(horizontal = APPLY_HORIZONTAL_PADDING, vertical = APPLY_VERTICAL_PADDING)
            ) {
                Text(
                    text = TEXT_APPLY,
                    color = COLOR_WHITE,
                    style = Theme.L.Type.button
                )
            }
        }
    }
}
