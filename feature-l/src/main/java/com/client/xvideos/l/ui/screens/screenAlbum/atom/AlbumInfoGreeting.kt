package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Genre

@Composable
fun AlbumInfoGreeting(
    parsed: AlbumDetails,
    onGenreClick: (Genre) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val titleStyle = remember(Theme.L.Type.caption, Theme.L.textColor) {
        Theme.L.Type.caption.copy(color = Theme.L.textColor, fontSize = 14.sp)
    }

    FlowRow(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        parsed.genres.forEach { genre ->
            key(genre.id) {
                val handleClick = remember(genre, onGenreClick) { { onGenreClick(genre) } }

                GenreChip(
                    item = genre,
                    style = titleStyle,
                    onClick = handleClick
                )
            }
        }

        parsed.description.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = Theme.L.Type.caption.copy(
                    color = Theme.L.textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Preview
@Composable
private fun AlbumInfoGreetingPreview() {
    val parsed = AlbumDetails(
        id = "album123",
        title = "Sample Album",
        description = "This is a sample album description.",
        genres = listOf(
            Genre(id = "genre1", title = "Adventure", actsAsWarning = false, url = "url/adventure"),
            Genre(id = "genre2", title = "Sci-Fi", actsAsWarning = false, url = "url/scifi")
        ),
        number_of_pictures = 10,
        number_of_animated_pictures = 2
    )
    AlbumInfoGreeting(parsed = parsed)
}
