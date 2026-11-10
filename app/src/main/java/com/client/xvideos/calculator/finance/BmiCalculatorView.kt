package com.client.xvideos.calculator.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import java.util.Locale

private data class BmiResult(
    val bmi: Double,
    val category: String,
    val color: Color,
    val idealRange: String
)

@Composable
fun BmiCalculatorView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var heightStr by remember { mutableStateOf("175") }
    var weightStr by remember { mutableStateOf("70") }

    val heightCm = heightStr.toDoubleOrNull() ?: 0.0
    val weightKg = weightStr.toDoubleOrNull() ?: 0.0

    val result = remember(heightCm, weightKg) {
        val hM = heightCm / 100.0
        if (hM <= 0.5 || weightKg <= 5.0) {
            BmiResult(0.0, "—", Color.Gray, "—")
        } else {
            val bmi = weightKg / (hM * hM)
            val (cat, col) = when {
                bmi < 18.5 -> "Дефицит массы" to Color(0xFF0A84FF)
                bmi < 25.0 -> "Нормальный вес" to Color(0xFF30D158)
                bmi < 30.0 -> "Избыточный вес" to Color(0xFFFF9F0A)
                else -> "Ожирение" to Color(0xFFFF453A)
            }
            val minIdeal = 18.5 * hM * hM
            val maxIdeal = 24.9 * hM * hM
            val rangeText = String.format(Locale.US, "%.1f – %.1f кг", minIdeal, maxIdeal)
            BmiResult(bmi, cat, col, rangeText)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalculatorColors.XIAOMI_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← Индекс массы тела (ИМТ)",
                color = CalculatorColors.XIAOMI_ORANGE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(4.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        FinanceInputField(label = "Рост (см)", value = heightStr, onValueChange = { heightStr = it })
        Spacer(Modifier.height(10.dp))
        FinanceInputField(label = "Вес (кг)", value = weightStr, onValueChange = { weightStr = it })

        Spacer(Modifier.height(20.dp))

        BmiResultCard(result = result, heightCm = heightCm)
    }
}

@Composable
private fun BmiResultCard(
    result: BmiResult,
    heightCm: Double
) {
    Column(
        modifier = Modifier
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
