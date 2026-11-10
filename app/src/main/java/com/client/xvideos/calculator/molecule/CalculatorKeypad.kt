package com.client.xvideos.calculator.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.CalculatorState
import com.client.xvideos.calculator.atom.CalcButton
import com.client.xvideos.calculator.atom.KeypadRow
import com.client.xvideos.calculator.atom.OperatorButton
import com.client.xvideos.calculator.scientific.ScientificKeypad
import kotlinx.coroutines.CoroutineScope

/**
 * Основная клавиатура калькулятора Xiaomi (MIUI / HyperOS Light Theme).
 */
@Composable
fun CalculatorKeypad(
    state: CalculatorState,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    onUnlock: suspend (String) -> Boolean,
    onUnlockFailed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (state.isScientific) {
            ScientificKeypad(
                state = state,
                haptic = haptic,
                spacing = 10.dp
            )
        }

        KeypadRow(spacing = 10.dp) {
            CalcButton(
                text = if (state.isAllClear) "AC" else "C",
                backgroundColor = CalculatorColors.XIAOMI_KEY_BG,
                textColor = CalculatorColors.XIAOMI_ORANGE
            ) {
                state.onClear(haptic)
            }
            CalcButton(
                text = "⌫",
                backgroundColor = CalculatorColors.XIAOMI_KEY_BG,
                textColor = CalculatorColors.XIAOMI_ORANGE
            ) {
                state.onBackspace(haptic)
            }
            CalcButton(
                text = "%",
                backgroundColor = CalculatorColors.XIAOMI_KEY_BG,
                textColor = CalculatorColors.XIAOMI_ORANGE
            ) {
                state.onPercent(haptic)
            }
            OperatorButton("÷", state.pendingOperation == "÷") {
                state.onOperator("÷", haptic)
            }
        }

        KeypadRow(spacing = 10.dp) {
            CalcButton("7", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("7", haptic) }
            CalcButton("8", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("8", haptic) }
            CalcButton("9", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("9", haptic) }
            OperatorButton("×", state.pendingOperation == "×") { state.onOperator("×", haptic) }
        }

        KeypadRow(spacing = 10.dp) {
            CalcButton("4", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("4", haptic) }
            CalcButton("5", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("5", haptic) }
            CalcButton("6", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("6", haptic) }
            OperatorButton("-", state.pendingOperation == "-") { state.onOperator("-", haptic) }
        }

        KeypadRow(spacing = 10.dp) {
            CalcButton("1", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("1", haptic) }
            CalcButton("2", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("2", haptic) }
            CalcButton("3", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("3", haptic) }
            OperatorButton("+", state.pendingOperation == "+") { state.onOperator("+", haptic) }
        }

        KeypadRow(spacing = 10.dp) {
            val sciBg = if (state.isScientific) CalculatorColors.XIAOMI_ORANGE else CalculatorColors.XIAOMI_KEY_BG
            val sciText = if (state.isScientific) Color.White else CalculatorColors.XIAOMI_ORANGE
            CalcButton(
                text = "⇄",
                backgroundColor = sciBg,
                textColor = sciText,
                fontSize = 24.sp
            ) {
                state.toggleScientific()
            }
            CalcButton("0", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDigit("0", haptic) }
            CalcButton(",", CalculatorColors.XIAOMI_KEY_BG, CalculatorColors.XIAOMI_KEY_DIGIT_TEXT) { state.onDecimal(haptic) }
            CalcButton(
                text = "=",
                backgroundColor = CalculatorColors.XIAOMI_ORANGE,
                textColor = Color.White
            ) {
                state.onEquals(
                    scope = scope,
                    haptic = haptic,
                    onUnlockFailed = onUnlockFailed,
                    onUnlock = onUnlock
                )
            }
        }
    }
}

@Preview
@Composable
private fun CalculatorKeypadPreview() {
    CalculatorKeypad(
        state = CalculatorState(),
        scope = rememberCoroutineScope(),
        haptic = LocalHapticFeedback.current,
        onUnlock = { true }
    )
}
