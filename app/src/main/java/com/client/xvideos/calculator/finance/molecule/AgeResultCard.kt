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
import com.client.xvideos.calculator.finance.model.AgeResult

@Composable
internal fun AgeResultCard(
    result: AgeResult,
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
            text = "Точный возраст",
            color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
            fontSize = 13.sp
        )
        Text(
            text = result.ageText,
            color = CalculatorColors.XIAOMI_ORANGE,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "До следующего ДР", color = CalculatorColors.XIAOMI_TEXT_SECONDARY, fontSize = 12.sp)
                Text(text = result.nextBirthdayDays, color = CalculatorColors.XIAOMI_TEXT_PRIMARY, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Column {
                Text(text = "День недели", color = CalculatorColors.XIAOMI_TEXT_SECONDARY, fontSize = 12.sp)
                Text(text = result.dayOfWeek, color = CalculatorColors.XIAOMI_TEXT_PRIMARY, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Прожито", color = CalculatorColors.XIAOMI_TEXT_SECONDARY, fontSize = 12.sp)
                Text(text = result.totalDays, color = CalculatorColors.XIAOMI_TEXT_PRIMARY, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview
@Composable
private fun AgeResultCardPreview() {
    AgeResultCard(
        result = AgeResult(
            ageText = "28 лет, 5 мес., 11 дн.",
            nextBirthdayDays = "200 дн.",
            dayOfWeek = "Понедельник",
            totalDays = "10390 дней",
        )
    )
}
