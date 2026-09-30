package com.client.xvideos.l.ui.screens.screenFullScreen

import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.selectionKey

internal fun resolveInitialIndex(items: List<PicsDetails>, target: PicsDetails): Int {
    if (items.isEmpty()) return 0
    val exact = items.indexOf(target)
    if (exact >= 0) return exact
    val targetKey = target.selectionKey()
    val byKey = items.indexOfFirst { it.selectionKey() == targetKey }
    if (byKey >= 0) return byKey
    return 0
}

internal fun resolveScrollIndex(currentIndex: Int, maxIndex: Int): Int =
    (currentIndex - 2).coerceIn(0, maxIndex.coerceAtLeast(0))
