package com.client.xvideos.x.screens.subscriptions

import com.client.xvideos.x.model.XSubscriptionItem

/**
 * Подписки [items] в порядке [order]. Сортировка устойчивая: при равных ключах
 * остаётся порядок с диска (от новых файлов к старым).
 */
fun xSortedSubscriptions(items: List<XSubscriptionItem>, order: XSubscriptionsOrder): List<XSubscriptionItem> =
    when (order) {
        XSubscriptionsOrder.NEWEST -> items.sortedByDescending { it.dateAdded }
        XSubscriptionsOrder.OLDEST -> items.sortedBy { it.dateAdded }
        XSubscriptionsOrder.NAME -> items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
        XSubscriptionsOrder.VIDEOS -> items.sortedWith(
            compareByDescending<XSubscriptionItem> { it.videoCount }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
        )
    }

/** Порядок по номеру из настроек; неизвестный номер — порядок по умолчанию. */
fun xSubscriptionsOrderOf(ordinal: Int): XSubscriptionsOrder =
    XSubscriptionsOrder.entries.getOrElse(ordinal) { XSubscriptionsOrder.NEWEST }
