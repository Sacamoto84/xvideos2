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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.SavedAlbumFilter
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

@Composable
fun SavedPresetCard(
    preset: SavedAlbumFilter,
    dateStr: String,
    summary: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val cardShape = RoundedCornerShape(8.dp)
    val titleStyle = remember { Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.Bold) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(1.dp, palette.border, cardShape)
            .background(palette.field)
            .clickable(onClick = onSelect)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete preset",
                    tint = palette.excludedBorder,
                    modifier = Modifier.size(20.dp)
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
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.accent, RoundedCornerShape(6.dp))
                    .background(palette.accentDark)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Apply",
                    color = Color.White,
                    style = Theme.L.Type.button
                )
            }
        }
    }
}

@Preview
@Composable
private fun SavedPresetCardPreview() {
    SavedPresetCard(
        preset = SavedAlbumFilter(id = "1", name = "My Preset", filter = AlbumListFilter(), createdAt = 1000L),
        dateStr = "1 Jan, 12:00",
        summary = "Manga, Hentai, 50-100 pics",
        onSelect = {},
        onDelete = {}
    )
}
