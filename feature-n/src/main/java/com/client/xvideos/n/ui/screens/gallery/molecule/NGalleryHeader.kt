package com.client.xvideos.n.ui.screens.gallery.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.AlbumInfoButtonServerFavorite
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
    BoxWithConstraints(modifier.fillMaxWidth().padding(16.dp)) {
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

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gutter)) {
                NGalleryCover(gallery = gallery, modifier = Modifier.width(coverWidth))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    NGalleryTitles(gallery)
                    if (wide) details()
                }
            }
            if (!wide) details()
        }
    }
}

@Composable
private fun NGalleryCover(gallery: NGallery, modifier: Modifier) {
    val cover = gallery.cover
    val ratio = if (cover.width > 0 && cover.height > 0) {
        (cover.width.toFloat() / cover.height).coerceIn(0.55f, 2f)
    } else {
        137f / 200f
    }
    val shape = RoundedCornerShape(8.dp)
    UrlImage(
        url = gallery.coverUrl,
        modifier = modifier
            .aspectRatio(ratio)
            .clip(shape)
            .background(Theme.tabLevel1)
            .border(1.dp, Theme.Surface.border, shape),
        contentScale = ContentScale.Fit
    )
}

private val titleNotes = Regex("""\[[^\[\]]*]""")

@Composable
private fun NGalleryTitles(gallery: NGallery) {
    val title = gallery.englishTitle.ifBlank { gallery.title }.ifBlank { gallery.japaneseTitle }
    val displayTitle = remember(title, gallery.title) {
        buildAnnotatedString {
            append(title)
            if (title != gallery.title) {
                titleNotes.findAll(title).forEach { note ->
                    addStyle(SpanStyle(color = Theme.Text.secondary), note.range.first, note.range.last + 1)
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title.isNotBlank()) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Theme.Text.primary,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (gallery.japaneseTitle.isNotBlank() && gallery.japaneseTitle != title) {
            Text(
                text = gallery.japaneseTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Theme.Text.secondary
            )
        }
        Text(
            text = "#${gallery.id}",
            style = MaterialTheme.typography.labelLarge,
            color = Theme.Text.secondary
        )
    }
}

@Composable
private fun NGalleryFavoriteActions(
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

@Preview(name = "Телефон", widthDp = 360)
@Preview(name = "Планшет", widthDp = 840)
@Composable
private fun NGalleryHeaderPreview() {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        Surface(modifier = Modifier.fillMaxSize(), color = Theme.background) {
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
    }
}
