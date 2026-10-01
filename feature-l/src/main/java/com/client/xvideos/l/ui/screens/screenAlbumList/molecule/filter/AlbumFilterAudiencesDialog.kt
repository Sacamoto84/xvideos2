package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter

import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom.AudienceDialogRow

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.net.graphQl.Audience
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

@Composable
fun AlbumFilterAudiencesDialog(
    audiences: List<Audience>,
    selectedIds: Set<String>,
    isAllSelected: Boolean,
    onToggleAudience: (Audience) -> Unit,
    onSelectAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val configuration = LocalConfiguration.current
    val maxListHeight = (configuration.screenHeightDp * 0.6f).dp.coerceIn(240.dp, 480.dp)
    val headerStyle = remember(palette.textPrimary) {
        Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold)
    }
    val dialogShape = RoundedCornerShape(16.dp)

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
                        text = "Audiences",
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

                // List of options
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .background(palette.panelBlack)
                        .padding(vertical = 4.dp)
                ) {
                    // Option "All audiences"
                    item {
                        AudienceDialogRow(
                            title = "All audiences",
                            isSelected = isAllSelected,
                            onClick = onSelectAll
                        )
                    }

                    // Individual audiences
                    itemsIndexed(audiences, key = { index, item -> "${item.id}#$index" }) { _, item ->
                        val isSelected = !isAllSelected && item.id in selectedIds
                        AudienceDialogRow(
                            title = item.title,
                            isSelected = isSelected,
                            onClick = { onToggleAudience(item) }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AlbumFilterAudiencesDialogPreview() {
    AlbumFilterAudiencesDialog(
        audiences = listOf(
            Audience(id = "1", title = "Straight"),
            Audience(id = "2", title = "Gay")
        ),
        selectedIds = setOf("1"),
        isAllSelected = false,
        onToggleAudience = {},
        onSelectAll = {},
        onDismiss = {}
    )
}
