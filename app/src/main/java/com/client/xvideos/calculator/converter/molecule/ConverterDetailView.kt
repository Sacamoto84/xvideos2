package com.client.xvideos.calculator.converter.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.converter.ConverterCategory
import com.client.xvideos.calculator.converter.ConverterKeypad
import com.client.xvideos.calculator.converter.UnitConverterEngine

@Composable
internal fun ConverterDetailView(
    category: ConverterCategory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var fromUnit by remember(category) { mutableStateOf(category.defaultUnits.firstOrNull() ?: "") }
    var toUnit by remember(category) {
        mutableStateOf(category.defaultUnits.getOrElse(1) { category.defaultUnits.firstOrNull() ?: "" })
    }
    var inputString by remember { mutableStateOf("1") }

    val resultString = remember(category, inputString, fromUnit, toUnit) {
        if (category == ConverterCategory.BASE) {
            UnitConverterEngine.convertBase(inputString, fromUnit, toUnit)
        } else {
            val inputValue = inputString.toDoubleOrNull() ?: 0.0
            val res = UnitConverterEngine.convert(category, inputValue, fromUnit, toUnit)
            UnitConverterEngine.formatResult(res)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalculatorColors.XIAOMI_BG)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ConverterTopBar(title = category.title, onBack = onBack)
            Spacer(Modifier.height(24.dp))

            ConverterRow(
                unit = fromUnit,
                value = if (inputString.isEmpty()) "0" else inputString,
                isOrange = true,
                availableUnits = category.defaultUnits,
                onUnitSelected = { fromUnit = it }
            )

            Spacer(Modifier.height(20.dp))

            ConverterRow(
                unit = toUnit,
                value = resultString,
                isOrange = false,
                availableUnits = category.defaultUnits,
                onUnitSelected = { toUnit = it }
            )
        }

        ConverterKeypad(
            onDigit = { d ->
                if (inputString == "0" && d != "00") inputString = d else inputString += d
            },
            onDecimal = {
                if (!inputString.contains(".")) {
                    inputString = if (inputString.isEmpty()) "0." else "$inputString."
                }
            },
            onBackspace = {
                if (inputString.isNotEmpty()) inputString = inputString.dropLast(1)
            },
            onClear = { inputString = "0" },
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Preview
@Composable
private fun ConverterDetailViewPreview() {
    ConverterDetailView(category = ConverterCategory.LENGTH, onBack = {})
}
