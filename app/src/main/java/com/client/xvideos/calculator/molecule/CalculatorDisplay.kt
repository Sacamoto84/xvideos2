package com.client.xvideos.calculator.molecule

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors

/**
 * Дисплей калькулятора в светлом стиле Xiaomi MIUI / HyperOS.
 *
 * Отображает строку текущего выражения, индикатор угла (DEG/RAD),
 * активное число с мигающим синим курсором и динамическим размером шрифта.
 */
@Composable
fun CalculatorDisplay(
    displayValue: String,
    expressionHistory: String,
    modifier: Modifier = Modifier,
    isScientific: Boolean = false,
    isRad: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        if (isScientific) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CalculatorColors.DISPLAY_HORIZONTAL_PADDING),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = CalculatorColors.XIAOMI_ORANGE.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isRad) "RAD" else "DEG",
                        color = CalculatorColors.XIAOMI_ORANGE,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (expressionHistory.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CalculatorColors.DISPLAY_HORIZONTAL_PADDING)
                    .horizontalScroll(rememberScrollState(), reverseScrolling = true),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = expressionHistory,
                    color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
                    fontSize = 20.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        val fontSize = when {
            displayValue.length > 13 -> 34.sp
            displayValue.length > 10 -> 42.sp
            displayValue.length > 7 -> 52.sp
            else -> 64.sp
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CalculatorColors.DISPLAY_HORIZONTAL_PADDING)
                .horizontalScroll(rememberScrollState(), reverseScrolling = true),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayValue,
                color = CalculatorColors.XIAOMI_TEXT_PRIMARY,
                fontSize = fontSize,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Spacer(Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(fontSize.value.dp * 0.75f)
                    .background(CalculatorColors.XIAOMI_CURSOR.copy(alpha = cursorAlpha))
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun CalculatorDisplayPreview() {
    CalculatorDisplay(
        displayValue = "0",
        expressionHistory = "150 × 1 450",
        isScientific = true,
        isRad = false
    )
}
