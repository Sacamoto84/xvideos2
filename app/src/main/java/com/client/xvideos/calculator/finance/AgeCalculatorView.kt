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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import java.time.LocalDate
import java.time.Period
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private data class AgeResult(
    val ageText: String,
    val nextBirthdayDays: String,
    val dayOfWeek: String,
    val totalDays: String
)

@Composable
fun AgeCalculatorView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dayStr by remember { mutableStateOf("15") }
    var monthStr by remember { mutableStateOf("6") }
    var yearStr by remember { mutableStateOf("1998") }

    val day = dayStr.toIntOrNull() ?: 1
    val month = monthStr.toIntOrNull() ?: 1
    val year = yearStr.toIntOrNull() ?: 2000

    val result = remember(day, month, year) {
        runCatching {
            val birthDate = LocalDate.of(year, month, day)
            val today = LocalDate.now()
            if (birthDate.isAfter(today)) {
                return@runCatching AgeResult("0 лет", "—", "—", "0 дней")
            }
            val period = Period.between(birthDate, today)
            val ageStr = "${period.years} лет, ${period.months} мес., ${period.days} дн."

            val nextBirthday = birthDate.withYear(today.year).let {
                if (it.isBefore(today)) it.plusYears(1) else it
            }
            val daysUntil = ChronoUnit.DAYS.between(today, nextBirthday)
            val ruLocale = Locale.forLanguageTag("ru")
            val dow = birthDate.dayOfWeek.getDisplayName(TextStyle.FULL, ruLocale)
                .replaceFirstChar { it.titlecase(ruLocale) }
            val daysLived = "${ChronoUnit.DAYS.between(birthDate, today)} дней"

            AgeResult(ageStr, "$daysUntil дн.", dow, daysLived)
        }.getOrElse {
            AgeResult("Некорректная дата", "—", "—", "—")
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
                text = "← Возраст и Даты",
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                FinanceInputField(label = "День", value = dayStr, onValueChange = { dayStr = it })
            }
            Box(modifier = Modifier.weight(1f)) {
                FinanceInputField(label = "Месяц", value = monthStr, onValueChange = { monthStr = it })
            }
            Box(modifier = Modifier.weight(1.5f)) {
                FinanceInputField(label = "Год", value = yearStr, onValueChange = { yearStr = it })
            }
        }

        Spacer(Modifier.height(20.dp))

        AgeResultCard(result = result)
    }
}

@Composable
private fun AgeResultCard(result: AgeResult) {
    Column(
        modifier = Modifier
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
