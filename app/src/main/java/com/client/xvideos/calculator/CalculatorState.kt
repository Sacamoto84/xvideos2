package com.client.xvideos.calculator

import androidx.compose.runtime.Stable
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
 * динамическое переключение «AC» / «C», секретный ввод пин-кода и подсветку активного оператора.
 *
 * @param initialDisplay Начальное строковое значение на экране (по умолчанию "0").
 * @param initialHistory Начальная история вычисления.
 */
@Stable
class CalculatorState(
    initialDisplay: String = "0",
    initialHistory: String = ""
) {
    /** Текущее отображаемое на дисплее число или статус ошибки. */
    var displayValue by mutableStateOf(initialDisplay)
    /** Строка истории текущего математического выражения (например, "200 +"). */
    var expressionHistory by mutableStateOf(initialHistory)
    /** Предыдущее введенное число для бинарных операций. */
    var previousValue by mutableStateOf<BigDecimal?>(null)
    /** Текущий ожидающий выполнения математический оператор ("+", "-", "×", "÷"). */
    var pendingOperation by mutableStateOf<String?>(null)
    /** Последний операнд для повторения операции по повторному нажатию "=". */
    var lastOperand by mutableStateOf<BigDecimal?>(null)
    /** Последний оператор для повторения операции по повторному нажатию "=". */
    var lastOperator by mutableStateOf<String?>(null)
    /** Флаг начала ввода нового числа (следующая цифра заменяет текущий дисплей). */
    var isNewEntry by mutableStateOf(true)
    /** Флаг выполнения асинхронной проверки пароля разблокировки. */
    var isVerifying by mutableStateOf(false)
        private set

    /**
     * Показывать ли «AC» (All Clear) вместо «C» (Clear).
     */
    val isAllClear: Boolean
        get() = displayValue != ERROR_TEXT && (displayValue == "0" || isNewEntry) && previousValue == null

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ' '
    }

    private val integerFormatter = DecimalFormat("#,##0", symbols)

    private val enteredPinDigits = StringBuilder()

    /**
     * Обрабатывает ввод цифры [digit] с тактильным откликом.
     *
     * @param digit Введенная цифра ("0".."9").
     * @param haptic Интерфейс тактильной отдачи.
     */
    fun onDigit(digit: String, haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (enteredPinDigits.length < MAX_INPUT_DIGITS) {
            enteredPinDigits.append(digit)
        }

        if (displayValue == ERROR_TEXT || isNewEntry) {
            displayValue = digit
            isNewEntry = false
        } else if (displayValue == "0") {
            displayValue = digit
        } else if (displayValue == "-0") {
            displayValue = if (digit == "0") "-0" else "-$digit"
        } else {
            val rawDigits = displayValue.replace(" ", "").replace("-", "")
            if (rawDigits.length < MAX_INPUT_DIGITS) {
                displayValue += digit
            }
        }
        lastOperator = null
        lastOperand = null
    }

    /**
     * Обрабатывает нажатие десятичной точки.
     */
    fun onDecimal(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        enteredPinDigits.clear()

        if (displayValue == ERROR_TEXT || isNewEntry) {
            displayValue = "0."
            isNewEntry = false
        } else if (!displayValue.contains(".")) {
            displayValue += "."
        }
    }

    /**
     * Инвертирует математический знак текущего отображаемого числа (+/-).
     */
    fun onPlusMinus(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        enteredPinDigits.clear()
        if (displayValue == "0" || displayValue == ERROR_TEXT) return

        displayValue = if (displayValue.startsWith("-")) {
            displayValue.removePrefix("-")
        } else {
            "-$displayValue"
        }
    }

    /**
     * Устанавливает или вычисляет промежуточный результат при нажатии бинарного оператора [op] (+, -, ×, ÷).
     */
    fun onOperator(op: String, haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        enteredPinDigits.clear()
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

    /**
     * Вычисляет процент в зависимости от наличия незавершенной операции сложения/вычитания.
     */
    fun onPercent(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        enteredPinDigits.clear()
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

    private fun isPinCandidate(s: String): Boolean = s.length >= 4 && s.all { it.isDigit() }

    private fun getCandidatePin(codeToTest: String): String? {
        if (previousValue != null || pendingOperation != null) return null
        val rawPin = enteredPinDigits.toString()
        return when {
            isPinCandidate(rawPin) -> rawPin
            isPinCandidate(codeToTest) -> codeToTest
            else -> null
        }
    }

    private suspend fun attemptUnlock(
        candidatePin: String,
        codeToTest: String,
        onUnlock: suspend (String) -> Boolean
    ): Boolean {
        val unlocked = onUnlock(candidatePin)
        if (!unlocked && candidatePin != codeToTest && isPinCandidate(codeToTest)) {
            return onUnlock(codeToTest)
        }
        return unlocked
    }

    private fun evaluateCalculation(current: BigDecimal) {
        val prev = previousValue
        val pending = pendingOperation

        if (prev != null && pending != null) {
            val result = executeOperation(prev, current, pending)
            if (result == null) {
                displayValue = ERROR_TEXT
                previousValue = null
                pendingOperation = null
                return
            }
            expressionHistory = "${formatNumber(prev)} $pending ${formatNumber(current)} ="
            displayValue = formatNumber(result)
            lastOperand = current
            lastOperator = pending
            previousValue = null
            pendingOperation = null
            isNewEntry = true
        } else {
            val op = lastOperator
            val operand = lastOperand
            if (op != null && operand != null) {
                // Повторение последней операции при повторном нажатии «=»
                val result = executeOperation(current, operand, op)
                if (result == null) {
                    displayValue = ERROR_TEXT
                    lastOperand = null
                    lastOperator = null
                    return
                }
                expressionHistory = "${formatNumber(current)} $op ${formatNumber(operand)} ="
                displayValue = formatNumber(result)
                isNewEntry = true
            }
        }
    }

    /**
     * Обрабатывает нажатие клавиши «=»: сначала проверяет разблокировку приложения
     * по введенному пин-коду, а при несовпадении производит математический расчет выражения.
     *
     * @param scope Корутинная область для асинхронного вызова проверки пароля.
     * @param haptic Интерфейс тактильной отдачи.
     * @param onUnlockFailed Обратный вызов при неудачной попытке ввода пин-кода.
     * @param onUnlock Функция проверки пароля и разблокировки.
     */
    fun onEquals(
        scope: CoroutineScope,
        haptic: HapticFeedback,
        onUnlockFailed: () -> Unit = {},
        onUnlock: suspend (String) -> Boolean
    ) {
        if (isVerifying) return

        val codeToTest = displayValue.replace(" ", "").replace(",", ".")
        val candidatePin = getCandidatePin(codeToTest)
        if (candidatePin != null) {
            isVerifying = true
        }

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            if (candidatePin != null) {
                val success = try {
                    attemptUnlock(candidatePin, codeToTest, onUnlock)
                } finally {
                    isVerifying = false
                }
                if (success) return@launch
                onUnlockFailed()
            }
            enteredPinDigits.clear()

            val current = parseDisplayValue() ?: BigDecimal.ZERO
            evaluateCalculation(current)
        }
    }

    /**
     * Очищает дисплей (Clear) или сбрасывает все состояние вычислений (All Clear).
     */
    fun onClear(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        enteredPinDigits.clear()
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

    /**
     * Удаляет последний введенный символ на дисплее (Backspace).
     */
    fun onBackspace(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (enteredPinDigits.isNotEmpty()) {
            enteredPinDigits.deleteCharAt(enteredPinDigits.length - 1)
        }
        if (displayValue == ERROR_TEXT) {
            displayValue = "0"
            isNewEntry = true
            return
        }

        if (!isNewEntry) {
            val isNegative = displayValue.startsWith("-")
            val clean = displayValue.removePrefix("-").replace(" ", "")
            if (clean.length > 1) {
                val dropped = clean.dropLast(1)
                displayValue = if (isNegative) "-$dropped" else dropped
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

    internal fun formatNumber(number: BigDecimal): String {
        val stripped = number.stripTrailingZeros()
        val plain = stripped.toPlainString()

        val isNegative = plain.startsWith("-")
        val unsigned = if (isNegative) plain.removePrefix("-") else plain

        val formatted = if (unsigned.contains(".")) {
            val parts = unsigned.split(".")
            val intPart = parts[0].toLongOrNull()?.let { integerFormatter.format(it) } ?: parts[0]
            "$intPart.${parts[1]}"
        } else {
            unsigned.toLongOrNull()?.let { integerFormatter.format(it) } ?: unsigned
        }

        return if (isNegative) "-$formatted" else formatted
    }

    companion object {
        /** Текст, отображаемый при ошибках вычислений (деление на ноль). */
        const val ERROR_TEXT = "Ошибка"
        private const val MAX_INPUT_DIGITS = 15
        private const val CALC_SCALE = 12
        private val ONE_HUNDRED = BigDecimal("100")

        /** [Saver] для сохранения состояния калькулятора при смене конфигурации Android. */
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
