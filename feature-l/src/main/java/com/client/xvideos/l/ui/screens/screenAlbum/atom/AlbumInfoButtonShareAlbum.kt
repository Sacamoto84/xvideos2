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
private val BUTTON_HEIGHT = 46.dp
private val BUTTON_TOP_PADDING = 2.dp
private val BUTTON_BOTTOM_PADDING = 4.dp
private val BUTTON_BORDER_WIDTH = 1.dp
private const val TEXT_SHARE_ALBUM = "Share Album (P2P)"
private val BOX_ALIGNMENT_CENTER = Alignment.Center
private val COLOR_WHITE = Color.White

private val SHARE_ALBUM_BUTTON_BASE_MODIFIER = Modifier
    .padding(top = BUTTON_TOP_PADDING, bottom = BUTTON_BOTTOM_PADDING)
    .height(BUTTON_HEIGHT)
    .fillMaxWidth()
    .clip(SHARE_ALBUM_BUTTON_SHAPE)

/** Кнопка «поделиться альбомом по P2P» в шапке ScreenLAlbum. */
@Composable
fun AlbumInfoButtonShareAlbum(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonTextStyle = remember(Theme.L.Type.button) {
        Theme.L.Type.button.copy(color = COLOR_WHITE)
    }
    val styledBase = remember(Theme.L.grey3, Theme.L.grey6) {
        SHARE_ALBUM_BUTTON_BASE_MODIFIER
            .border(BUTTON_BORDER_WIDTH, Theme.L.grey3, SHARE_ALBUM_BUTTON_SHAPE)
            .background(Theme.L.grey6)
    }
    val boxModifier = if (modifier == Modifier) styledBase else modifier.then(styledBase)

    Box(
        modifier = boxModifier
            .clickable(onClick = onClick),
        contentAlignment = BOX_ALIGNMENT_CENTER
    ) {
        Text(
            text = TEXT_SHARE_ALBUM,
            color = COLOR_WHITE,
            style = buttonTextStyle
        )
    }
}

@Preview
@Composable
fun AlbumInfoButtonShareAlbumPreview() {
    AlbumInfoButtonShareAlbum(onClick = {})
}
