package com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.sectionRowFillers
import com.client.xvideos.l.model.AlbumListTopHits
import com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.atom.ButtonSeeAll
import com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.atom.TopHitsAlbumItem

@Composable
fun TopHitsSectionItem(
    item: AlbumListTopHits,
    itemWidth: Dp,
    itemsPerRow: Int,
    onAlbumClick: (Long) -> Unit,
    onSeeAllClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            item.title,
            color = Theme.L.textColor,
            fontSize = 24.sp,
            fontFamily = Theme.L.fontFamilyKarla,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp)
        )

        FlowRow(
            maxItemsInEachRow = itemsPerRow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val albums = remember(item.items, itemsPerRow) { item.items.take(itemsPerRow * 3) }
            albums.forEach { album ->
                key(album.id) {
                    TopHitsAlbumItem(
                        album = album,
                        itemWidth = itemWidth,
                        onAlbumClick = onAlbumClick
                    )
                }
            }
            // Пустые места неполной последней строки: иначе SpaceBetween разносит её к краям.
            repeat(sectionRowFillers(albums.size, itemsPerRow)) { Spacer(Modifier.width(itemWidth)) }
        }

        val onSeeAll = remember(item.url, item.title, onSeeAllClick) {
            { onSeeAllClick(item.url, item.title) }
        }
        ButtonSeeAll(onClick = onSeeAll)
    }
}

@Preview
@Composable
private fun TopHitsSectionItemPreview() {
    TopHitsSectionItem(
        item = AlbumListTopHits(
            title = "Trending Manga",
            url = "/albums/list/?album_type=manga",
            items = emptyList()
        ),
        itemWidth = 120.dp,
        itemsPerRow = 3,
        onAlbumClick = {},
        onSeeAllClick = { _, _ -> }
    )
}
