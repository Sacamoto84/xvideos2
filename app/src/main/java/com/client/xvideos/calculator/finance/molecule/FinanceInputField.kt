package com.client.xvideos.calculator.finance.molecule

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.common.ui.excludedFromAutofill

@Composable
fun FinanceInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier.fillMaxWidth().excludedFromAutofill()
    )
}

@Preview
@Composable
private fun FinanceInputFieldPreview() {
    FinanceInputField(label = "Сумма", value = "1000", onValueChange = {})
}
