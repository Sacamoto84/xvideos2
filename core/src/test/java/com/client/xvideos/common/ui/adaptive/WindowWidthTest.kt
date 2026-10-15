package com.client.xvideos.common.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowWidthTest {

    @Test
    fun `на Compact ширина - из конфигурации, как до адаптации`() {
        // Окно округляется вниз, а screenWidthDp на новых API — к ближайшему:
        // без этого карточки рядов L на телефоне стали бы на пиксель уже.
        assertEquals(412, windowWidthDp(containerWidth = 411.6.dp, configWidthDp = 412))
        assertEquals(411, windowWidthDp(containerWidth = 411.dp, configWidthDp = 411))
    }

    @Test
    fun `вне Compact ширина - окно, округлённое вниз`() {
        assertEquals(852, windowWidthDp(containerWidth = 852.9.dp, configWidthDp = 853))
        assertEquals(700, windowWidthDp(containerWidth = 700.dp, configWidthDp = 700))
    }

    @Test
    fun `класс ширины решает окно, а не конфигурация`() {
        // Сложение: окно уже узкое, конфигурация ещё от раскрытого экрана.
        assertEquals(443, windowWidthDp(containerWidth = 443.dp, configWidthDp = 852))
        // Раскрытие: окно уже широкое, конфигурация ещё от внешнего экрана.
        assertEquals(852, windowWidthDp(containerWidth = 852.dp, configWidthDp = 443))
        // Граница: окно 599.6dp — Compact, конфигурация округлила до 600.
        assertEquals(599, windowWidthDp(containerWidth = 599.6.dp, configWidthDp = 600))
    }

    @Test
    fun `без окна - ширина из конфигурации`() {
        assertEquals(411, windowWidthDp(containerWidth = Dp.Unspecified, configWidthDp = 411))
        assertEquals(852, windowWidthDp(containerWidth = Dp.Unspecified, configWidthDp = 852))
    }
}
