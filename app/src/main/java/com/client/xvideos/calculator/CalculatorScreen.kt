package com.client.xvideos.calculator

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope

private val FUNCTION_BG = Color(0xFFA5A5A5)
private val FUNCTION_TEXT = Color.Black
private val DIGIT_BG = Color(0xFF333333)
private val DIGIT_TEXT = Color.White
private val OPERATOR_BG = Color(0xFFFF9F0A)
private val OPERATOR_TEXT = Color.White
private val HISTORY_TEXT_COLOR = Color(0xFF8E8E93)

/**
 * Экран-камуфляж «Калькулятор».
 *
 * Выполнен в виде нативного системного калькулятора с равномерной сеткой кнопок 4×5.
 * При вводе верного PIN-кода и нажатии «=» разблокирует приложение через [onUnlock].
 */
@Composable
fun CalculatorScreen(
    onUnlock: suspend (String) -> Boolean,
    onUnlockFailed: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val state = rememberSaveable(saver = CalculatorState.Saver) { CalculatorState() }

    val canBackspace = state.displayValue == CalculatorState.ERROR_TEXT || (!state.isNewEntry && state.displayValue != "0")
    val canClear = !canBackspace && !state.isAllClear

    val onBackspaceAction = remember(state, haptic) { { state.onBackspace(haptic) } }
    val onClearAction = remember(state, haptic) { { state.onClear(haptic) } }
    val onBackAction = remember(onBack) { { onBack() } }

    BackHandler(enabled = canBackspace, onBack = onBackspaceAction)
    BackHandler(enabled = canClear, onBack = onClearAction)
    BackHandler(enabled = !canBackspace && !canClear, onBack = onBackAction)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .displayCutoutPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        CalculatorDisplay(
            displayValue = state.displayValue,
            expressionHistory = state.expressionHistory,
            modifier = Modifier.weight(1f)
        )
        CalculatorKeypad(
            state = state,
            scope = scope,
            haptic = haptic,
            onUnlock = onUnlock,
            onUnlockFailed = onUnlockFailed
        )
    }
}

@Composable
private fun CalculatorDisplay(
    displayValue: String,
    expressionHistory: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        if (expressionHistory.isNotEmpty()) {
            Text(
                text = expressionHistory,
                color = HISTORY_TEXT_COLOR,
                fontSize = 22.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))

        val fontSize = when {
            displayValue.length > 13 -> 34.sp
            displayValue.length > 10 -> 42.sp
            displayValue.length > 7 -> 52.sp
            else -> 64.sp
        }

        Text(
            text = displayValue,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CalculatorKeypad(
    state: CalculatorState,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    onUnlock: suspend (String) -> Boolean,
    onUnlockFailed: () -> Unit = {}
) {
    val spacing = 12.dp

    KeypadRow(spacing) {
        CalcButton(if (state.isAllClear) "AC" else "C", FUNCTION_BG, FUNCTION_TEXT) { state.onClear(haptic) }
        CalcButton("⌫", FUNCTION_BG, FUNCTION_TEXT) { state.onBackspace(haptic) }
        CalcButton("%", FUNCTION_BG, FUNCTION_TEXT) { state.onPercent(haptic) }
        OperatorButton("÷", state.pendingOperation == "÷") { state.onOperator("÷", haptic) }
    }

    KeypadRow(spacing) {
        CalcButton("7", DIGIT_BG, DIGIT_TEXT) { state.onDigit("7", haptic) }
        CalcButton("8", DIGIT_BG, DIGIT_TEXT) { state.onDigit("8", haptic) }
        CalcButton("9", DIGIT_BG, DIGIT_TEXT) { state.onDigit("9", haptic) }
        OperatorButton("×", state.pendingOperation == "×") { state.onOperator("×", haptic) }
    }

    KeypadRow(spacing) {
        CalcButton("4", DIGIT_BG, DIGIT_TEXT) { state.onDigit("4", haptic) }
        CalcButton("5", DIGIT_BG, DIGIT_TEXT) { state.onDigit("5", haptic) }
        CalcButton("6", DIGIT_BG, DIGIT_TEXT) { state.onDigit("6", haptic) }
        OperatorButton("-", state.pendingOperation == "-") { state.onOperator("-", haptic) }
    }

    KeypadRow(spacing) {
        CalcButton("1", DIGIT_BG, DIGIT_TEXT) { state.onDigit("1", haptic) }
        CalcButton("2", DIGIT_BG, DIGIT_TEXT) { state.onDigit("2", haptic) }
        CalcButton("3", DIGIT_BG, DIGIT_TEXT) { state.onDigit("3", haptic) }
        OperatorButton("+", state.pendingOperation == "+") { state.onOperator("+", haptic) }
    }

    KeypadRow(spacing) {
        CalcButton("+/-", DIGIT_BG, DIGIT_TEXT) { state.onPlusMinus(haptic) }
        CalcButton("0", DIGIT_BG, DIGIT_TEXT) { state.onDigit("0", haptic) }
        CalcButton(".", DIGIT_BG, DIGIT_TEXT) { state.onDecimal(haptic) }
        CalcButton("=", OPERATOR_BG, OPERATOR_TEXT) {
            state.onEquals(
                scope = scope,
                haptic = haptic,
                onUnlockFailed = onUnlockFailed,
                onUnlock = onUnlock
            )
        }
    }
}

@Composable
private fun KeypadRow(
    spacing: Dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
private fun RowScope.OperatorButton(
    symbol: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color.White else OPERATOR_BG
    val text = if (isSelected) OPERATOR_BG else OPERATOR_TEXT
    CalcButton(symbol, bg, text, onClick = onClick)
}

@Composable
private fun RowScope.CalcButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (text.length > 2) 22.sp else 28.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
