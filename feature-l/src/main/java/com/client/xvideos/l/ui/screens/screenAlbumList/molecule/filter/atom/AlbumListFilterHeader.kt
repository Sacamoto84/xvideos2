package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

@Composable
fun AlbumListFilterHeader(
    presetsCount: Int,
    onSaveClick: () -> Unit,
    onSavedPresetsClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val buttonShape = RoundedCornerShape(6.dp)
    val headerTitleStyle = remember(palette.textPrimary) {
        Theme.L.Type.screenTitle.copy(fontWeight = FontWeight.Bold)
    }
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Filters",
            color = palette.textPrimary,
            style = headerTitleStyle
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Save button
            Row(
                modifier = Modifier
                    .clip(buttonShape)
                    .border(1.dp, palette.border, buttonShape)
                    .background(palette.field)
                    .clickable { onSaveClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save filter preset",
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Save",
                    color = palette.textPrimary,
                    style = Theme.L.Type.button
                )
            }

            // Saved presets button
            Row(
                modifier = Modifier
                    .clip(buttonShape)
                    .border(1.dp, palette.border, buttonShape)
                    .background(palette.field)
                    .clickable { onSavedPresetsClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Saved presets",
                    tint = palette.selectedBorder,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Saved ($presetsCount)",
                    color = palette.textPrimary,
                    style = Theme.L.Type.button
                )
            }

            // Close button (X)
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close filters",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlbumListFilterHeaderPreview() {
    AlbumListFilterHeader(
        presetsCount = 3,
        onSaveClick = {},
        onSavedPresetsClick = {},
        onClose = {}
    )
}
