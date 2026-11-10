package com.client.xvideos.calculator

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScientificCalculatorTest {

    private val noOpHaptic = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
    }

    @Test
    fun `sin 30 градусов равен 0_5`() {
        val state = CalculatorState()
        assertFalse(state.isRad) // по умолчанию градусы (DEG)

        state.onDigit("3", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onScientificUnary("sin", noOpHaptic)

        assertEquals("0.5", state.displayValue)
    }

    @Test
    fun `cos 0 градусов равен 1`() {
        val state = CalculatorState()
        state.onDigit("0", noOpHaptic)
        state.onScientificUnary("cos", noOpHaptic)

        assertEquals("1", state.displayValue)
    }

    @Test
    fun `квадратный корень из 16 равен 4`() {
        val state = CalculatorState()
        state.onDigit("1", noOpHaptic)
        state.onDigit("6", noOpHaptic)
        state.onScientificUnary("√", noOpHaptic)

        assertEquals("4", state.displayValue)
    }

    @Test
    fun `квадратный корень из отрицательного числа дает Ошибка`() {
        val state = CalculatorState()
        state.onDigit("4", noOpHaptic)
        state.onPlusMinus(noOpHaptic)
        state.onScientificUnary("√", noOpHaptic)

        assertEquals(CalculatorState.ERROR_TEXT, state.displayValue)
    }

    @Test
    fun `возведение в квадрат`() {
        val state = CalculatorState()
        state.onDigit("5", noOpHaptic)
        state.onScientificUnary("x²", noOpHaptic)

        assertEquals("25", state.displayValue)
    }

    @Test
    fun `факториал 5 равен 120`() {
        val state = CalculatorState()
        state.onDigit("5", noOpHaptic)
        state.onScientificUnary("x!", noOpHaptic)

        assertEquals("120", state.displayValue)
    }

    @Test
    fun `обратное число 1 делить на 4 равно 0_25`() {
        val state = CalculatorState()
        state.onDigit("4", noOpHaptic)
        state.onScientificUnary("1/x", noOpHaptic)

        assertEquals("0.25", state.displayValue)
    }

    @Test
    fun `константа пи вставляет 3_1415926536`() {
        val state = CalculatorState()
        state.onConstant("π", noOpHaptic)

        assertTrue(state.displayValue.startsWith("3.1415"))
    }

    @Test
    fun `переключение инженерного режима и единиц измерения углов`() {
        val state = CalculatorState()
        assertFalse(state.isScientific)
        assertFalse(state.isRad)

        state.toggleScientific()
        assertTrue(state.isScientific)

        state.toggleAngleUnit()
        assertTrue(state.isRad)
    }
}
