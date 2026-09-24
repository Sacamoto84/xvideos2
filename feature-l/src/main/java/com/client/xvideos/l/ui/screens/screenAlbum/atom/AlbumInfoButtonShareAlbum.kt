package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

private val SHARE_ALBUM_BUTTON_SHAPE = RoundedCornerShape(4.dp)
private const val TEXT_SHARE_ALBUM = "Share Album (P2P)"

/** Кнопка «поделиться альбомом по P2P» в шапке ScreenLAlbum. */
@Composable
fun AlbumInfoButtonShareAlbum(onClick: () -> Unit) {
    val buttonTextStyle = remember {
        Theme.L.Type.button.copy(color = Color.White)
    }

    Box(
        modifier = Modifier
            .padding(top = 2.dp, bottom = 4.dp)
            .height(46.dp)
            .fillMaxWidth()
            .clip(SHARE_ALBUM_BUTTON_SHAPE)
            .border(1.dp, Theme.L.grey3, SHARE_ALBUM_BUTTON_SHAPE)
            .background(Theme.L.grey6)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = TEXT_SHARE_ALBUM,
            color = Color.White,
            style = buttonTextStyle
        )
    }
}

@Preview
@Composable
fun AlbumInfoButtonShareAlbumPreview() {
    AlbumInfoButtonShareAlbum(onClick = {})
}
