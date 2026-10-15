package com.client.xvideos.x.screens.favorites.atom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.screens.common.atom.SavedTabHeader

@Composable
fun FavoritesHeader(
    topCutout: Dp,
    modifier: Modifier = Modifier,
) {
    SavedTabHeader(title = "Избранное", topCutout = topCutout, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun FavoritesHeaderPreview() {
    FavoritesHeader(topCutout = 0.dp)
}
