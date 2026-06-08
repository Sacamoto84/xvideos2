package com.client.xvideos.l.ui.screens.screenAlbum.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Cover
import com.client.xvideos.l.model.Genre

@Composable
fun AlbumDialogDeleteAlbum(pending: AlbumDetails, onDismiss: () -> Unit, onClick: () -> Unit) {

    AlertDialog(
        icon = { UrlImage(pending.cover.url, modifier = Modifier.size(96.dp)) },
        onDismissRequest = { onDismiss() },
        title = {
            com.composeunstyled.Text(
                "Удалить Альбом?",
                style = Theme.L.Type.dialogTitle.copy(color = Color.Black, fontWeight = FontWeight.Bold)
            )
        },
        text = {
            com.composeunstyled.Text(buildAnnotatedString {
                append("Удалить «")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.title) }
                append("» из сохранённых?")
            }, style = Theme.L.Type.dialogBody.copy(color = Color(0xFF474747)))
        },

        confirmButton = {
            TextButton( onClick = { onClick() } ) {
                com.composeunstyled.Text(
                    "Удалить",
                    color = Color(0xFF6552A5),
                    style = Theme.L.Type.button.copy(color = Color(0xFF6552A5))
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                com.composeunstyled.Text(
                    "Отмена",
                    color = Color(0xFF6552A5),
                    style = Theme.L.Type.button.copy(color = Color(0xFF6552A5))
                )
            }
        },

        /* Доп. стили при желании */
        containerColor = Color(0xFFEBE6EE)
    )

}

@Preview
@Composable
fun AlbumDialogDeleteAlbumPreview() {
    val sampleAlbumDetails = AlbumDetails(
        created = 1678886400.0, // Example timestamp
        modified = 1678886400.0, // Example timestamp
        id = "album123",
        title = "Summer Vacation",
        tags = emptyList(),
        is_manga = false,
        content = com.client.xvideos.l.model.Content(id = "content1", title = "Photo Album", url = "http://example.com/content"),
        genres = listOf(Genre(id = "genre1", title = "Nature", actsAsWarning = false, url = "http://example.com/genre/nature")),
        cover = Cover(width = 100, height = 100, size = "100x100", url = "https://via.placeholder.com/150"),
        description = "A collection of photos from summer vacation.",
        audiences = emptyList(),
        number_of_pictures = 50,
        number_of_animated_pictures = 5,
        url = "http://example.com/album/summer_vacation",
        download_url = "http://example.com/download/summer_vacation"
    )
    AlbumDialogDeleteAlbum(
        pending = sampleAlbumDetails,
        onDismiss = {},
        onClick = {}
    )
}

