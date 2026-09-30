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
    val bg = if (isSelected) Color.White else CalculatorColors.OPERATOR_BG
    val text = if (isSelected) CalculatorColors.OPERATOR_BG else CalculatorColors.OPERATOR_TEXT
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
