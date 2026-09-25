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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
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

private val SERVER_FAVORITE_BUTTON_SHAPE = RoundedCornerShape(4.dp)
private val BUTTON_HEIGHT = 46.dp
private val BUTTON_TOP_PADDING = 2.dp
private val BUTTON_BOTTOM_PADDING = 4.dp
private val BUTTON_BORDER_WIDTH = 1.dp
private val CONTENT_SPACING = 8.dp
private val ICON_SIZE = 20.dp
private val PROGRESS_INDICATOR_SIZE = 20.dp
private val PROGRESS_STROKE_WIDTH = 2.dp
private const val TEXT_REMOVE_FROM_SERVER = "Удалить альбом с сервера"
private const val TEXT_ADD_TO_SERVER = "Добавить альбом на сервер"
private val ICON_FAVORITE_FILLED = Icons.Filled.Favorite
private val ICON_FAVORITE_BORDER = Icons.Outlined.FavoriteBorder
private val CONTENT_ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val BOX_ALIGNMENT_CENTER = Alignment.Center
private val COLOR_WHITE = Color.White

private val SERVER_FAVORITE_BUTTON_BASE_MODIFIER = Modifier
    .padding(top = BUTTON_TOP_PADDING, bottom = BUTTON_BOTTOM_PADDING)
    .height(BUTTON_HEIGHT)
    .fillMaxWidth()
    .clip(SERVER_FAVORITE_BUTTON_SHAPE)
private val CONTENT_ROW_HORIZONTAL_ARRANGEMENT = Arrangement.spacedBy(CONTENT_SPACING)
private val ICON_MODIFIER = Modifier.size(ICON_SIZE)
private val PROGRESS_INDICATOR_MODIFIER = Modifier.size(PROGRESS_INDICATOR_SIZE)

/**
 * Кнопка «добавить/удалить альбом из избранного на сервере Luscious» в шапке ScreenLAlbum.
 */
@Composable
fun AlbumInfoButtonServerFavorite(
    isFavorite: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonText = remember(isFavorite) {
        if (isFavorite) TEXT_REMOVE_FROM_SERVER else TEXT_ADD_TO_SERVER
    }
    val iconVector = remember(isFavorite) {
        if (isFavorite) ICON_FAVORITE_FILLED else ICON_FAVORITE_BORDER
    }
    val iconTint = remember(isFavorite, Theme.L.red) {
        if (isFavorite) Theme.L.red else COLOR_WHITE
    }
    val backgroundColor = remember(isFavorite, Theme.L.grey6, Theme.L.red) {
        if (isFavorite) Theme.L.grey6 else Theme.L.red
    }
    val buttonTextStyle = remember(Theme.L.Type.button) {
        Theme.L.Type.button.copy(color = COLOR_WHITE)
    }

    Box(
        modifier = modifier
            .then(SERVER_FAVORITE_BUTTON_BASE_MODIFIER)
            .border(BUTTON_BORDER_WIDTH, Theme.L.grey3, SERVER_FAVORITE_BUTTON_SHAPE)
            .background(backgroundColor)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = BOX_ALIGNMENT_CENTER
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = PROGRESS_INDICATOR_MODIFIER,
                color = COLOR_WHITE,
                strokeWidth = PROGRESS_STROKE_WIDTH
            )
        } else {
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
}

@Preview
@Composable
fun AlbumInfoButtonServerFavoriteNotLikedPreview() {
    AlbumInfoButtonServerFavorite(isFavorite = false, onClick = {})
}

@Preview
@Composable
fun AlbumInfoButtonServerFavoriteLikedPreview() {
    AlbumInfoButtonServerFavorite(isFavorite = true, onClick = {})
}
