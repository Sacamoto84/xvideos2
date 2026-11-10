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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.common.ui.excludedFromAutofill
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.pow

@Composable
fun LoanCalculatorView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amountStr by remember { mutableStateOf("1000000") }
    var rateStr by remember { mutableStateOf("16.5") }
    var monthsStr by remember { mutableStateOf("36") }
    var isAnnuity by remember { mutableStateOf(true) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val rate = rateStr.toDoubleOrNull() ?: 0.0
    val months = (monthsStr.toIntOrNull() ?: 1).coerceAtLeast(1)

    val monthlyRate = (rate / 100.0) / 12.0

    val (monthlyPaymentText, overpayText, totalText) = remember(amount, rate, months, isAnnuity) {
        val fmt = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ' ' })
        if (amount <= 0.0) {
            Triple("0 ₽", "0 ₽", "0 ₽")
        } else if (isAnnuity) {
            val payment = if (monthlyRate > 0.0) {
                val factor = (1.0 + monthlyRate).pow(months)
                amount * (monthlyRate * factor) / (factor - 1.0)
            } else {
                amount / months
            }
            val total = payment * months
            val overpay = total - amount
            Triple("${fmt.format(payment.toLong())} ₽", "${fmt.format(overpay.toLong())} ₽", "${fmt.format(total.toLong())} ₽")
        } else {
            val principalPart = amount / months
            val firstPayment = principalPart + amount * monthlyRate
            val lastPayment = principalPart + (amount / months) * monthlyRate
            val overpay = (months + 1) * amount * monthlyRate / 2.0
            val total = amount + overpay
            val paymentRange = "${fmt.format(firstPayment.toLong())} ... ${fmt.format(lastPayment.toLong())} ₽"
            Triple(paymentRange, "${fmt.format(overpay.toLong())} ₽", "${fmt.format(total.toLong())} ₽")
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
                text = "← Ипотека и Кредит",
                color = CalculatorColors.XIAOMI_ORANGE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        PaymentTypeSelector(isAnnuity = isAnnuity, onSelect = { isAnnuity = it })

        Spacer(Modifier.height(16.dp))

        FinanceInputField(label = "Сумма кредита (₽)", value = amountStr, onValueChange = { amountStr = it })
        Spacer(Modifier.height(10.dp))
        FinanceInputField(label = "Процентная ставка (% годовых)", value = rateStr, onValueChange = { rateStr = it })
        Spacer(Modifier.height(10.dp))
        FinanceInputField(label = "Срок (месяцев)", value = monthsStr, onValueChange = { monthsStr = it })

        Spacer(Modifier.height(20.dp))

        LoanResultCard(monthlyPayment = monthlyPaymentText, overpay = overpayText, total = totalText)
    }
}

@Composable
private fun PaymentTypeSelector(isAnnuity: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
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

@Composable
private fun LoanResultCard(monthlyPayment: String, overpay: String, total: String) {
    Column(
        modifier = Modifier
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

@Composable
fun FinanceInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = CalculatorColors.XIAOMI_TEXT_SECONDARY) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CalculatorColors.XIAOMI_TEXT_PRIMARY,
            unfocusedTextColor = CalculatorColors.XIAOMI_TEXT_PRIMARY,
            focusedBorderColor = CalculatorColors.XIAOMI_ORANGE,
            unfocusedBorderColor = CalculatorColors.XIAOMI_DIVIDER,
            focusedContainerColor = CalculatorColors.XIAOMI_SURFACE,
            unfocusedContainerColor = CalculatorColors.XIAOMI_SURFACE
        ),
        modifier = Modifier.fillMaxWidth().excludedFromAutofill()
    )
}
