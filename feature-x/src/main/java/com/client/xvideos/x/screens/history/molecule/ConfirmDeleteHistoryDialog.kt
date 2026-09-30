package com.client.xvideos.x.screens.history.molecule

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.x.model.ItemsX

private const val CARD_ASPECT_RATIO = 352f / 198f

/**
 * Диалог подтверждения удаления одного ролика из истории просмотров.
 */
@Composable
fun ConfirmDeleteHistoryDialog(
    item: ItemsX,
    posterUrl: String = item.previewImage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LavenderDialog(
        title = "Удалить из истории?",
        onDismiss = onDismiss,
        icon = {
            UrlImage(
                url = posterUrl,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(CARD_ASPECT_RATIO)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Preview
@Composable
private fun ConfirmDeleteHistoryDialogPreview() {
    ConfirmDeleteHistoryDialog(
        item = ItemsX(id = 1L, title = "Preview Item"),
        onConfirm = {},
        onDismiss = {}
    )
}
