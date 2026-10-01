package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.style.StyleGenresTags

@Composable
fun SavedPresetsEmptyState(
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val cardShape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(cardShape)
            .background(palette.panelBlack)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No saved presets yet.\nConfigure filters and tap 'Save'.",
            color = palette.textSecondary,
            style = Theme.L.Type.rowTitle,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun SavedPresetsEmptyStatePreview() {
    SavedPresetsEmptyState()
}
