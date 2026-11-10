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
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Состояние и математический движок калькулятора в стиле Xiaomi (MIUI / HyperOS).
 *
 * Использует [BigDecimal] для высокой точности без артефактов вещественных чисел Double.
 * Поддерживает базовые арифметические действия, честный расчёт процентов, смену знака (+/-),
 * повторение операции по «=», динамическое переключение «AC» / «C», секретный ввод пин-кода,
 * инженерный (научный) режим (sin, cos, tan, ln, log, sqrt, pow, factorial, pi, e).
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
    /** Текущий ожидающий выполнения математический оператор ("+", "-", "×", "÷", "^"). */
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

    /** Включён ли научный / инженерный режим калькулятора. */
    var isScientific by mutableStateOf(false)

    /** Режим измерения углов: false = Градусы (DEG), true = Радианы (RAD). */
    var isRad by mutableStateOf(false)

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
     * Переключает отображение инженерной клавиатуры.
     */
    fun toggleScientific() {
        isScientific = !isScientific
    }

    /**
     * Переключает единицу измерения углов между Градусами (DEG) и Радианами (RAD).
     */
    fun toggleAngleUnit() {
        isRad = !isRad
    }

    /**
     * Обрабатывает ввод цифры [digit] с тактильным откликом.
     *
     * @param digit Введенная цифра ("0".."9").
     * @param haptic Интерфейс тактильной отдачи.
     */
    fun onDigit(digit: String, haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (enteredPinDigits.length < MAX_CODE_DIGITS) {
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
     * Устанавливает или вычисляет промежуточный результат при нажатии бинарного оператора [op] (+, -, ×, ÷, ^).
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
                prev.multiply(current).divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
            } else {
                current.divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
            }
        } else {
            current.divide(ONE_HUNDRED, CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
        }

        displayValue = formatNumber(result)
        isNewEntry = true
    }

    /**
     * Обрабатывает унарные научные функции: sin, cos, tan, ln, log, √, x², 1/x, x!.
     */
    fun onScientificUnary(op: String, haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        enteredPinDigits.clear()
        if (displayValue == ERROR_TEXT) return

        val current = parseDisplayValue() ?: return
        val currentD = current.toDouble()

        when (op) {
            "sin", "cos", "tan" -> evalTrig(op, current, currentD)
            "ln", "log" -> evalLog(op, current, currentD)
            "√", "x²", "1/x" -> evalPowerRoot(op, current, currentD)
            "x!" -> evalFactorial(current)
        }
        isNewEntry = true
    }

    private fun evalTrig(op: String, current: BigDecimal, currentD: Double) {
        val rad = if (isRad) currentD else Math.toRadians(currentD)
        val unit = if (isRad) "rad" else "°"
        when (op) {
            "sin" -> {
                val res = sin(rad)
                val normalized = if (kotlin.math.abs(res) < 1e-15) 0.0 else res
                displayValue = formatDouble(normalized)
                expressionHistory = "sin(${formatNumber(current)}$unit)"
            }
            "cos" -> {
                val res = cos(rad)
                val normalized = if (kotlin.math.abs(res) < 1e-15) 0.0 else res
                displayValue = formatDouble(normalized)
                expressionHistory = "cos(${formatNumber(current)}$unit)"
            }
            "tan" -> {
                val cosVal = cos(rad)
                if (kotlin.math.abs(cosVal) < 1e-15) {
                    displayValue = ERROR_TEXT
                } else {
                    val res = tan(rad)
                    val normalized = if (kotlin.math.abs(res) < 1e-15) 0.0 else res
                    displayValue = formatDouble(normalized)
                    expressionHistory = "tan(${formatNumber(current)}$unit)"
                }
            }
        }
    }

    private fun evalLog(op: String, current: BigDecimal, currentD: Double) {
        if (currentD <= 0.0) {
            displayValue = ERROR_TEXT
            return
        }
        if (op == "ln") {
            displayValue = formatDouble(log(currentD, kotlin.math.E))
            expressionHistory = "ln(${formatNumber(current)})"
        } else {
            displayValue = formatDouble(log10(currentD))
            expressionHistory = "log(${formatNumber(current)})"
        }
    }

    private fun evalPowerRoot(op: String, current: BigDecimal, currentD: Double) {
        when (op) {
            "√" -> {
                if (currentD < 0.0) {
                    displayValue = ERROR_TEXT
                } else {
                    displayValue = formatDouble(sqrt(currentD))
                    expressionHistory = "√(${formatNumber(current)})"
                }
            }
            "x²" -> {
                val res = current.multiply(current)
                displayValue = formatNumber(res)
                expressionHistory = "(${formatNumber(current)})²"
            }
            "1/x" -> {
                if (current.compareTo(BigDecimal.ZERO) == 0) {
                    displayValue = ERROR_TEXT
                } else {
                    val res = BigDecimal.ONE.divide(current, CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
                    displayValue = formatNumber(res)
                    expressionHistory = "1/(${formatNumber(current)})"
                }
            }
        }
    }

    private fun evalFactorial(current: BigDecimal) {
        val isInt = current.stripTrailingZeros().scale() <= 0
        val longVal = current.toLong()
        if (isInt && longVal in 0..100) {
            var fact = BigInteger.ONE
            for (i in 2..longVal) {
                fact = fact.multiply(BigInteger.valueOf(i))
            }
            displayValue = if (fact.toString().length > 14) {
                String.format(Locale.US, "%.6e", fact.toDouble())
            } else {
                fact.toString()
            }
            expressionHistory = "${formatNumber(current)}!"
        } else {
            displayValue = ERROR_TEXT
        }
    }

    /**
     * Вставляет математическую константу (π, e).
     */
    fun onConstant(constant: String, haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        enteredPinDigits.clear()
        when (constant) {
            "π" -> displayValue = formatDouble(PI)
            "e" -> displayValue = formatDouble(kotlin.math.E)
        }
        isNewEntry = true
    }

    private fun isPinCandidate(s: String): Boolean = isEnterableCode(s)

    /**
     * Код, который «=» отправит на проверку, или `null`, если проверять нечего.
     */
    private fun getCandidatePin(codeToTest: String): String? {
        if (previousValue != null || pendingOperation != null) return null
        val rawPin = enteredPinDigits.toString()
        return when {
            isPinCandidate(rawPin) -> rawPin
            !isNewEntry && isPinCandidate(codeToTest) -> codeToTest
            else -> null
        }
    }

    private suspend fun attemptUnlock(
        candidatePin: String,
        codeToTest: String,
        onUnlock: suspend (String) -> Boolean,
        onUnlockFailed: () -> Unit
    ): Boolean {
        val unlocked = onUnlock(candidatePin)
        if (!unlocked && candidatePin != codeToTest && isPinCandidate(codeToTest)) {
            onUnlockFailed()
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
                    attemptUnlock(candidatePin, codeToTest, onUnlock, onUnlockFailed)
                } finally {
                    isVerifying = false
                }
                if (success) {
                    resetState()
                    return@launch
                }
                onUnlockFailed()
            }
            enteredPinDigits.clear()

            val current = parseDisplayValue() ?: BigDecimal.ZERO
            evaluateCalculation(current)
        }
    }

    /**
     * Полностью сбрасывает все состояние вычислений, дисплей и накопленные цифры кода доступа.
     */
    fun resetState() {
        enteredPinDigits.clear()
        displayValue = "0"
        previousValue = null
        pendingOperation = null
        lastOperand = null
        lastOperator = null
        expressionHistory = ""
        isNewEntry = true
    }

    /**
     * Очищает дисплей (Clear) или сбрасывает все состояние вычислений (All Clear).
     */
    fun onClear(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!isAllClear && displayValue != "0") {
            enteredPinDigits.clear()
            displayValue = "0"
            isNewEntry = true
        } else {
            resetState()
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
        "^" -> {
            val powResult = a.toDouble().pow(b.toDouble())
            if (powResult.isNaN() || powResult.isInfinite()) {
                null
            } else {
                BigDecimal.valueOf(powResult).setScale(CALC_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()
            }
        }
        else -> b
    }

    private fun formatDouble(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return ERROR_TEXT
        val bd = BigDecimal.valueOf(d).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        return formatNumber(bd)
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

        /** Самый короткий код доступа. */
        const val MIN_CODE_DIGITS = 4

        /**
         * Самый длинный код, который можно набрать на калькуляторе.
         */
        const val MAX_CODE_DIGITS = 64

        /**
         * Можно ли набрать [code] на калькуляторе: только цифры 0–9 и длина в пределах буфера.
         */
        fun isEnterableCode(code: String): Boolean =
            code.length in MIN_CODE_DIGITS..MAX_CODE_DIGITS && code.all { it in '0'..'9' }

        private const val CALC_SCALE = 12
        private val ONE_HUNDRED = BigDecimal("100")

        /** [Saver] для сохранения состояния калькулятора при смене конфигурации Android. */
        val Saver: Saver<CalculatorState, List<String?>> = Saver(
            save = {
                val rawDigits = it.displayValue.replace(" ", "")
                val isSecretCandidate = it.previousValue == null &&
                    it.pendingOperation == null &&
                    (it.isPinCandidate(rawDigits) || it.enteredPinDigits.isNotEmpty())
                val safeDisplay = if (isSecretCandidate) "0" else it.displayValue

                listOf(
                    safeDisplay,
                    it.expressionHistory,
                    it.previousValue?.toPlainString(),
                    it.pendingOperation,
                    it.lastOperand?.toPlainString(),
                    it.lastOperator,
                    it.isNewEntry.toString(),
                    it.isScientific.toString(),
                    it.isRad.toString(),
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
                    isScientific = it.getOrNull(7)?.toBooleanStrictOrNull() ?: false
                    isRad = it.getOrNull(8)?.toBooleanStrictOrNull() ?: false
                }
            }
        )
    }
}
