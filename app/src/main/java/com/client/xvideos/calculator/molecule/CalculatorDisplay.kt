package com.client.xvideos.calculator.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors

@Composable
fun CalculatorDisplay(
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
                color = Color(0xFF8E8E93),
                fontSize = 22.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CalculatorColors.DISPLAY_HORIZONTAL_PADDING)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CalculatorColors.DISPLAY_HORIZONTAL_PADDING)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun CalculatorDisplayPreview() {
    CalculatorDisplay(
        displayValue = "123",
        expressionHistory = "100+23"
    )
}
