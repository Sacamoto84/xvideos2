package com.client.xvideos.x.screens.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.core.R
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.XSubscriptionItem
import com.client.xvideos.x.screens.channel.ScreenX_Channel

/**
 * Контент экрана подписок раздела X (вкладки «Каналы» и «Актрисы»).
 *
 * Отображает вертикальный список подписанных авторов (подобно реализации в разделе L):
 * - Превью (аватарка/баннер) с закругленными углами.
 * - Имя автора и метаинформация (число видео, подписчики, просмотры).
 * - Нажатие на элемент открывает экран канала/актрисы ([ScreenX_Channel]).
 * - Долгое нажатие или кнопка удаления вызывают диалог подтверждения отписки ([DialogXSubscriptionDelete]).
 *
 * @param saved Фасад локальных данных X ([SavedX]).
 * @param isModel `true` для вкладки актрис/моделей, `false` для вкладки каналов.
 */
@Composable
fun X_SubscriptionsContent(
    saved: SavedX,
    isModel: Boolean,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow

    val itemsList = if (isModel) {
        saved.subscriptions.modelsList
    } else {
        saved.subscriptions.channelsList
    }

    var itemToDelete by remember { mutableStateOf<XSubscriptionItem?>(null) }
    val listState = rememberLazyListState()
    val topCutout = getTopInsetDp()

    BackHandler(enabled = itemToDelete != null) {
        itemToDelete = null
    }

    val onOpenCreator = remember(navigator, isModel) {
        { item: XSubscriptionItem ->
            navigator.push(ScreenX_Channel(slug = item.cleanSlug, isModel = item.isModel))
        }
    }

    val onConfirmDelete = remember(saved, isModel) {
        { item: XSubscriptionItem ->
            if (item.isModel) {
                saved.subscriptions.removeModel(item.cleanSlug)
            } else {
                saved.subscriptions.removeChannel(item.cleanSlug)
            }
            itemToDelete = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040404))
    ) {
        if (itemsList.isEmpty()) {
            SubscriptionsEmptyState(
                isModel = isModel,
                topCutout = topCutout,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topCutout, bottom = 24.dp)
            ) {
                item(key = "header", contentType = "header") {
                    SubscriptionsHeader(
                        title = if (isModel) "Подписки на актрис" else "Подписки на каналы",
                        count = itemsList.size,
                        isModel = isModel,
                    )
                }

                itemsIndexed(
                    items = itemsList,
                    key = { index, item -> "${item.cleanSlug}#$index" },
                    contentType = { _, _ -> "subscription_item" }
                ) { _, item ->
                    SubscriptionListItem(
                        item = item,
                        isModel = isModel,
                        onClick = { onOpenCreator(item) },
                        onLongClick = { itemToDelete = item },
                        onDeleteClick = { itemToDelete = item },
                    )
                }
            }
        }

        // Диалог подтверждения удаления/отписки
        DialogXSubscriptionDelete(
            item = itemToDelete,
            onDismiss = { itemToDelete = null },
            onConfirm = onConfirmDelete,
        )
    }
}

/**
 * Заголовок вкладки подписок со счётчиком.
 */
@Composable
private fun SubscriptionsHeader(
    title: String,
    count: Int,
    isModel: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isModel) Color(0x33DE2600) else Color(0x331E88E5))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        color = if (isModel) Color(0xFFFF5252) else Color(0xFF42A5F5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFF26262C), thickness = 0.5.dp)
    }
}

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

/**
 * Экран пустого состояния для подписок.
 */
@Composable
private fun SubscriptionsEmptyState(
    isModel: Boolean,
    topCutout: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(top = topCutout)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = if (isModel) Icons.Outlined.Person else Icons.Outlined.Tv,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isModel) "У вас нет подписок на актрис" else "У вас нет подписок на каналы",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Перейдите на страницу ${if (isModel) "актрисы" else "канала"} и нажмите кнопку «Подписаться»",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
