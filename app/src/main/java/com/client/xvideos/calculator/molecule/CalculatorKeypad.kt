package com.client.xvideos.calculator.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.CalculatorState
import com.client.xvideos.calculator.atom.CalcButton
import com.client.xvideos.calculator.atom.KeypadRow
import com.client.xvideos.calculator.atom.OperatorButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

@Composable
fun CalculatorKeypad(
    state: CalculatorState,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    onUnlock: suspend (String) -> Boolean,
    onUnlockFailed: () -> Unit = {}
) {
    KeypadRow {
        CalcButton(if (state.isAllClear) "AC" else "C", CalculatorColors.FUNCTION_BG, CalculatorColors.FUNCTION_TEXT) {
            state.onClear(haptic)
        }
        CalcButton("⌫", CalculatorColors.FUNCTION_BG, CalculatorColors.FUNCTION_TEXT) { state.onBackspace(haptic) }
        CalcButton("%", CalculatorColors.FUNCTION_BG, CalculatorColors.FUNCTION_TEXT) { state.onPercent(haptic) }
        OperatorButton("÷", state.pendingOperation == "÷") { state.onOperator("÷", haptic) }
    }

    KeypadRow {
        CalcButton("7", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("7", haptic) }
        CalcButton("8", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("8", haptic) }
        CalcButton("9", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("9", haptic) }
        OperatorButton("×", state.pendingOperation == "×") { state.onOperator("×", haptic) }
    }

    KeypadRow {
        CalcButton("4", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("4", haptic) }
        CalcButton("5", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("5", haptic) }
        CalcButton("6", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("6", haptic) }
        OperatorButton("-", state.pendingOperation == "-") { state.onOperator("-", haptic) }
    }

    KeypadRow {
        CalcButton("1", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("1", haptic) }
        CalcButton("2", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("2", haptic) }
        CalcButton("3", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("3", haptic) }
        OperatorButton("+", state.pendingOperation == "+") { state.onOperator("+", haptic) }
    }

    KeypadRow {
        CalcButton("+/-", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onPlusMinus(haptic) }
        CalcButton("0", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDigit("0", haptic) }
        CalcButton(".", CalculatorColors.DIGIT_BG, CalculatorColors.DIGIT_TEXT) { state.onDecimal(haptic) }
        CalcButton("=", CalculatorColors.OPERATOR_BG, CalculatorColors.OPERATOR_TEXT) {
            state.onEquals(
                scope = scope,
                haptic = haptic,
                onUnlockFailed = onUnlockFailed,
                onUnlock = onUnlock
            )
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
