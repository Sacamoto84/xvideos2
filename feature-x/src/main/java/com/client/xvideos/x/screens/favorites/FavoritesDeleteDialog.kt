package com.client.xvideos.x.screens.favorites

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.ui.theme.XvideosTheme

private val FAVORITE_POSTER_CORNER = 8.dp
private val FAVORITE_POSTER_SHAPE = RoundedCornerShape(FAVORITE_POSTER_CORNER)
private const val POSTER_ASPECT_RATIO = 352f / 198f
private val POSTER_WIDTH = 160.dp
private const val DIALOG_TITLE = "Удалить из избранного?"
private const val CONFIRM_TEXT = "Удалить"

/**
 * Диалог подтверждения удаления видео из «Избранного».
 *
 * Для ориентира показываем миниатюру превью (как в L), а не текст названия.
 */
@Composable
fun ConfirmDeleteFavoriteDialog(
    item: ItemsX,
    posterUrl: String = item.previewImage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val handleConfirm = remember(onConfirm) { { onConfirm() } }
    val handleDismiss = remember(onDismiss) { { onDismiss() } }

    val iconContent: @Composable () -> Unit = remember(posterUrl) {
        {
            UrlImage(
                url = posterUrl,
                modifier = Modifier
                    .width(POSTER_WIDTH)
                    .aspectRatio(POSTER_ASPECT_RATIO)
                    .clip(FAVORITE_POSTER_SHAPE)
            )
        }
    }

    LavenderDialog(
        title = DIALOG_TITLE,
        onDismiss = handleDismiss,
        icon = iconContent,
        confirmText = CONFIRM_TEXT,
        onConfirm = handleConfirm,
        destructive = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun ConfirmDeleteFavoriteDialogPreview() {
    XvideosTheme(darkTheme = true) {
        ConfirmDeleteFavoriteDialog(
            item = ItemsX(
                id = 1L,
                title = "Sample favorite video",
                duration = "12:34",
                views = "1.2M",
                channel = "Preview Channel",
                previewImage = "",
                href = "/video/1",
                nameProfile = "Preview Channel",
                linkProfile = "/preview-channel",
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
