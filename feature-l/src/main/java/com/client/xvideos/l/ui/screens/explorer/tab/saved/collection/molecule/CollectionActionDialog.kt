package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
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

@Composable
fun CollectionActionDialog(
    pending: String,
    coverUrl: String?,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val iconShape = remember { RoundedCornerShape(8.dp) }
    LavenderDialog(
        title = "Действие с коллекцией",
        onDismiss = onDismiss,
        icon = {
            val iconSize = Theme.DialogLavande.iconSize
            if (coverUrl != null) {
                UrlImage(url = coverUrl, modifier = Modifier.clip(iconShape).size(iconSize))
            } else {
                Box(Modifier.clip(iconShape).size(iconSize).background(Color.Gray))
            }
        },
        content = {
            Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    pending,
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            DropdownMenuItem(
                text = { Text("Переименовать", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                onClick = onRename,
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
            )

            DropdownMenuItem(
                text = { Text("Поделиться (P2P)", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                onClick = onShare,
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
            )

            DropdownMenuItem(
                text = { Text("Удалить коллекцию", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                onClick = onDelete,
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
            )
        }
    )
}

@Preview
@Composable
private fun CollectionActionDialogPreview() {
    CollectionActionDialog(
        pending = "Favorites",
        coverUrl = null,
        onDismiss = {},
        onRename = {},
        onShare = {},
        onDelete = {}
    )
}
