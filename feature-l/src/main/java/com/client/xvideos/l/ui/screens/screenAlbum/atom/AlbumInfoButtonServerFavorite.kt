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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

/**
 * Кнопка «добавить/удалить альбом из избранного на сервере Luscious» в шапке ScreenLAlbum.
 */
@Composable
fun AlbumInfoButtonServerFavorite(
    isFavorite: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(top = 2.dp, bottom = 4.dp)
            .height(46.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Theme.L.grey3, RoundedCornerShape(4.dp))
            .background(if (isFavorite) Theme.L.grey6 else Theme.L.red)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Theme.L.red else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isFavorite) "Удалить альбом с сервера" else "Добавить альбом на сервер",
                    color = Color.White,
                    style = Theme.L.Type.button.copy(color = Color.White)
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
