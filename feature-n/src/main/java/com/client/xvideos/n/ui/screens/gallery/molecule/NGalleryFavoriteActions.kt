package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.AlbumInfoButtonServerFavorite
import com.client.xvideos.n.ui.screens.gallery.NServerFavoriteState

@Composable
internal fun NGalleryFavoriteActions(
    isFavorite: Boolean,
    canToggleFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    serverFavorite: NServerFavoriteState,
    onServerFavoriteClick: () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    val accent = Color(0xFFED2553)
    Row(
        modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(if (isFavorite) accent.copy(alpha = 0.16f) else Theme.tabLevel2)
                .border(1.dp, if (isFavorite) accent else Theme.Surface.border, shape)
                .clickable(enabled = canToggleFavorite, role = Role.Button, onClick = onFavoriteClick),
            contentAlignment = Alignment.Center
        ) {
            if (canToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isFavorite) "Удалить из закладок" else "В закладки",
                    tint = if (isFavorite) accent else Theme.Text.primary
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
            }
        }
        AlbumInfoButtonServerFavorite(
            isFavorite = serverFavorite.saved == true,
            isLoading = serverFavorite.loading,
            onClick = onServerFavoriteClick,
            modifier = Modifier.weight(1f),
            label = if (serverFavorite.readError != null) "Проверить избранное" else null
        )
    }
}

@Preview
@Composable
private fun NGalleryFavoriteActionsPreview() {
    NGalleryFavoriteActions(
        isFavorite = true,
        canToggleFavorite = true,
        onFavoriteClick = {},
        serverFavorite = NServerFavoriteState(),
        onServerFavoriteClick = {},
    )
}
