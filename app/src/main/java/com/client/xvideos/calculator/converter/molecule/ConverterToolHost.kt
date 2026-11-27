package com.client.xvideos.calculator.converter.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.calculator.converter.ConverterCategory
import com.client.xvideos.calculator.finance.AgeCalculatorView
import com.client.xvideos.calculator.finance.BmiCalculatorView
import com.client.xvideos.calculator.finance.DiscountCalculatorView
import com.client.xvideos.calculator.finance.LoanCalculatorView

@Composable
internal fun ConverterToolHost(
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

@Preview
@Composable
private fun ConverterToolHostPreview() {
    ConverterToolHost(category = ConverterCategory.BMI, onBack = {})
}
