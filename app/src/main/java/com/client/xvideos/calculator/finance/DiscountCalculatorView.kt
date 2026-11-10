package com.client.xvideos.calculator.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun DiscountCalculatorView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var priceStr by remember { mutableStateOf("5000") }
    var discountStr by remember { mutableStateOf("25") }

    val price = priceStr.toDoubleOrNull() ?: 0.0
    val discount = (discountStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)

    val (finalPriceText, savedText) = remember(price, discount) {
        val fmt = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ' ' })
        val saved = price * (discount / 100.0)
        val finalPrice = price - saved
        "${fmt.format(finalPrice)} ₽" to "${fmt.format(saved)} ₽"
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
                text = "← Скидки",
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

        FinanceInputField(label = "Исходная цена (₽)", value = priceStr, onValueChange = { priceStr = it })
        Spacer(Modifier.height(10.dp))
        FinanceInputField(label = "Скидка (%)", value = discountStr, onValueChange = { discountStr = it })

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CalculatorColors.XIAOMI_SURFACE)
                .padding(18.dp)
        ) {
            Text(
                text = "Цена со скидкой",
                color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
                fontSize = 13.sp
            )
            Text(
                text = finalPriceText,
                color = CalculatorColors.XIAOMI_ORANGE,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Вы экономите:", color = CalculatorColors.XIAOMI_TEXT_SECONDARY, fontSize = 14.sp)
                Text(text = savedText, color = Color(0xFF30D158), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
