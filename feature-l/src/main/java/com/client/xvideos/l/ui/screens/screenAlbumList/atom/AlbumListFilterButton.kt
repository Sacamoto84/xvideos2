package com.client.xvideos.l.ui.screens.screenAlbumList.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip

@Composable
internal fun AlbumListFilterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = "Фильтры"
    HintTooltip(text = hint, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(2.dp, Color(0xFF434343), RoundedCornerShape(4.dp))
                .background(Color(0xFF414141))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FilterList, contentDescription = hint, tint = Color.White)
        }
    }
}

@Preview
@Composable
private fun AlbumListFilterButtonPreview() {
    AlbumListFilterButton(onClick = {})
}
