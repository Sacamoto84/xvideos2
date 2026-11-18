package com.client.xvideos.n.ui.screens.explorer.tab.saved.serverFavorites.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.n.model.NGalleryPreview

/**
 * Диалог действий над мангой из серверного избранного по долгому тапу.
 */
@Composable
fun NServerFavoriteActionDialog(
    item: NGalleryPreview,
    isLocallySaved: Boolean,
    onDismiss: () -> Unit,
    onOpen: (Long) -> Unit,
    onImport: (NGalleryPreview) -> Unit,
    onRemoveFromServer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = modifier
                .clip(shape)
                .border(1.dp, Color(0xFF3E3E3E), shape)
                .background(Color(0xFF1E1E1E))
                .padding(20.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))

            ActionRow(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                title = "Открыть",
                tint = Color.White,
                onClick = {
                    onDismiss()
                    onOpen(item.id)
                }
            )

            if (!isLocallySaved) {
                ActionRow(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = "Сохранить локально",
                    tint = Color.White,
                    onClick = {
                        onDismiss()
                        onImport(item)
                    }
                )
            } else {
                ActionRow(
                    icon = Icons.Filled.Bookmark,
                    title = "Уже сохранено локально",
                    tint = Color(0xFF888888),
                    onClick = null
                )
            }

            ActionRow(
                icon = Icons.Outlined.Delete,
                title = "Удалить с сервера",
                tint = Color(0xFFED2553),
                onClick = {
                    onDismiss()
                    onRemoveFromServer(item.id)
                }
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: (() -> Unit)?
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(clickableModifier)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        Text(
            text = title,
            color = tint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun NServerFavoriteActionDialogPreview() {
    NServerFavoriteActionDialog(
        item = NGalleryPreview(id = 123L, mediaId = 123L, title = "Server Favorite Preview Manga"),
        isLocallySaved = false,
        onDismiss = {},
        onOpen = {},
        onImport = {},
        onRemoveFromServer = {}
    )
}
