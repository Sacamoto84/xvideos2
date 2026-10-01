package com.client.xvideos.x.screens.subscriptions.molecule

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.XSubscriptionItem
import com.client.xvideos.x.screens.subscriptions.atom.SubscriptionsEmptyState
import com.client.xvideos.x.screens.subscriptions.atom.SubscriptionsHeader

/**
 * Список подписок на каналы или моделей с шапкой и счётчиком; пустой — заглушка.
 */
@Composable
fun X_SubscriptionsList(
    itemsList: List<XSubscriptionItem>,
    isModel: Boolean,
    topCutout: androidx.compose.ui.unit.Dp,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onOpenCreator: (XSubscriptionItem) -> Unit = {},
    onDeleteRequest: (XSubscriptionItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (itemsList.isEmpty()) {
        SubscriptionsEmptyState(
            isModel = isModel,
            topCutout = topCutout,
            modifier = modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topCutout, bottom = 24.dp)
        ) {
            item(key = "header", contentType = "header") {
                SubscriptionsHeader(
                    title = if (isModel) "Подписки на актрис" else "Подписки на каналы",
                    count = itemsList.size,
                    isModel = isModel,
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
