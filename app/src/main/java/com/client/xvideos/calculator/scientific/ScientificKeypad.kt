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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.CalculatorState
import com.client.xvideos.calculator.atom.CalcButton

private data class ScientificKeySpec(
    val label: String,
    val backgroundColor: Color = CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
    val textColor: Color = CalculatorColors.XIAOMI_KEY_DIGIT_TEXT,
    val fontSize: TextUnit = 15.sp,
    val fontWeight: FontWeight = FontWeight.Normal,
    val onClick: () -> Unit,
)

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
    val keyShape = RoundedCornerShape(14.dp)
    val rows = listOf(
        listOf(
            ScientificKeySpec(
                label = if (state.isRad) "RAD" else "DEG",
                backgroundColor = if (state.isRad) CalculatorColors.XIAOMI_ORANGE else CalculatorColors.XIAOMI_KEY_SCIENTIFIC,
                textColor = if (state.isRad) Color.White else CalculatorColors.XIAOMI_ORANGE,
                fontWeight = FontWeight.Bold,
                onClick = { state.toggleAngleUnit() },
            ),
            ScientificKeySpec("sin") { state.onScientificUnary("sin", haptic) },
            ScientificKeySpec("cos") { state.onScientificUnary("cos", haptic) },
            ScientificKeySpec("tan") { state.onScientificUnary("tan", haptic) },
            ScientificKeySpec("π", textColor = CalculatorColors.XIAOMI_ORANGE, fontSize = 17.sp) {
                state.onConstant("π", haptic)
            },
        ),
        listOf(
            ScientificKeySpec("ln") { state.onScientificUnary("ln", haptic) },
            ScientificKeySpec("log") { state.onScientificUnary("log", haptic) },
            ScientificKeySpec("1/x") { state.onScientificUnary("1/x", haptic) },
            ScientificKeySpec("e", textColor = CalculatorColors.XIAOMI_ORANGE, fontSize = 17.sp) {
                state.onConstant("e", haptic)
            },
            ScientificKeySpec("x!") { state.onScientificUnary("x!", haptic) },
        ),
        listOf(
            ScientificKeySpec("√", fontSize = 17.sp) { state.onScientificUnary("√", haptic) },
            ScientificKeySpec("x²") { state.onScientificUnary("x²", haptic) },
            ScientificKeySpec(
                label = "^",
                backgroundColor = if (state.pendingOperation == "^") {
                    CalculatorColors.XIAOMI_ORANGE
                } else {
                    CalculatorColors.XIAOMI_KEY_SCIENTIFIC
                },
                textColor = if (state.pendingOperation == "^") Color.White else CalculatorColors.XIAOMI_ORANGE,
                fontSize = 17.sp,
                onClick = { state.onOperator("^", haptic) },
            ),
            ScientificKeySpec("(", textColor = CalculatorColors.XIAOMI_TEXT_SECONDARY, fontSize = 17.sp) {},
            ScientificKeySpec(")", textColor = CalculatorColors.XIAOMI_TEXT_SECONDARY, fontSize = 17.sp) {},
        ),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        rows.forEach { rowSpecs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowSpecs.forEach { spec ->
                    CalcButton(
                        text = spec.label,
                        backgroundColor = spec.backgroundColor,
                        textColor = spec.textColor,
                        shape = keyShape,
                        fontSize = spec.fontSize,
                        fontWeight = spec.fontWeight,
                        onClick = spec.onClick,
                    )
                }
            }
        }
    }
}
