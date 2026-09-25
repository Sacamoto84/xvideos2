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

private val SAVE_ALBUM_BUTTON_CORNER = 4.dp
private val SAVE_ALBUM_BUTTON_SHAPE = RoundedCornerShape(SAVE_ALBUM_BUTTON_CORNER)
private val BUTTON_HEIGHT = 46.dp
private val BUTTON_TOP_PADDING = 2.dp
private val BUTTON_BOTTOM_PADDING = 4.dp
private val BUTTON_BORDER_WIDTH = 1.dp
private val CONTENT_SPACING = 8.dp
private val ICON_SIZE = 20.dp
private const val TEXT_SAVE_ALBUM = "Сохранить альбом"
private const val TEXT_REMOVE_ALBUM = "Удалить из сохранённых"
private val ICON_BOOKMARK_FILLED = Icons.Filled.Bookmark
private val ICON_BOOKMARK_BORDER = Icons.Outlined.BookmarkBorder
private val CONTENT_ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val BOX_ALIGNMENT_CENTER = Alignment.Center
private val COLOR_WHITE = Color.White

private val SAVE_ALBUM_BUTTON_BASE_MODIFIER = Modifier
    .padding(top = BUTTON_TOP_PADDING, bottom = BUTTON_BOTTOM_PADDING)
    .height(BUTTON_HEIGHT)
    .fillMaxWidth()
    .clip(SAVE_ALBUM_BUTTON_SHAPE)
private val CONTENT_ROW_HORIZONTAL_ARRANGEMENT = Arrangement.spacedBy(CONTENT_SPACING)
private val ICON_MODIFIER = Modifier.size(ICON_SIZE)

@Composable
fun AlbumInfoButtonSaveAlbum(
    saved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonText = remember(saved) {
        if (!saved) TEXT_SAVE_ALBUM else TEXT_REMOVE_ALBUM
    }
    val iconVector = remember(saved) {
        if (saved) ICON_BOOKMARK_FILLED else ICON_BOOKMARK_BORDER
    }
    val iconTint = remember(saved, Theme.L.red) {
        if (saved) Theme.L.red else COLOR_WHITE
    }
    val backgroundColor = remember(saved, Theme.L.red, Theme.L.grey6) {
        if (!saved) Theme.L.red else Theme.L.grey6
    }
    val buttonTextStyle = remember(Theme.L.Type.button) {
        Theme.L.Type.button.copy(color = COLOR_WHITE)
    }

    Box(
        modifier = modifier
            .then(SAVE_ALBUM_BUTTON_BASE_MODIFIER)
            .border(BUTTON_BORDER_WIDTH, Theme.L.grey3, SAVE_ALBUM_BUTTON_SHAPE)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = BOX_ALIGNMENT_CENTER
    ) {
        Row(
            horizontalArrangement = CONTENT_ROW_HORIZONTAL_ARRANGEMENT,
            verticalAlignment = CONTENT_ROW_VERTICAL_ALIGNMENT
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = buttonText,
                tint = iconTint,
                modifier = ICON_MODIFIER
            )
            Text(
                text = buttonText,
                color = COLOR_WHITE,
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
