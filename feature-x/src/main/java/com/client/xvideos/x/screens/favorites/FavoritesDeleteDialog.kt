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
    val posterShape = remember { RoundedCornerShape(8.dp) }
    LavenderDialog(
        title = "Удалить из избранного?",
        onDismiss = onDismiss,
        icon = {
            UrlImage(
                url = posterUrl,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(352f / 198f)
                    .clip(posterShape)
            )
        },
        confirmText = "Удалить",
        onConfirm = onConfirm,
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
