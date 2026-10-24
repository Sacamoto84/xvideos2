package com.client.xvideos.x.screens.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * Горизонтальный ряд внутри горизонтального пейджера: остаток прокрутки ряда
 * (он упёрся в край) забирается себе и до пейджера не доходит.
 *
 * Без этого длинный свайп по ряду доводил его до конца, а остаток листал
 * страницу пейджера. Ставится в цепочке **перед** `horizontalScroll` ряда.
 */
internal object KeepHorizontalScroll : NestedScrollConnection {

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        Offset(available.x, 0f)

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(available.x, 0f)
}
