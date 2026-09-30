package com.client.xvideos.x.screens.saved.molecule

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

/** Универсальный диалог подтверждения удаления (тёмный стиль под фон L). */
@Composable
fun ConfirmDeleteVideoDialog(
    title: String,
    imageUrl: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LavenderDialog(
        title = title,
        onDismiss = onDismiss,
        icon = {
            UrlImage(
                url = imageUrl,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(352f / 198f)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun ConfirmDeleteVideoDialogPreview() {
    ConfirmDeleteVideoDialog(
        title = "Удалить из сохранённого?",
        imageUrl = "",
        onConfirm = {},
        onDismiss = {},
    )
}
