package com.client.xvideos.l.ui.element

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage

private val ALBUM_CARD_CORNER = 8.dp
private val ALBUM_CARD_SHAPE = RoundedCornerShape(ALBUM_CARD_CORNER)
private val ALBUM_BORDER_WIDTH = 1.dp
private const val ALBUM_CARD_ASPECT_RATIO = 137f / 200f
private val BOTTOM_OVERLAY_BG = Color(0x80000000)
private val TITLE_PADDING_HORIZONTAL = 4.dp
private val SUBTITLE_PADDING_START = 4.dp
private const val SUFFIX_GIFS = " gifs"
private const val SEPARATOR_SLASH = " / "
private const val SUFFIX_PICTURES = " pictures"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumListItem(
    title: String,
    coverUrl: String,
    numberOfAnimatedPictures: Int,
    numberOfPictures: Int,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }

    val titleStyle = remember(Theme.L.Type.rowTitle) {
        Theme.L.Type.rowTitle.copy(color = Color.White)
    }

    val countSubtitle = remember(numberOfAnimatedPictures, numberOfPictures) {
        buildString {
            if (numberOfAnimatedPictures > 0) {
                append(numberOfAnimatedPictures)
                append(SUFFIX_GIFS)
                if (numberOfPictures > 0) append(SEPARATOR_SLASH)
            }
            if (numberOfPictures > 0) {
                append(numberOfPictures)
                if (numberOfAnimatedPictures == 0) append(SUFFIX_PICTURES)
            }
        }
    }

    val cleanTitle = remember(title) { title.removePrefix(" ") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(ALBUM_BORDER_WIDTH, Theme.tabLevel3, ALBUM_CARD_SHAPE)
            .clip(ALBUM_CARD_SHAPE)
            .background(Theme.tabLevel1)
            .then(clickModifier)
    ) {
        UrlImage(
            coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ALBUM_CARD_ASPECT_RATIO),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(BOTTOM_OVERLAY_BG)
        ) {
            Text(
                cleanTitle,
                modifier = Modifier.padding(horizontal = TITLE_PADDING_HORIZONTAL),
                color = Color.White,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .padding(start = SUBTITLE_PADDING_START)
                    .fillMaxWidth()
            ) {
                Text(
                    countSubtitle,
                    modifier = Modifier,
                    color = Theme.L.textColor,
                    style = Theme.L.Type.rowSubtitle
                )
            }
        }
    }
}

@Preview
@Composable
fun AlbumListItemPreview() {
    AlbumListItem(
        title = "Album Title",
        coverUrl = "https://i.pinimg.com/1200x/2c/86/8d/2c868d9ab0c4d4f3a76631c1b0077058.jpg",
        numberOfAnimatedPictures = 5,
        numberOfPictures = 10,
        onClick = {}
    )
}
