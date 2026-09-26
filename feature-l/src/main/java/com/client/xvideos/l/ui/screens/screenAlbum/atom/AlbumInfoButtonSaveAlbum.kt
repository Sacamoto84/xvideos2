package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
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

private val BUTTON_SHAPE = RoundedCornerShape(4.dp)

@Composable
fun AlbumInfoButtonSaveAlbum(
    saved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonText = if (!saved) "Сохранить альбом" else "Удалить из сохранённых"
    val iconVector = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
    val iconTint = if (saved) Theme.L.red else Color.White
    val backgroundColor = if (!saved) Theme.L.red else Theme.L.grey6
    val buttonTextStyle = remember(Theme.L.Type.button) {
        Theme.L.Type.button.copy(color = Color.White)
    }
    val styledBase = remember(Theme.L.grey3, backgroundColor) {
        Modifier
            .padding(top = 2.dp, bottom = 4.dp)
            .height(46.dp)
            .fillMaxWidth()
            .clip(BUTTON_SHAPE)
            .border(1.dp, Theme.L.grey3, BUTTON_SHAPE)
            .background(backgroundColor)
    }
    val baseModifier = if (modifier == Modifier) styledBase else modifier.then(styledBase)

    Box(
        modifier = baseModifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = buttonText,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = buttonText,
                color = Color.White,
                style = buttonTextStyle
            )
        }
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
