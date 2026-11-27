package com.client.xvideos.calculator.finance.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.finance.model.BmiResult
import java.util.Locale

@Composable
internal fun BmiResultCard(
    result: BmiResult,
    heightCm: Double,
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
            text = "Ваш ИМТ",
            color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
            fontSize = 13.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (result.bmi > 0) String.format(Locale.US, "%.1f", result.bmi) else "—",
                color = CalculatorColors.XIAOMI_TEXT_PRIMARY,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(result.color.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = result.category,
                    color = result.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(modifier = Modifier.weight(18.5f).background(Color(0xFF0A84FF)))
            Box(modifier = Modifier.weight(6.5f).background(Color(0xFF30D158)))
            Box(modifier = Modifier.weight(5f).background(Color(0xFFFF9F0A)))
            Box(modifier = Modifier.weight(10f).background(Color(0xFFFF453A)))
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Идеальный вес для роста $heightCm см:",
                color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
                fontSize = 13.sp
            )
            Text(
                text = result.idealRange,
                color = CalculatorColors.XIAOMI_ORANGE,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview
@Composable
private fun BmiResultCardPreview() {
    BmiResultCard(
        result = BmiResult(22.9, "Нормальный вес", Color(0xFF30D158), "56.7 – 76.3 кг"),
        heightCm = 175.0,
    )
}
