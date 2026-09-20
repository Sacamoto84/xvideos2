package com.client.xvideos.calculator

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorStateTest {

    private val noOpHaptic = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
    }

    @Test
    fun `ввод цифр и вычисление сложения работают корректно`() = runTest {
        val state = CalculatorState()
        state.onDigit("1", noOpHaptic)
        state.onDigit("2", noOpHaptic)
        assertEquals("12", state.displayValue)

        state.onOperator("+", noOpHaptic)
        state.onDigit("3", noOpHaptic)
        state.onDigit("4", noOpHaptic)
        assertEquals("34", state.displayValue)

        var unlocked = false
        state.onEquals(this, noOpHaptic) { code ->
            if (code == "1234") {
                unlocked = true
                true
            } else {
                false
            }
        }
        testScheduler.advanceUntilIdle()

        assertFalse(unlocked)
        assertEquals("46", state.displayValue)
    }

    @Test
    fun `точность BigDecimal исключает артефакты вещественных чисел`() = runTest {
        val state = CalculatorState()
        state.onDigit("0", noOpHaptic)
        state.onDecimal(noOpHaptic)
        state.onDigit("1", noOpHaptic)
        state.onOperator("+", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onDecimal(noOpHaptic)
        state.onDigit("2", noOpHaptic)

        state.onEquals(this, noOpHaptic) { false }
        testScheduler.advanceUntilIdle()

        assertEquals("0.3", state.displayValue)
    }

    @Test
    fun `смена знака плюс-минус инвертирует число`() {
        val state = CalculatorState()
        state.onDigit("5", noOpHaptic)
        assertEquals("5", state.displayValue)

        state.onPlusMinus(noOpHaptic)
        assertEquals("-5", state.displayValue)

        state.onPlusMinus(noOpHaptic)
        assertEquals("5", state.displayValue)
    }

    @Test
    fun `проценты при сложении считают процент от базы`() = runTest {
        val state = CalculatorState()
        state.onDigit("2", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onOperator("+", noOpHaptic)
        state.onDigit("1", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onPercent(noOpHaptic)
        assertEquals("20", state.displayValue)

        state.onEquals(this, noOpHaptic) { false }
        testScheduler.advanceUntilIdle()

        assertEquals("220", state.displayValue)
    }

    @Test
    fun `проценты при вычитании считают процент от базы`() = runTest {
        val state = CalculatorState()
        state.onDigit("1", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onOperator("-", noOpHaptic)
        state.onDigit("2", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onPercent(noOpHaptic)
        assertEquals("20", state.displayValue)

        state.onEquals(this, noOpHaptic) { false }
        testScheduler.advanceUntilIdle()

        assertEquals("80", state.displayValue)
    }

    @Test
    fun `одиночный процент делит на сто`() {
        val state = CalculatorState()
        state.onDigit("5", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onPercent(noOpHaptic)
        assertEquals("0.5", state.displayValue)
    }

    @Test
    fun `повторное нажатие равно повторяет последнюю операцию`() = runTest {
        val state = CalculatorState()
        state.onDigit("5", noOpHaptic)
        state.onOperator("+", noOpHaptic)
        state.onDigit("2", noOpHaptic)

        state.onEquals(this, noOpHaptic) { false }
        testScheduler.advanceUntilIdle()
        assertEquals("7", state.displayValue)

        state.onEquals(this, noOpHaptic) { false }
        testScheduler.advanceUntilIdle()
        assertEquals("9", state.displayValue)

        state.onEquals(this, noOpHaptic) { false }
        testScheduler.advanceUntilIdle()
        assertEquals("11", state.displayValue)
    }

    @Test
    fun `динамическая кнопка AC и C`() {
        val state = CalculatorState()
        assertTrue(state.isAllClear)

        state.onDigit("9", noOpHaptic)
        assertFalse(state.isAllClear)

        state.onClear(noOpHaptic)
        assertTrue(state.isAllClear)
        assertEquals("0", state.displayValue)
    }

    @Test
    fun `ввод верного кода разблокировки триггерит onUnlock`() = runTest {
        val state = CalculatorState()
        state.onDigit("7", noOpHaptic)
        state.onDigit("7", noOpHaptic)
        state.onDigit("8", noOpHaptic)
        state.onDigit("8", noOpHaptic)
        assertEquals("7788", state.displayValue)

        var unlockedCode = ""
        state.onEquals(this, noOpHaptic) { code ->
            unlockedCode = code
            code == "7788"
        }
        testScheduler.advanceUntilIdle()

        assertEquals("7788", unlockedCode)
    }

    @Test
    fun `деление на ноль отображает Ошибка`() = runTest {
        val state = CalculatorState()
        state.onDigit("9", noOpHaptic)
        state.onOperator("÷", noOpHaptic)
        state.onDigit("0", noOpHaptic)

        state.onEquals(this, noOpHaptic) { false }
        testScheduler.advanceUntilIdle()

        assertEquals("Ошибка", state.displayValue)
    }

    @Test
    fun `кнопка бекспейс стирает последнюю цифру`() {
        val state = CalculatorState()
        state.onDigit("1", noOpHaptic)
        state.onDigit("2", noOpHaptic)
        state.onDigit("3", noOpHaptic)
        assertEquals("123", state.displayValue)

        state.onBackspace(noOpHaptic)
        assertEquals("12", state.displayValue)
    }

    @Test
    fun `арифметические операции не вызывают onUnlock`() = runTest {
        val state = CalculatorState()
        state.onDigit("5", noOpHaptic)
        state.onOperator("+", noOpHaptic)
        state.onDigit("5", noOpHaptic)

        var unlockCalled = false
        state.onEquals(this, noOpHaptic) {
            unlockCalled = true
            false
        }
        testScheduler.advanceUntilIdle()

        assertFalse("onUnlock не должен вызываться для незавершённых выражений", unlockCalled)
        assertEquals("10", state.displayValue)
    }

    @Test
    fun `Saver корректно сохраняет и восстанавливает полное состояние вычислений`() {
        val state = CalculatorState()
        state.onDigit("4", noOpHaptic)
        state.onDigit("2", noOpHaptic)
        state.onOperator("×", noOpHaptic)

        val saverScope = androidx.compose.runtime.saveable.SaverScope { true }
        val saved = with(CalculatorState.Saver) { saverScope.save(state) }
        val restored = checkNotNull(saved?.let { CalculatorState.Saver.restore(it) })

        assertEquals("42", restored.displayValue)
        assertEquals("42 ×", restored.expressionHistory)
        assertEquals("×", restored.pendingOperation)
        assertTrue(restored.isNewEntry)
    }

    @Test
    fun `isVerifying блокирует повторные параллельные вызовы onUnlock`() = runTest {
        val state = CalculatorState()
        state.onDigit("1", noOpHaptic)
        state.onDigit("2", noOpHaptic)
        state.onDigit("3", noOpHaptic)
        state.onDigit("4", noOpHaptic)

        var callCount = 0
        state.onEquals(this, noOpHaptic) {
            callCount++
            kotlinx.coroutines.delay(100)
            false
        }

        // Повторный клик во время активной проверки
        state.onEquals(this, noOpHaptic) {
            callCount++
            false
        }

        testScheduler.advanceUntilIdle()

        assertEquals("onUnlock должен быть вызван только один раз", 1, callCount)
        assertFalse(state.isVerifying)
    }

    @Test
    fun `PIN с ведущими нулями 0000 корректно разблокирует калькулятор`() = runTest {
        val state = CalculatorState()
        state.onDigit("0", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onDigit("0", noOpHaptic)
        state.onDigit("0", noOpHaptic)

        var unlocked = false
        state.onEquals(this, noOpHaptic) { code ->
            if (code == "0000") {
                unlocked = true
                true
            } else {
                false
            }
        }
        testScheduler.advanceUntilIdle()

        assertTrue("PIN 0000 должен разблокировать калькулятор", unlocked)
    }

    @Test
    fun `PIN с ведущим нулём 0123 корректно разблокирует калькулятор`() = runTest {
        val state = CalculatorState()
        state.onDigit("0", noOpHaptic)
        state.onDigit("1", noOpHaptic)
        state.onDigit("2", noOpHaptic)
        state.onDigit("3", noOpHaptic)

        var unlocked = false
        state.onEquals(this, noOpHaptic) { code ->
            if (code == "0123") {
                unlocked = true
                true
            } else {
                false
            }
        }
        testScheduler.advanceUntilIdle()

        assertTrue("PIN 0123 должен разблокировать калькулятор", unlocked)
    }

    @Test
    fun `onBackspace и onClear корректно управляют PIN-буфером`() = runTest {
        val state = CalculatorState()
        state.onDigit("0", noOpHaptic)
        state.onDigit("1", noOpHaptic)
        state.onDigit("2", noOpHaptic)
        state.onDigit("3", noOpHaptic)
        state.onDigit("9", noOpHaptic)
        state.onBackspace(noOpHaptic)

        var unlocked = false
        state.onEquals(this, noOpHaptic) { code ->
            if (code == "0123") {
                unlocked = true
                true
            } else {
                false
            }
        }
        testScheduler.advanceUntilIdle()

        assertTrue("После Backspace PIN 0123 должен подойти", unlocked)

        // Теперь проверка onClear
        state.onClear(noOpHaptic)
        state.onDigit("5", noOpHaptic)
        state.onDigit("6", noOpHaptic)
        state.onDigit("7", noOpHaptic)
        state.onDigit("8", noOpHaptic)

        var unlocked2 = false
        state.onEquals(this, noOpHaptic) { code ->
            if (code == "5678") {
                unlocked2 = true
                true
            } else {
                false
            }
        }
        testScheduler.advanceUntilIdle()

        assertTrue("После onClear новый PIN 5678 должен подойти", unlocked2)
    }

    @Test
    fun `onUnlockFailed вызывается ровно один раз при неудачной попытке ввода PIN`() = runTest {
        val state = CalculatorState()
        state.onDigit("9", noOpHaptic)
        state.onDigit("9", noOpHaptic)
        state.onDigit("9", noOpHaptic)
        state.onDigit("9", noOpHaptic)

        var failedAttempts = 0
        state.onEquals(
            scope = this,
            haptic = noOpHaptic,
            onUnlock = { false },
            onUnlockFailed = { failedAttempts++ }
        )
        testScheduler.advanceUntilIdle()

        assertEquals(1, failedAttempts)
    }

    @Test
    fun `onBackspace корректно удаляет цифры из форматированного числа с пробелами`() {
        val state = CalculatorState()
        state.displayValue = "-1 234"
        state.isNewEntry = false

        state.onBackspace(noOpHaptic)
        assertEquals("-123", state.displayValue)

        state.onBackspace(noOpHaptic)
        assertEquals("-12", state.displayValue)

        state.onBackspace(noOpHaptic)
        assertEquals("-1", state.displayValue)

        state.onBackspace(noOpHaptic)
        assertEquals("0", state.displayValue)
        assertTrue(state.isNewEntry)
    }

    @Test
    fun `вычитание дающее отрицательную дробь между 0 и -1 сохраняет знак минус`() = runTest {
        val state = CalculatorState()
        state.onDigit("1", noOpHaptic)
        state.onOperator("-", noOpHaptic)
        state.onDigit("1", noOpHaptic)
        state.onDecimal(noOpHaptic)
        state.onDigit("5", noOpHaptic)

        state.onEquals(this, noOpHaptic) { false }
        testScheduler.advanceUntilIdle()

        assertEquals("-0.5", state.displayValue)
    }

    @Test
    fun `formatNumber сохраняет знак минус для отрицательных дробей`() {
        val state = CalculatorState()
        assertEquals("-0.5", state.formatNumber(java.math.BigDecimal("-0.5")))
        assertEquals("-0.05", state.formatNumber(java.math.BigDecimal("-0.05")))
        assertEquals("-1 000.5", state.formatNumber(java.math.BigDecimal("-1000.5")))
    }

    @Test
    fun `ввод цифры после минус нуля заменяет ноль без артефакта ведущего нуля`() {
        val state = CalculatorState()
        state.displayValue = "-0"
        state.isNewEntry = false
        state.onDigit("7", noOpHaptic)
        assertEquals("-7", state.displayValue)

        state.displayValue = "-0"
        state.isNewEntry = false
        state.onDigit("0", noOpHaptic)
        assertEquals("-0", state.displayValue)
    }

    @Test
    fun `пошаговый onBackspace при вводе цифр переводит состояние в isAllClear`() {
        val state = CalculatorState()
        state.onDigit("1", noOpHaptic)
        state.onDigit("2", noOpHaptic)
        state.onDigit("3", noOpHaptic)
        assertEquals("123", state.displayValue)
        assertFalse(state.isAllClear)

        state.onBackspace(noOpHaptic)
        assertEquals("12", state.displayValue)
        assertFalse(state.isAllClear)

        state.onBackspace(noOpHaptic)
        assertEquals("1", state.displayValue)
        assertFalse(state.isAllClear)

        state.onBackspace(noOpHaptic)
        assertEquals("0", state.displayValue)
        assertTrue(state.isAllClear)
    }

    @Test
    fun `onBackspace при ошибке вычислений сбрасывает в ноль и isAllClear`() {
        val state = CalculatorState()
        state.displayValue = CalculatorState.ERROR_TEXT
        assertFalse(state.isAllClear)

        state.onBackspace(noOpHaptic)
        assertEquals("0", state.displayValue)
        assertTrue(state.isAllClear)
    }

    @Test
    fun `onClear при незавершённой бинарной операции сбрасывает состояние в isAllClear`() {
        val state = CalculatorState()
        state.onDigit("5", noOpHaptic)
        state.onOperator("+", noOpHaptic)
        assertFalse(state.isAllClear)
        assertEquals("+", state.pendingOperation)

        // Первый onClear сбрасывает текущее значение в 0 (режим C)
        state.onClear(noOpHaptic)
        assertEquals("0", state.displayValue)
        assertFalse(state.isAllClear)

        // Второй onClear сбрасывает операцию и возвращает полное состояние All Clear (режим AC)
        state.onClear(noOpHaptic)
        assertEquals("0", state.displayValue)
        assertTrue(state.isAllClear)
        org.junit.Assert.assertNull(state.pendingOperation)
    }
}
