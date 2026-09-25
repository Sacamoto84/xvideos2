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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumListFilter

private val DIALOG_SHAPE_16 = RoundedCornerShape(16.dp)
private val BUTTON_SHAPE_8 = RoundedCornerShape(8.dp)
private val SUMMARY_SHAPE_6 = RoundedCornerShape(6.dp)
private val BORDER_WIDTH_1 = 1.dp
private val DIALOG_PADDING = 16.dp
private const val DIALOG_WIDTH_FRACTION = 0.92f
private val DIALOG_MAX_WIDTH = 420.dp
private val ROW_VERTICAL_ALIGNMENT_CENTER = Alignment.CenterVertically
private val ROW_ARRANGEMENT_SPACE_BETWEEN = Arrangement.SpaceBetween
private val ROW_ARRANGEMENT_END = Arrangement.End
private val BOX_ALIGNMENT_CENTER = Alignment.Center
private val BOX_ALIGNMENT_CENTER_START = Alignment.CenterStart
private val DIALOG_PROPERTIES = DialogProperties(usePlatformDefaultWidth = false)

@Composable
fun AlbumFilterSaveDialog(
    filter: AlbumListFilter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = StyleGenresTags.Palette
    var presetName by remember(filter) {
        mutableStateOf(AlbumFilterPresetManager.generateDefaultName(filter))
    }
    val summary = remember(filter) { AlbumFilterPresetManager.formatFilterSummary(filter) }
    val titleStyle = remember { Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.Bold) }
    val rowValueStyle = remember(palette.textPrimary) { Theme.L.Type.rowValue.copy(color = palette.textPrimary) }
    val placeholderStyle = remember(palette.textSecondary) {
        Theme.L.Type.rowValue.copy(color = palette.textSecondary.copy(alpha = 0.5f))
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
            Column(modifier = Modifier.fillMaxWidth()) {
                SaveDialogHeader(
                    title = "Save Filter Preset",
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = summary,
                    color = palette.textSecondary,
                    style = Theme.L.Type.rowSubtitle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SUMMARY_SHAPE_6)
                        .background(palette.panelBlack)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Preset name:",
                    color = palette.textPrimary,
                    style = titleStyle
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(BUTTON_SHAPE_8)
                        .border(BORDER_WIDTH_1, palette.border, BUTTON_SHAPE_8)
                        .background(palette.field)
                        .padding(horizontal = 10.dp),
                    contentAlignment = BOX_ALIGNMENT_CENTER_START
                ) {
                    if (presetName.isEmpty()) {
                        Text(
                            text = "Preset name",
                            color = palette.textSecondary.copy(alpha = 0.5f),
                            style = placeholderStyle
                        )
                    }
                    BasicTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        singleLine = true,
                        textStyle = rowValueStyle,
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                SaveDialogActions(
                    canSave = presetName.isNotBlank(),
                    onCancel = onDismiss,
                    onSave = {
                        AlbumFilterPresetManager.savePreset(context, presetName, filter)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SaveDialogHeader(
    title: String,
    onDismiss: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val headerStyle = remember(palette.textPrimary) {
        Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold)
    }
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
}

@Composable
private fun SaveDialogActions(
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val saveButtonStyle = remember { Theme.L.Type.button.copy(fontWeight = FontWeight.Bold) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = ROW_ARRANGEMENT_END,
        verticalAlignment = ROW_VERTICAL_ALIGNMENT_CENTER
    ) {
        Box(
            modifier = Modifier
                .clip(BUTTON_SHAPE_8)
                .clickable(onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = BOX_ALIGNMENT_CENTER
        ) {
            Text(
                text = "Cancel",
                color = palette.textSecondary,
                style = Theme.L.Type.button
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(BUTTON_SHAPE_8)
                .border(BORDER_WIDTH_1, if (canSave) palette.accent else palette.border, BUTTON_SHAPE_8)
                .background(if (canSave) palette.accentDark else palette.field)
                .clickable(enabled = canSave, onClick = onSave)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = BOX_ALIGNMENT_CENTER
        ) {
            Text(
                text = "Save",
                color = if (canSave) Color.White else palette.textSecondary,
                style = saveButtonStyle
            )
        }
    }
}
