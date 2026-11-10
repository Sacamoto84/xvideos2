package com.client.xvideos.calculator.converter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.atom.CalcButton
import com.client.xvideos.calculator.atom.KeypadRow

/**
 * Цифровая клавиатура конвертера Xiaomi MIUI / HyperOS Light Theme.
 * Полностью повторяет раскладку экрана «Данные» и других конвертеров:
 * C, ⌫, %, ÷
 * 7, 8, 9, ×
 * 4, 5, 6, -
 * 1, 2, 3, +
 * 00, 0, ., =
 */
@Composable
fun ConverterKeypad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onOperator: (String) -> Unit = {},
    onPercent: () -> Unit = {},
    onEquals: () -> Unit = {},
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        KeypadRow(spacing = spacing) {
            CalcButton("C", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_ORANGE) { onClear() }
            CalcButton("⌫", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_ORANGE) { onBackspace() }
            CalcButton("%", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_ORANGE) { onPercent() }
            CalcButton("÷", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_ORANGE) { onOperator("÷") }
        }

        KeypadRow(spacing = spacing) {
            CalcButton("7", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("7") }
            CalcButton("8", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("8") }
            CalcButton("9", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("9") }
            CalcButton("×", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_ORANGE) { onOperator("×") }
        }

        KeypadRow(spacing = spacing) {
            CalcButton("4", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("4") }
            CalcButton("5", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("5") }
            CalcButton("6", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("6") }
            CalcButton("-", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_ORANGE) { onOperator("-") }
        }

        KeypadRow(spacing = spacing) {
            CalcButton("1", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("1") }
            CalcButton("2", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("2") }
            CalcButton("3", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("3") }
            CalcButton("+", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_ORANGE) { onOperator("+") }
        }

        KeypadRow(spacing = spacing) {
            CalcButton("00", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT, fontSize = 24.sp) { onDigit("00") }
            CalcButton("0", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDigit("0") }
            CalcButton(".", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { onDecimal() }
            CalcButton(
                text = "=",
                backgroundColor = CalculatorColors.XIAOMI_ORANGE,
                textColor = Color.White
            ) {
                onEquals()
            }
        }
    }
}
