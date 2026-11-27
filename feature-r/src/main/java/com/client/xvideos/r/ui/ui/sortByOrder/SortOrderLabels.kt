package com.client.xvideos.r.ui.ui.sortByOrder

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import com.client.xvideos.r.model.Order

internal val SORT_ORDER_TEXT_AUTO_SIZE = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 18.sp)

internal fun Order.toDisplayName(): String = when (this) {
    Order.TOP -> "Top"
    Order.LATEST -> "Latest"
    Order.TRENDING -> "Trending"
    Order.HOT -> "Hot"
    Order.OLDEST -> "Oldest"
    Order.TOP28 -> "Top28"
    Order.RELEVANT -> "Relevant"
    Order.TOP_WEEK -> "Week"
    Order.TOP_MONTH -> "Month"
    Order.NICHES_SUBSCRIBERS_D -> "Subscribers↓"
    Order.NICHES_POST_D -> "Post↓"
    Order.NICHES_SUBSCRIBERS_A -> "Subscribers↑"
    Order.NICHES_POST_A -> "Post↑"
    Order.NICHES_NAME_A_Z -> "Name A-Z"
    Order.NICHES_NAME_Z_A -> "Name Z-A"
}

internal fun Order.toIconLabel(): String = when (this) {
    Order.TOP_WEEK -> "Топ недели"
    Order.TOP_MONTH, Order.TOP28 -> "Топ месяца"
    Order.TOP -> "Топ за всё время"
    Order.TRENDING -> "В тренде"
    Order.HOT -> "Горячее"
    Order.LATEST -> "Новые"
    Order.OLDEST -> "Старые"
    Order.RELEVANT -> "По релевантности"
    Order.NICHES_SUBSCRIBERS_D -> "Больше подписчиков"
    Order.NICHES_SUBSCRIBERS_A -> "Меньше подписчиков"
    Order.NICHES_POST_D -> "Больше публикаций"
    Order.NICHES_POST_A -> "Меньше публикаций"
    Order.NICHES_NAME_A_Z -> "Название A–Z"
    Order.NICHES_NAME_Z_A -> "Название Z–A"
}
