package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.Genre

@Composable
fun GenreChip(
    item: Genre,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipShape = RoundedCornerShape(4.dp)
    Text(
        text = item.title,
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .clip(chipShape)
            .border(1.dp, Theme.L.secondaryColor, chipShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        color = Theme.L.primaryColor,
        style = style
    )
}

@Preview
@Composable
private fun GenreChipPreview() {
    GenreChip(
        item = Genre(id = "1", title = "Action", url = "url"),
        style = Theme.L.Type.rowValue,
        onClick = {}
    )
}
