package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NTag
import com.client.xvideos.n.ui.screens.gallery.NServerFavoriteState

@Composable
fun NGalleryHeader(
    gallery: NGallery,
    onTagClick: (NTag) -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    serverFavorite: NServerFavoriteState = NServerFavoriteState(),
    onServerFavoriteClick: () -> Unit = {},
    canToggleFavorite: Boolean = true
) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(4.dp)) {
        val wide = maxWidth >= 600.dp
        val gutter = if (wide) 32.dp else 16.dp
        val coverWidth = if (wide) minOf(maxWidth * 0.33f, 360.dp) else minOf(maxWidth * 0.34f, 144.dp)
        val details: @Composable () -> Unit = {
            NGalleryMetadata(gallery = gallery, onTagClick = onTagClick)
            NGalleryFavoriteActions(
                isFavorite = isFavorite,
                canToggleFavorite = canToggleFavorite,
                onFavoriteClick = onFavoriteClick,
                serverFavorite = serverFavorite,
                onServerFavoriteClick = onServerFavoriteClick
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gutter)) {
                NGalleryCover(gallery = gallery, modifier = Modifier.width(coverWidth))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NGalleryTitles(gallery)
                    if (wide) details()
                }
            }
            if (!wide) details()
        }
    }
}

@Preview(name = "Телефон", widthDp = 360)
@Preview(name = "Планшет", widthDp = 840)
@Composable
private fun NGalleryHeaderPreview() {
    NGalleryHeader(
        gallery = NGallery(
            id = 123456L,
            mediaId = 987654L,
            title = "The Midnight Library",
            englishTitle = "[Studio Paper] The Midnight Library [Digital Edition]",
            japaneseTitle = "真夜中の図書館",
            uploadDate = 1_579_132_800L,
            numPages = 32,
            tags = listOf(
                NTag(id = 1, type = "artist", name = "Aki Sato", count = 120),
                NTag(id = 2, type = "tag", name = "full color", count = 5300),
                NTag(id = 3, type = "tag", name = "illustrations", count = 15650),
                NTag(id = 4, type = "tag", name = "short stories", count = 73900),
                NTag(id = 5, type = "language", name = "japanese", count = 98000),
                NTag(id = 6, type = "category", name = "manga", count = 521100)
            )
        ),
        onTagClick = {},
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    )
}
