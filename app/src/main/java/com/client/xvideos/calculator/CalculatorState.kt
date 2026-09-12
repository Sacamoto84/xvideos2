package com.client.xvideos.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Состояние и математический движок калькулятора.
 *
 * Использует [BigDecimal] для высокой точности без артефактов вещественных чисел Double.
 * Поддерживает честный расчёт процентов, смену знака (+/-), повторение операции по «=»,
 * динамическое переключение «AC» / «C» и подсветку активного оператора.
 */
class CalculatorState(
    initialDisplay: String = "0",
    initialHistory: String = ""
) {
    var displayValue by mutableStateOf(initialDisplay)
    var expressionHistory by mutableStateOf(initialHistory)
    var previousValue by mutableStateOf<BigDecimal?>(null)
    var pendingOperation by mutableStateOf<String?>(null)
    var lastOperand by mutableStateOf<BigDecimal?>(null)
    var lastOperator by mutableStateOf<String?>(null)
    var isNewEntry by mutableStateOf(true)

    /**
     * Показывать ли «AC» (All Clear) вместо «C» (Clear).
     */
    val isAllClear: Boolean
        get() = (displayValue == "0" || isNewEntry) && previousValue == null

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ' '
    }

    private val integerFormatter = DecimalFormat("#,##0", symbols)

    fun onDigit(digit: String, haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (displayValue == ERROR_TEXT || isNewEntry) {
            displayValue = digit
            isNewEntry = false
        } else if (displayValue == "0") {
            displayValue = digit
        } else {
            val rawDigits = displayValue.replace(" ", "").replace("-", "")
            if (rawDigits.length < MAX_INPUT_DIGITS) {
                displayValue += digit
            }
        }
        lastOperator = null
        lastOperand = null
    }

    fun onDecimal(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (displayValue == ERROR_TEXT || isNewEntry) {
            displayValue = "0."
            isNewEntry = false
        } else if (!displayValue.contains(".")) {
            displayValue += "."
        }
    }

    fun onPlusMinus(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (displayValue == "0" || displayValue == ERROR_TEXT) return

        displayValue = if (displayValue.startsWith("-")) {
            displayValue.removePrefix("-")
        } else {
            "-$displayValue"
        }
    }

    fun onOperator(op: String, haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (displayValue == ERROR_TEXT) return

        val current = parseDisplayValue() ?: BigDecimal.ZERO
        val prev = previousValue
        val pending = pendingOperation

        if (prev != null && pending != null && !isNewEntry) {
            val result = executeOperation(prev, current, pending)
            if (result == null) {
                displayValue = ERROR_TEXT
                previousValue = null
                pendingOperation = null
                return
            }
            previousValue = result
            displayValue = formatNumber(result)
            expressionHistory = "${formatNumber(result)} $op"
        } else {
            previousValue = current
            expressionHistory = "${formatNumber(current)} $op"
        }

        pendingOperation = op
        lastOperand = null
        lastOperator = null
        isNewEntry = true
    }

    fun onPercent(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (displayValue == ERROR_TEXT) return

        val current = parseDisplayValue() ?: return
        val prev = previousValue
        val pending = pendingOperation

        val result = if (prev != null && pending != null) {
            if (pending == "+" || pending == "-") {
                // 200 + 10% = 200 + 20
                prev.multiply(current).divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
            } else {
                // 200 × 10% = 200 × 0.1
                current.divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
            }
        } else {
            current.divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
        }

        displayValue = formatNumber(result)
        isNewEntry = true
    }

    fun onEquals(
        scope: CoroutineScope,
        haptic: HapticFeedback,
        onUnlock: suspend (String) -> Boolean
    ) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            val codeToTest = displayValue.replace(" ", "").replace(",", ".")
            val isCandidatePin = previousValue == null &&
                pendingOperation == null &&
                codeToTest.length >= 4 &&
                codeToTest.all { it.isDigit() }

            if (isCandidatePin && onUnlock(codeToTest)) return@launch

            val current = parseDisplayValue() ?: BigDecimal.ZERO
            val prev = previousValue
            val pending = pendingOperation

            if (prev != null && pending != null) {
                val result = executeOperation(prev, current, pending)
                if (result == null) {
                    displayValue = ERROR_TEXT
                    previousValue = null
                    pendingOperation = null
                    return@launch
                }
                expressionHistory = "${formatNumber(prev)} $pending ${formatNumber(current)} ="
                displayValue = formatNumber(result)
                lastOperand = current
                lastOperator = pending
                previousValue = null
                pendingOperation = null
                isNewEntry = true
            } else if (lastOperand != null && lastOperator != null) {
                // Повторение последней операции при повторном нажатии «=»
                val op = lastOperator!!
                val operand = lastOperand!!
                val result = executeOperation(current, operand, op)
                if (result == null) {
                    displayValue = ERROR_TEXT
                    lastOperand = null
                    lastOperator = null
                    return@launch
                }
                expressionHistory = "${formatNumber(current)} $op ${formatNumber(operand)} ="
                displayValue = formatNumber(result)
                isNewEntry = true
            }
        }
    }

    fun onClear(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!isAllClear && displayValue != "0") {
            displayValue = "0"
            isNewEntry = true
        } else {
            displayValue = "0"
            previousValue = null
            pendingOperation = null
            lastOperand = null
            lastOperator = null
            expressionHistory = ""
            isNewEntry = true
        }
    }

    fun onBackspace(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (displayValue == ERROR_TEXT) {
            displayValue = "0"
            isNewEntry = true
            return
        }

        if (!isNewEntry) {
            val clean = displayValue.removePrefix("-")
            if (clean.length > 1) {
                displayValue = displayValue.dropLast(1)
            } else {
                displayValue = "0"
                isNewEntry = true
            }
        }
    }

    private fun parseDisplayValue(): BigDecimal? =
        runCatching { BigDecimal(displayValue.replace(" ", "").replace(",", ".")) }.getOrNull()

    private fun executeOperation(a: BigDecimal, b: BigDecimal, op: String): BigDecimal? = when (op) {
        "+" -> a.add(b)
        "-" -> a.subtract(b)
        "×" -> a.multiply(b)
        "÷" -> {
            if (b.compareTo(BigDecimal.ZERO) == 0) {
                null
            } else {
                a.divide(b, CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
            }
        }
        else -> b
    }

    private fun formatNumber(number: BigDecimal): String {
        val stripped = number.stripTrailingZeros()
        val plain = stripped.toPlainString()

        if (plain.contains(".")) {
            val parts = plain.split(".")
            val intPart = parts[0].toLongOrNull()?.let { integerFormatter.format(it) } ?: parts[0]
            return "$intPart.${parts[1]}"
        }

        return plain.toLongOrNull()?.let { integerFormatter.format(it) } ?: plain
    }

    companion object {
        const val ERROR_TEXT = "Ошибка"
        private const val MAX_INPUT_DIGITS = 15
        private const val CALC_SCALE = 12
        private val ONE_HUNDRED = BigDecimal("100")

        val Saver: Saver<CalculatorState, List<String?>> = Saver(
            save = {
                listOf(
                    it.displayValue,
                    it.expressionHistory,
                    it.previousValue?.toPlainString(),
                    it.pendingOperation,
                    it.lastOperand?.toPlainString(),
                    it.lastOperator,
                    it.isNewEntry.toString()
                )
            },
            restore = {
                CalculatorState(
                    initialDisplay = it.getOrNull(0) ?: "0",
                    initialHistory = it.getOrNull(1) ?: ""
                ).apply {
                    previousValue = it.getOrNull(2)?.let { str -> runCatching { BigDecimal(str) }.getOrNull() }
                    pendingOperation = it.getOrNull(3)
                    lastOperand = it.getOrNull(4)?.let { str -> runCatching { BigDecimal(str) }.getOrNull() }
                    lastOperator = it.getOrNull(5)
                    isNewEntry = it.getOrNull(6)?.toBooleanStrictOrNull() ?: true
                }
            }
        )
    }
}
