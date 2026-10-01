package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter

import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.SavedPresetCard
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.SavedPresetsEmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.featured.filter.AlbumFilterPresetManager
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlbumFilterSavedPresetsDialog(
    onSelectPreset: (AlbumListFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = StyleGenresTags.Palette
    val configuration = LocalConfiguration.current
    val maxListHeight = (configuration.screenHeightDp * 0.6f).dp.coerceIn(240.dp, 520.dp)
    val dialogShape = RoundedCornerShape(16.dp)
    val cardShape = RoundedCornerShape(8.dp)

    val presets by AlbumFilterPresetManager.presets.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val headerStyle = remember(palette.textPrimary) {
        Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .clip(dialogShape)
                .border(1.dp, palette.border, dialogShape)
                .background(palette.surface)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                            contentDescription = "Close",
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
                            .clip(cardShape)
                            .border(1.dp, palette.border, cardShape)
                            .background(palette.panelBlack)
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = presets,
                            key = { it.id },
                            contentType = { "saved_preset_card" }
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

@Preview
@Composable
private fun AlbumFilterSavedPresetsDialogPreview() {
    AlbumFilterSavedPresetsDialog(
        onSelectPreset = {},
        onDismiss = {}
    )
}
