package com.client.xvideos.l.ui.screens.screenAlbum.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val ALBUM_DIALOG_COVER_SHAPE = RoundedCornerShape(8.dp)
private val ALBUM_COVER_SIZE = 96.dp
private const val DIALOG_TITLE = "Удалить Альбом?"
private const val CONFIRM_TEXT = "Удалить"

@Composable
fun AlbumDialogDeleteAlbum(pending: AlbumDetails, onDismiss: () -> Unit, onClick: () -> Unit) {
    val coverUrl = pending.cover?.url.orEmpty()
    val dialogBody = remember(pending.title) {
        buildAnnotatedString {
            append("Удалить «")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending.title) }
            append("» из сохранённых?")
        }
    }
    val iconContent: @Composable () -> Unit = remember(coverUrl) {
        {
            UrlImage(
                url = coverUrl,
                modifier = Modifier
                    .size(ALBUM_COVER_SIZE)
                    .clip(ALBUM_DIALOG_COVER_SHAPE)
            )
        }
    }

    LavenderDialog(
        title = DIALOG_TITLE,
        onDismiss = onDismiss,
        icon = iconContent,
        body = dialogBody,
        confirmText = CONFIRM_TEXT,
        onConfirm = onClick,
        destructive = true,
    )
}

@Preview
@Composable
fun AlbumDialogDeleteAlbumPreview() {
    val sampleAlbumDetails = AlbumDetails(
        created = 1678886400.0,
        modified = 1678886400.0,
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
