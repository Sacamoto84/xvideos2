package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.theme.Theme

private val N_ACCENT = Color(0xFFED2553)

/**
 * Диалог действий с коллекцией манги (переименовать, удалить).
 */
@Composable
fun NCollectionActionDialog(
    pending: String,
    coverUrl: String?,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val iconShape = remember { RoundedCornerShape(8.dp) }
    LavenderDialog(
        title = "Действие с коллекцией",
        onDismiss = onDismiss,
        icon = {
            val iconSize = Theme.DialogLavande.iconSize
            if (!coverUrl.isNullOrEmpty()) {
                UrlImage(
                    url = coverUrl,
                    modifier = Modifier
                        .clip(iconShape)
                        .size(iconSize)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(iconShape)
                        .size(iconSize)
                        .background(Color.Gray)
                )
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pending,
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            DropdownMenuItem(
                text = { Text("Переименовать", color = Color.White) },
                onClick = onRename,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = N_ACCENT
                    )
                }
            )

            DropdownMenuItem(
                text = { Text("Удалить коллекцию", color = Color.White) },
                onClick = onDelete,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = N_ACCENT
                    )
                }
            )
        }
    )
}

@Preview
@Composable
private fun NCollectionActionDialogPreview() {
    NCollectionActionDialog(
        pending = "Избранные страницы",
        coverUrl = null,
        onDismiss = {},
        onRename = {},
        onDelete = {}
    )
}
