package com.client.xvideos.x.screens.favorites

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.x.model.ItemsX

/**
 * Диалог подтверждения удаления видео из «Избранного».
 *
 * Для ориентира показываем миниатюру превью (как в L), а не текст названия.
 */
@Composable
fun ConfirmDeleteFavoriteDialog(
    item: ItemsX,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            UrlImage(
                url = item.previewImage,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(352f / 198f)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        title = { Text("Удалить из избранного?", color = Color.White) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить", color = Color(0xFFFF6B6B))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFFCCCCCC))
            }
        },
        containerColor = Color(0xFF2E2E2E),
    )
}
