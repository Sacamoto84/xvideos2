package com.client.xvideos.calculator.scientific

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.CalculatorState
import com.client.xvideos.calculator.atom.CalcButton

/**
 * Инженерная панель калькулятора Xiaomi в светлой теме.
 */
@Composable
fun ScientificKeypad(
    state: CalculatorState,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        ScientificRow1(state, haptic, spacing)
        ScientificRow2(state, haptic, spacing)
        ScientificRow3(state, haptic, spacing)
    }
}

@Composable
private fun ScientificRow1(state: CalculatorState, haptic: HapticFeedback, spacing: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        val angleLabel = if (state.isRad) "RAD" else "DEG"
        CalcButton(
            text = angleLabel,
            backgroundColor = if (state.isRad) CalculatorColors.XIAOMI_ORANGE else CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = if (state.isRad) Color.White else CalculatorColors.XIAOMI_ORANGE,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            onClick = { state.toggleAngleUnit() }
        )
        CalcButton(
            text = "sin",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            onClick = { state.onScientificUnary("sin", haptic) }
        )
        CalcButton(
            text = "cos",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            onClick = { state.onScientificUnary("cos", haptic) }
        )
        CalcButton(
            text = "tan",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            onClick = { state.onScientificUnary("tan", haptic) }
        )
        CalcButton(
            text = "π",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_ORANGE,
            shape = RoundedCornerShape(14.dp),
            fontSize = 17.sp,
            onClick = { state.onConstant("π", haptic) }
        )
    }
}

@Composable
private fun ScientificRow2(state: CalculatorState, haptic: HapticFeedback, spacing: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        CalcButton(
            text = "ln",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            onClick = { state.onScientificUnary("ln", haptic) }
        )
        CalcButton(
            text = "log",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            onClick = { state.onScientificUnary("log", haptic) }
        )
        CalcButton(
            text = "1/x",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            onClick = { state.onScientificUnary("1/x", haptic) }
        )
        CalcButton(
            text = "e",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_ORANGE,
            shape = RoundedCornerShape(14.dp),
            fontSize = 17.sp,
            onClick = { state.onConstant("e", haptic) }
        )
        CalcButton(
            text = "x!",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            onClick = { state.onScientificUnary("x!", haptic) }
        )
    }
}

@Composable
private fun ScientificRow3(state: CalculatorState, haptic: HapticFeedback, spacing: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        CalcButton(
            text = "√",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 17.sp,
            onClick = { state.onScientificUnary("√", haptic) }
        )
        CalcButton(
            text = "x²",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
            shape = RoundedCornerShape(14.dp),
            fontSize = 15.sp,
            onClick = { state.onScientificUnary("x²", haptic) }
        )
        CalcButton(
            text = "^",
            backgroundColor = if (state.pendingOperation == "^") CalculatorColors.XIAOMI_ORANGE else CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = if (state.pendingOperation == "^") Color.White else CalculatorColors.XIAOMI_ORANGE,
            shape = RoundedCornerShape(14.dp),
            fontSize = 17.sp,
            onClick = { state.onOperator("^", haptic) }
        )
        CalcButton(
            text = "(",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_TEXT_SECONDARY,
            shape = RoundedCornerShape(14.dp),
            fontSize = 17.sp,
            onClick = {}
        )
        CalcButton(
            text = ")",
            backgroundColor = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
            textColor = CalculatorColors.XIAOMI_TEXT_SECONDARY,
            shape = RoundedCornerShape(14.dp),
            fontSize = 17.sp,
            onClick = {}
        )
    }
}
