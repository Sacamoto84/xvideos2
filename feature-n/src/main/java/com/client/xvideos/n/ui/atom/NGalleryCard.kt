package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.common.expandmenu.LazyExpandMenuAnchor
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.model.NGalleryPreview

/**
 * Карточка манги / галереи для сеток и списков.
 */
@Composable
fun NGalleryCard(
    item: NGalleryPreview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuActions: (@Composable (dismiss: () -> Unit) -> Unit)? = null
) {
    val cardShape = RoundedCornerShape(8.dp)
    val cleanTitle = remember(item.title) { item.title.trim() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Theme.tabLevel3, cardShape)
            .clip(cardShape)
            .background(Theme.tabLevel1)
            .clickable(onClick = onClick)
    ) {
        UrlImage(
            item.thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(137f / 200f),
            contentScale = ContentScale.Crop
        )

        // Нижняя плашка с заголовком и метаданными
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC1A1A1A))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = cleanTitle,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.numPages > 0 || item.language.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.numPages > 0) {
                        Text(
                            text = "${item.numPages} стр.",
                            color = Color(0xFFB0B0B0),
                            fontSize = 10.sp
                        )
                    }
                    if (item.language.isNotEmpty()) {
                        Text(
                            text = " • ${item.language}",
                            color = Color(0xFFED2553),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (menuActions != null) {
            LazyExpandMenuAnchor(modifier = Modifier.align(Alignment.TopEnd)) { dismiss ->
                ExpandMenuActionItem(Icons.AutoMirrored.Outlined.OpenInNew, "Открыть") {
                    dismiss()
                    onClick()
                }
                menuActions(dismiss)
            }
        }
    }
}

@Preview
@Composable
private fun NGalleryCardPreview() {
    NGalleryCard(
        item = NGalleryPreview(
            id = 123456L,
            mediaId = 987654L,
            title = "Preview Manga Title",
            thumbnailUrl = "",
            numPages = 24,
            language = "english"
        ),
        onClick = {}
    )
}
