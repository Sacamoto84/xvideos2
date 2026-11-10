package com.client.xvideos.calculator.atom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.calculator.CalculatorColors

@Composable
fun RowScope.OperatorButton(
    symbol: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) CalculatorColors.XIAOMI_ORANGE else CalculatorColors.XIAOMI_KEY_BG
    val text = if (isSelected) Color.White else CalculatorColors.XIAOMI_ORANGE
    CalcButton(symbol, bg, text, onClick = onClick)
}

@Preview
@Composable
private fun OperatorButtonPreview() {
    Row {
        OperatorButton(
            symbol = "÷",
            isSelected = false,
            onClick = {}
        )
    }
}
