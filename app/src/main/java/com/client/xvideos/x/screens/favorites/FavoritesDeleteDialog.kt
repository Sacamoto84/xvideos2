package com.client.xvideos.x.screens.favorites

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.client.xvideos.x.model.ItemsX

/**
 * Диалог подтверждения удаления видео из «Избранного».
 *
 * Тёмный стиль под фон раздела (как у L, 0xFF262626).
 */
@Composable
fun ConfirmDeleteFavoriteDialog(
    item: ItemsX,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить из избранного?", color = Color.White) },
        text = {
            Text(
                "Удалить «${item.title}» из избранного?",
                color = Color(0xFFCCCCCC),
            )
        },
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
