package com.client.xvideos.n.ui.screens.explorer.tab.saved.manga.molecule

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.n.model.NGalleryPreview

/**
 * Диалог действий над сохранённой мангой по долгому тапу (открыть или удалить).
 */
@Composable
fun NSavedMangaActionDialog(
    item: NGalleryPreview,
    onDismiss: () -> Unit,
    onOpen: (Long) -> Unit,
    onRemove: (Long) -> Unit,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onDismiss()
                        onOpen(item.id)
                    }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = "Открыть",
                    color = Color.White,
                    fontSize = 15.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onDismiss()
                        onRemove(item.id)
                    }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = Color(0xFFED2553)
                )
                Text(
                    text = "Удалить из сохранённого",
                    color = Color(0xFFED2553),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview
@Composable
private fun NSavedMangaActionDialogPreview() {
    NSavedMangaActionDialog(
        item = NGalleryPreview(id = 123L, mediaId = 123L, title = "Preview Manga Title"),
        onDismiss = {},
        onOpen = {},
        onRemove = {}
    )
}
