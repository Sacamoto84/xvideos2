package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter

import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.SaveDialogActions
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.SaveDialogHeader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.featured.filter.AlbumFilterPresetManager
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

@Composable
fun AlbumFilterSaveDialog(
    filter: AlbumListFilter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = StyleGenresTags.Palette
    val dialogShape = RoundedCornerShape(16.dp)
    val buttonShape = RoundedCornerShape(8.dp)

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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .clip(dialogShape)
                .border(1.dp, palette.border, dialogShape)
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
                    style = titleStyle
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(buttonShape)
                        .border(1.dp, palette.border, buttonShape)
                        .background(palette.field)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
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

@Preview
@Composable
private fun AlbumFilterSaveDialogPreview() {
    AlbumFilterSaveDialog(
        filter = AlbumListFilter(),
        onDismiss = {}
    )
}
