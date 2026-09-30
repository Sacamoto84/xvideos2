package com.client.xvideos.x.screens.favorites.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.core.HorizontalSeparator

@Composable
fun FavoritesHeader(
    topCutout: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topCutout)
    ) {
        Text(
            "Избранное",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        HorizontalSeparator(color = Color(0xFF9E9E9E))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun FavoritesHeaderPreview() {
    FavoritesHeader(topCutout = 0.dp)
}
