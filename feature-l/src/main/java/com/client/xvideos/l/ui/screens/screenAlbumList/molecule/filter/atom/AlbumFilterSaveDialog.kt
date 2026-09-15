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

@Composable
fun AlbumFilterSaveDialog(
    filter: AlbumListFilter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = StyleGenresTags.Palette
    var presetName by remember {
        mutableStateOf(AlbumFilterPresetManager.generateDefaultName(filter))
    }
    val summary = remember(filter) { AlbumFilterPresetManager.formatFilterSummary(filter) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                .background(palette.surface)
                .padding(16.dp)
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.panelBlack)
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Preset name:",
                    color = palette.textPrimary,
                    style = Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .background(palette.field)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        singleLine = true,
                        textStyle = Theme.L.Type.rowValue.copy(color = palette.textPrimary),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = palette.textPrimary,
            style = Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
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
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (canSave) palette.accent else palette.border, RoundedCornerShape(8.dp))
                .background(if (canSave) palette.accentDark else palette.field)
                .clickable(enabled = canSave, onClick = onSave)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Save",
                color = if (canSave) Color.White else palette.textSecondary,
                style = Theme.L.Type.button.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
