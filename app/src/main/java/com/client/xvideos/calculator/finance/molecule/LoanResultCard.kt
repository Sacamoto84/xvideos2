package com.client.xvideos.calculator.finance.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors

@Composable
internal fun LoanResultCard(
    monthlyPayment: String,
    overpay: String,
    total: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CalculatorColors.XIAOMI_SURFACE)
            .padding(18.dp)
    ) {
        Text(
            text = "Ежемесячный платёж",
            color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
            fontSize = 13.sp
        )
        Text(
            text = monthlyPayment,
            color = CalculatorColors.XIAOMI_ORANGE,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Начисленные %", color = CalculatorColors.XIAOMI_TEXT_SECONDARY, fontSize = 12.sp)
                Text(text = overpay, color = CalculatorColors.XIAOMI_TEXT_PRIMARY, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Общая выплата", color = CalculatorColors.XIAOMI_TEXT_SECONDARY, fontSize = 12.sp)
                Text(text = total, color = CalculatorColors.XIAOMI_TEXT_PRIMARY, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview
@Composable
private fun LoanResultCardPreview() {
    LoanResultCard(monthlyPayment = "35 404 ₽", overpay = "274 544 ₽", total = "1 274 544 ₽")
}
