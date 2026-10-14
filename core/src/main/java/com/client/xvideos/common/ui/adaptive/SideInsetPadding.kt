package com.client.xvideos.common.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Тот же отступ, но с прибавкой [side] слева и справа.
 *
 * Нулевая прибавка возвращает сам [this] — на телефоне список получает ровно
 * прежний `contentPadding`.
 */
fun PaddingValues.plusSides(side: Dp): PaddingValues =
    if (side <= 0.dp) this else SideInsetPadding(this, side)

private class SideInsetPadding(
    private val base: PaddingValues,
    private val side: Dp,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateLeftPadding(layoutDirection) + side

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateRightPadding(layoutDirection) + side

    override fun calculateTopPadding(): Dp = base.calculateTopPadding()

    override fun calculateBottomPadding(): Dp = base.calculateBottomPadding()

    override fun equals(other: Any?): Boolean =
        other is SideInsetPadding && other.base == base && other.side == side

    override fun hashCode(): Int = 31 * base.hashCode() + side.hashCode()
}
