package com.client.xvideos.n.ui

import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Test

class NCardColumnsTest {
    @Test
    fun `остаток пикселей не делает первую карточку шире соседней`() {
        with(NCardColumns(2)) {
            assertEquals(listOf(535, 535), Density(2.4375f).calculateCrossAxisCellSizes(1076, 5))
        }
        with(NCardColumns(4)) {
            assertEquals(List(4) { 386 }, Density(2.4375f).calculateCrossAxisCellSizes(1560, 5))
        }
    }
}
