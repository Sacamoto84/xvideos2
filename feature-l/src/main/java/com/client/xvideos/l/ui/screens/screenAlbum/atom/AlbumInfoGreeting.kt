package com.client.xvideos.l.ui.screens.screenAlbum.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Audience
import com.client.xvideos.l.model.Content
import com.client.xvideos.l.model.Cover
import com.client.xvideos.l.model.Genre
import com.client.xvideos.l.model.Tag

private val GENRE_CHIP_CORNER = 4.dp
private val GENRE_CHIP_SHAPE = RoundedCornerShape(GENRE_CHIP_CORNER)
private val CHIP_BORDER_WIDTH = 1.dp
private val CHIP_HORIZONTAL_PADDING = 2.dp
private val CHIP_VERTICAL_PADDING = 2.dp
private val CHIP_CONTENT_PADDING = 4.dp
private val HEADER_VERTICAL_PADDING = 4.dp
private val HEADER_FONT_SIZE = 16.sp
private val GENRE_FONT_SIZE = 14.sp
private const val LABEL_GENRES = "Genres: "

@Composable
fun AlbumInfoGreeting(
    parsed: AlbumDetails,
    onGenreClick: (Genre) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val headerStyle = remember(Theme.L.Type.rowTitle) {
        Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = HEADER_FONT_SIZE)
    }
    val genreTextStyle = remember(Theme.L.Type.rowValue, Theme.L.primaryColor) {
        Theme.L.Type.rowValue.copy(color = Theme.L.primaryColor, fontSize = GENRE_FONT_SIZE)
    }

    FlowRow(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = LABEL_GENRES,
            color = Theme.L.textColor,
            style = headerStyle,
            modifier = Modifier.padding(vertical = HEADER_VERTICAL_PADDING)
        )

        parsed.genres.forEach { item ->
            key(item.id) {
                val handleClick = remember(item, onGenreClick) { { onGenreClick(item) } }
                Text(
                    text = item.title,
                    modifier = Modifier
                        .padding(horizontal = CHIP_HORIZONTAL_PADDING, vertical = CHIP_VERTICAL_PADDING)
                        .border(CHIP_BORDER_WIDTH, Theme.L.secondaryColor, GENRE_CHIP_SHAPE)
                        .clip(GENRE_CHIP_SHAPE)
                        .clickable(onClick = handleClick)
                        .padding(CHIP_CONTENT_PADDING),
                    color = Theme.L.primaryColor,
                    style = genreTextStyle
                )
            }
        }
    }
}

@Preview
@Composable
fun AlbumInfoGreetingPreview() {
    val parsed = AlbumDetails(
        created = 1678886400.0,
        modified = 1678886400.0,
        id = "album123",
        title = "Sample Album",
        tags = listOf(
            Tag(id = "tag1", category = "Nature", text = "Mountains", url = "url/mountains", count = 100),
            Tag(id = "tag2", category = "Nature", text = "Rivers", url = "url/rivers", count = 50)
        ),
        is_manga = false,
        content = Content(id = "content1", title = "Album Content", url = "url/content"),
        genres = listOf(
            Genre(id = "genre1", title = "Adventure", actsAsWarning = false, url = "url/adventure"),
            Genre(id = "genre2", title = "Sci-Fi", actsAsWarning = false, url = "url/scifi")
        ),
        cover = Cover(width = 800, height = 600, size = "large", url = "url/cover.jpg"),
        description = "This is a sample album description.",
        audiences = listOf(
            Audience(id = "audience1", title = "General", url = "url/general"),
            Audience(id = "audience2", title = "Teens", url = "url/teens")
        ),
        number_of_pictures = 10,
        number_of_animated_pictures = 2,
        url = "url/album123",
        download_url = "url/download/album123"
    )
    AlbumInfoGreeting(parsed = parsed)
}
