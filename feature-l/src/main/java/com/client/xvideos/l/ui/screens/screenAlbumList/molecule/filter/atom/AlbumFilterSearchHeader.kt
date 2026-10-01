package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.style.StyleGenresTags

@Composable
fun AlbumFilterSearchHeader(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val cardShape = RoundedCornerShape(8.dp)
    val buttonShape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .padding(top = 8.dp)
            .clip(cardShape)
            .background(palette.surface)
            .border(1.dp, palette.border, cardShape)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(buttonShape)
                .border(1.dp, palette.border, buttonShape)
                .background(palette.field)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Search:",
                color = palette.textSecondary,
                style = Theme.L.Type.rowSubtitle
            )
            Spacer(Modifier.width(6.dp))
            Text(
                searchQuery,
                color = palette.textPrimary,
                style = Theme.L.Type.rowValue,
                maxLines = 1
            )
        }
    }
}

@Preview
@Composable
private fun AlbumFilterSearchHeaderPreview() {
    AlbumFilterSearchHeader(searchQuery = "Cosplay")
}
