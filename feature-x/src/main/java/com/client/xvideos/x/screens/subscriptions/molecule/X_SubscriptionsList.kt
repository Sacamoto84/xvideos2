package com.client.xvideos.x.screens.subscriptions.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.x.model.XSubscriptionItem
import com.client.xvideos.x.screens.subscriptions.XSubscriptionsOrder
import com.client.xvideos.x.screens.subscriptions.atom.SubscriptionsEmptyState
import com.client.xvideos.x.screens.subscriptions.atom.SubscriptionsHeader

/**
 * Список подписок на каналы или моделей с шапкой и счётчиком; пустой — заглушка.
 * [itemsList] уже в порядке [order]; значок сортировки в шапке меняет его через
 * [onOrderChange].
 */
@Composable
fun X_SubscriptionsList(
    itemsList: List<XSubscriptionItem>,
    isModel: Boolean,
    topCutout: androidx.compose.ui.unit.Dp,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onOpenCreator: (XSubscriptionItem) -> Unit = {},
    onDeleteRequest: (XSubscriptionItem) -> Unit = {},
    onImageError: (XSubscriptionItem) -> Unit = {},
    order: XSubscriptionsOrder = XSubscriptionsOrder.NEWEST,
    onOrderChange: (XSubscriptionsOrder) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val title = if (isModel) "Подписки на актрис" else "Подписки на каналы"
    if (itemsList.isEmpty()) {
        // Шапка есть и у пустой вкладки — как у «Избранного» и «Сохранённого».
        Column(modifier = modifier.fillMaxSize()) {
            SubscriptionsHeader(title = title, count = 0, isModel = isModel, topCutout = topCutout)
            SubscriptionsEmptyState(
                isModel = isModel,
                topCutout = 0.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topCutout, bottom = 24.dp)
        ) {
            // Шапка — на всю ширину, строки — в полосе 640dp по одной. Без ключа:
            // строка "header" совпала бы со слагом подписки и уронила бы список.
            item(contentType = "header") {
                SubscriptionsHeader(
                    title = title,
                    count = itemsList.size,
                    isModel = isModel,
                    actions = { SubscriptionsSortButton(order = order, onOrderChange = onOrderChange) }
                )
            }

            items(
                items = itemsList,
                key = { it.cleanSlug },
                contentType = { "subscription_item" }
            ) { item ->
                SubscriptionListItem(
                    item = item,
                    isModel = isModel,
                    onClick = { onOpenCreator(item) },
                    onLongClick = { onDeleteRequest(item) },
                    onDeleteClick = { onDeleteRequest(item) },
                    onImageError = { onImageError(item) },
                    modifier = Modifier.adaptiveContentWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun X_SubscriptionsListPreview() {
    X_SubscriptionsList(
        itemsList = emptyList(),
        isModel = true,
        topCutout = 0.dp
    )
}
