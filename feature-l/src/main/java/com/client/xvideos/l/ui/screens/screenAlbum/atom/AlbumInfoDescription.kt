package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

/**
 * Описание альбома — последним блоком сведений, под тегами. Пустое не выводится.
 */
@Composable
fun AlbumInfoDescription(
    description: String,
    modifier: Modifier = Modifier
) {
    if (description.isBlank()) return

    val style = remember(Theme.L.Type.caption, Theme.L.textColor) {
        Theme.L.Type.caption.copy(color = Theme.L.textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    Text(
        text = description,
        style = style,
        modifier = modifier.padding(start = 2.dp, top = 4.dp, bottom = 4.dp)
    )
}

@Preview
@Composable
private fun AlbumInfoDescriptionPreview() {
    AlbumInfoDescription(description = "This is a sample album description.")
}
