package com.client.xvideos.n.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

internal object NCardLayout {
    const val aspectRatio = 137f / 200f
    val gridPadding = PaddingValues(1.dp)
}

/** Одинаковая ширина ячеек, даже если ширина строки не делится на число колонок. */
internal data class NCardColumns(private val count: Int) : GridCells {
    init {
        require(count > 0)
    }

    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val cellWidth = (availableSize - spacing * (count - 1)).coerceAtLeast(0) / count
        return List(count) { cellWidth }
    }
}
