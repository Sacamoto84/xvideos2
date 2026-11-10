package com.client.xvideos.calculator.converter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.calculator.CalculatorColors
import com.client.xvideos.calculator.finance.AgeCalculatorView
import com.client.xvideos.calculator.finance.BmiCalculatorView
import com.client.xvideos.calculator.finance.DiscountCalculatorView
import com.client.xvideos.calculator.finance.LoanCalculatorView

/**
 * Экран «Конвертер» Xiaomi MIUI / HyperOS Light Theme.
 * Содержит сетку из 14 категорий (Image 2) и детальный экран конвертации (Image 3).
 */
@Composable
fun ConverterScreen(
    onNavigateBackToCalc: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<ConverterCategory?>(null) }

    when {
        selectedCategory?.isTool == true -> {
            ConverterToolHost(
                category = selectedCategory!!,
                onBack = { selectedCategory = null },
                modifier = modifier
            )
        }
        selectedCategory != null -> {
            ConverterDetailView(
                category = selectedCategory!!,
                onBack = { selectedCategory = null },
                modifier = modifier
            )
        }
        else -> {
            ConverterGridView(
                onSelect = { selectedCategory = it },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ConverterToolHost(
    category: ConverterCategory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    when (category) {
        ConverterCategory.FINANCE -> LoanCalculatorView(onBack = onBack, modifier = modifier)
        ConverterCategory.DATE -> AgeCalculatorView(onBack = onBack, modifier = modifier)
        ConverterCategory.DISCOUNT -> DiscountCalculatorView(onBack = onBack, modifier = modifier)
        ConverterCategory.BMI -> BmiCalculatorView(onBack = onBack, modifier = modifier)
        else -> Unit
    }
}

@Composable
private fun ConverterGridView(
    onSelect: (ConverterCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = modifier
            .fillMaxSize()
            .background(CalculatorColors.XIAOMI_BG)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        items(ConverterCategory.entries) { category ->
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(category) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ConverterCategoryIcon(
                    category = category,
                    size = 32.dp,
                    tint = Color(0xFF707070)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = category.title,
                    color = CalculatorColors.XIAOMI_TEXT_PRIMARY,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ConverterDetailView(
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

@Composable
private fun ConverterTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CalculatorColors.XIAOMI_TEXT_PRIMARY
            )
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = CalculatorColors.XIAOMI_TEXT_PRIMARY,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.size(36.dp))
    }
}

@Composable
private fun ConverterRow(
    unit: String,
    value: String,
    isOrange: Boolean,
    availableUnits: List<String>,
    onUnitSelected: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showMenu = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unit,
                    color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = " ⇅",
                    color = CalculatorColors.XIAOMI_TEXT_SECONDARY,
                    fontSize = 14.sp
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(CalculatorColors.XIAOMI_BG)
            ) {
                availableUnits.forEach { u ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = u,
                                color = if (u == unit) CalculatorColors.XIAOMI_ORANGE else CalculatorColors.XIAOMI_TEXT_PRIMARY
                            )
                        },
                        onClick = {
                            onUnitSelected(u)
                            showMenu = false
                        }
                    )
                }
            }
        }

        Text(
            text = value,
            color = if (isOrange) CalculatorColors.XIAOMI_ORANGE else CalculatorColors.XIAOMI_TEXT_PRIMARY,
            fontSize = 34.sp,
            fontWeight = if (isOrange) FontWeight.Normal else FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}
