package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun AlbumTagChip(
    label: String,
    onClick: () -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val borderColor = Theme.L.secondaryColor
    val chipBorderModifier = remember(borderColor) {
        val shape = RoundedCornerShape(4.dp)
        Modifier
            .padding(vertical = 2.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
    }
    val chipModifier = if (modifier == Modifier) chipBorderModifier else modifier.then(chipBorderModifier)

    Text(
        text = label,
        modifier = chipModifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        color = Theme.L.textColor,
        style = textStyle
    )
}

@Preview
@Composable
private fun AlbumTagChipPreview() {
    XvideosTheme {
        AlbumTagChip(
            label = "Nature (150)",
            onClick = {},
            textStyle = TextStyle(fontSize = 14.sp)
        )
    }
}
