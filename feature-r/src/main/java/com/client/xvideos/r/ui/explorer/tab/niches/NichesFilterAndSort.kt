package com.client.xvideos.r.ui.explorer.tab.niches

import com.client.xvideos.r.model.Niche
import com.client.xvideos.r.model.Order

internal fun filterAndSortNiches(
    niches: List<Niche>,
    query: String,
    order: Order
): List<Niche> {
    val filtered = if (query.isBlank()) {
        niches
    } else {
        niches.filter { it.name.contains(query, ignoreCase = true) }
    }

    return when (order) {
        Order.NICHES_SUBSCRIBERS_D -> filtered.sortedByDescending { it.subscribers }
        Order.NICHES_POST_D -> filtered.sortedByDescending { it.gifs }
        Order.NICHES_SUBSCRIBERS_A -> filtered.sortedBy { it.subscribers }
        Order.NICHES_POST_A -> filtered.sortedBy { it.gifs }
        Order.NICHES_NAME_A_Z -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        Order.NICHES_NAME_Z_A -> filtered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
        else -> filtered.sortedBy { it.subscribers }
    }
}
