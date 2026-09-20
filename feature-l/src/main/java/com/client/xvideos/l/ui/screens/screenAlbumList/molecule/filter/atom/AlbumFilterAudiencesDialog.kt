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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.client.xvideos.l.net.graphQl.Audience

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

@Composable
private fun AudienceDialogRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    val borderColor = if (isSelected) palette.selectedBorder else Color.Transparent
    val backgroundColor = if (isSelected) palette.selected else Color.Transparent
    val textColor = if (isSelected) palette.selectedText else palette.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = Theme.L.Type.rowTitle.copy(
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
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
