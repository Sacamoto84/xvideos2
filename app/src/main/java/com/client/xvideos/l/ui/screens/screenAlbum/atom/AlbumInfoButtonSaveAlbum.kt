package com.client.xvideos.l.ui.screens.screenAlbum.atom

import com.client.xvideos.common.theme.Theme

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun AlbumInfoButtonSaveAlbum(saved: Boolean, onClick : ()->Unit) {

    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .padding(top = 2.dp, bottom = 4.dp)
            .height(46.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Theme.L.grey3, RoundedCornerShape(4.dp))
            .background(if (!saved) Theme.L.red else Theme.L.grey6)
            .clickable(onClick = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        if (!saved) Text(
            "Save Album",
            color = Color.White,
            style = Theme.L.Type.button.copy(color = Color.White)
        )
        else Text(
            "Remove Album",
            color = Color.White,
            style = Theme.L.Type.button.copy(color = Color.White)
        )
    }


}

@Preview
@Composable
fun AlbumInfoButtonSaveAlbumPreview() {
    AlbumInfoButtonSaveAlbum(saved = false, onClick = {})
}

@Preview
@Composable
fun AlbumInfoButtonSaveAlbumSavedPreview() {
    AlbumInfoButtonSaveAlbum(saved = true, onClick = {})
}
