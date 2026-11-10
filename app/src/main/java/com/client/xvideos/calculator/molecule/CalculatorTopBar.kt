package com.client.xvideos.calculator.molecule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors

/**
 * Верхняя навигационная панель калькулятора Xiaomi MIUI / HyperOS.
 * Отображает переключение между вкладками «Калькулятор» и «Конвертер».
 */
@Composable
fun CalculatorTopBar(
    selectedTab: CalculatorTab,
    onTabSelected: (CalculatorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalculatorTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Text(
                    text = tab.title,
                    color = if (isSelected) CalculatorColors.XIAOMI_TEXT_PRIMARY else CalculatorColors.XIAOMI_TEXT_SECONDARY,
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}
