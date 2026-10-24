package com.client.xvideos.x.screens.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Горизонтальный ряд внутри пейджера: остаток прокрутки ряда пейджер не листает. */
class KeepHorizontalScrollTest {

    @Test
    fun `остаток горизонтальной прокрутки остаётся у ряда`() {
        val kept = KeepHorizontalScroll.onPostScroll(Offset(-10f, 0f), Offset(-30f, 5f), NestedScrollSource.UserInput)

        assertEquals(Offset(-30f, 0f), kept)
    }

    @Test
    fun `остаток горизонтального броска тоже`() {
        val kept = runBlocking { KeepHorizontalScroll.onPostFling(Velocity.Zero, Velocity(-500f, 40f)) }

        assertEquals(Velocity(-500f, 0f), kept)
    }
}
