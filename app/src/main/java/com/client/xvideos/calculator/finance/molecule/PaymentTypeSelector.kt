package com.client.xvideos.calculator.finance.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
internal fun PaymentTypeSelector(
    isAnnuity: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CalculatorColors.XIAOMI_SURFACE)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isAnnuity) CalculatorColors.XIAOMI_ORANGE else Color.Transparent)
                .clickable { onSelect(true) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Аннуитетный",
                color = if (isAnnuity) Color.White else CalculatorColors.XIAOMI_TEXT_SECONDARY,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (!isAnnuity) CalculatorColors.XIAOMI_ORANGE else Color.Transparent)
                .clickable { onSelect(false) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Дифференцированный",
                color = if (!isAnnuity) Color.White else CalculatorColors.XIAOMI_TEXT_SECONDARY,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview
@Composable
private fun PaymentTypeSelectorPreview() {
    PaymentTypeSelector(isAnnuity = true, onSelect = {})
}
