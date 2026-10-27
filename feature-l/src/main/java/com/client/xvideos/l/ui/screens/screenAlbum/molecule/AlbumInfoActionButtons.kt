package com.client.xvideos.l.ui.screens.screenAlbum.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.WidthClass
import com.client.xvideos.common.ui.adaptive.currentWidthClass
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonSaveAlbum
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonServerFavorite
import com.client.xvideos.l.ui.screens.screenAlbum.atom.AlbumInfoButtonShareAlbum

/**
 * Кнопки шапки альбома: сохранить, избранное на сервере, поделиться.
 *
 * На широком окне (раскрытый складной) — три в один ряд поровну; уже — друг
 * под другом на всю ширину, как на телефоне. На Medium в трети ряда подпись
 * «Добавить альбом на сервер» уже не помещается.
 */
@Composable
fun AlbumInfoActionButtons(
    saved: Boolean,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onSaveClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentWidthClass() == WidthClass.Expanded) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlbumInfoButtonSaveAlbum(saved = saved, onClick = onSaveClick, modifier = Modifier.weight(1f))
            AlbumInfoButtonServerFavorite(
                isFavorite = isFavorite,
                isLoading = isFavoriteLoading,
                onClick = onFavoriteClick,
                modifier = Modifier.weight(1f)
            )
            AlbumInfoButtonShareAlbum(onClick = onShareClick, modifier = Modifier.weight(1f))
        }
    } else {
        Column(modifier = modifier) {
            AlbumInfoButtonSaveAlbum(saved = saved, onClick = onSaveClick)
            AlbumInfoButtonServerFavorite(isFavorite = isFavorite, isLoading = isFavoriteLoading, onClick = onFavoriteClick)
            AlbumInfoButtonShareAlbum(onClick = onShareClick)
        }
    }
}

@Preview
@Composable
private fun AlbumInfoActionButtonsPreview() {
    AlbumInfoActionButtons(
        saved = false,
        isFavorite = false,
        isFavoriteLoading = false,
        onSaveClick = {},
        onFavoriteClick = {},
        onShareClick = {}
    )
}
