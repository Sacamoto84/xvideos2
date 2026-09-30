package com.client.xvideos.x.screens.subscriptions.molecule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.core.R
import com.client.xvideos.x.model.XSubscriptionItem

/**
 * Вертикальный элемент списка подписки (стиль L: картинка слева, имя и счётчики справа, кнопка удаления).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubscriptionListItem(
    item: XSubscriptionItem,
    isModel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (isModel) Color(0xFFDE2600) else Color(0xFF1E88E5)
    val imageUrl = remember(item.avatarUrl, item.bannerUrl) {
        item.avatarUrl.ifBlank { item.bannerUrl }
    }

    val subtitle = remember(item.subscribers, item.videoCount, item.totalViews) {
        val parts = mutableListOf<String>()
        if (item.videoCount > 0) {
            parts.add("${item.videoCount} видео")
        }
        if (item.subscribers.isNotBlank()) {
            parts.add("${item.subscribers} подписчиков")
        }
        if (item.totalViews.isNotBlank()) {
            parts.add("${item.totalViews} просмотров")
        }
        if (parts.isEmpty()) {
            if (isModel) "Актриса" else "Канал"
        } else {
            parts.joinToString(" • ")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF16161C))
            .border(1.dp, Color(0xFF282830), RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Превью / аватар автора
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF222228)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotBlank()) {
                    UrlImage(
                        url = imageUrl,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = if (isModel) "\uE9B8" else "\uE956",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 28.sp,
                            fontFamily = FontFamily(Font(R.font.iconfont))
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Текстовая колонка: название и метаданные
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.displayName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFA0A0A8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Кнопка удаления (отписки)
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Отписаться",
                    tint = Color(0xFF888890),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF16161C)
@Composable
private fun SubscriptionListItemPreview() {
    SubscriptionListItem(
        item = XSubscriptionItem(
            slug = "test-channel",
            name = "Test Channel",
            isModel = false,
            videoCount = 120,
            subscribers = "10K",
            totalViews = "500K"
        ),
        isModel = false,
        onClick = {},
        onLongClick = {},
        onDeleteClick = {}
    )
}
