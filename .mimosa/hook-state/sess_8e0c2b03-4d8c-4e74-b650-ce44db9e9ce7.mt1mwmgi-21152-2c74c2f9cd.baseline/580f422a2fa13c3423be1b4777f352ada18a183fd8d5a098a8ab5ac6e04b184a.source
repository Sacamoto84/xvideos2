package com.client.xvideos.l.ui.screens.screenAlbum.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.Content
import com.client.xvideos.l.model.Cover
import com.client.xvideos.l.model.Genre

@Composable
fun AlbumDialogDeleteAlbum(pending: AlbumDetails, onDismiss: () -> Unit, onClick: () -> Unit) {

    LavenderDialog(
        title = "Удалить Альбом?",
        onDismiss = onDismiss,
        icon = { UrlImage(pending.cover?.url.orEmpty(), modifier = Modifier.size(96.dp)) },
        body = buildAnnotatedString {
            append("Удалить «")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.title) }
            append("» из сохранённых?")
        },
        confirmText = "Удалить",
        onConfirm = onClick,
        destructive = true,
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
        content = Content(id = "content1", title = "Photo Album", url = "http://example.com/content"),
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

