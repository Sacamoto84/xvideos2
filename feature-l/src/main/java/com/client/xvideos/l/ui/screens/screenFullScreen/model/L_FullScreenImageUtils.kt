package com.client.xvideos.l.ui.screens.screenFullScreen.model

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

/**
 * Номер картинки для пользователя: с единицы и с общим числом. Один на метку
 * в углу экрана и на окно сведений — раньше метка показывала индекс с нуля.
 */
internal fun lPicturePositionLabel(position: Int, total: Int): String = "${position + 1} / $total"
