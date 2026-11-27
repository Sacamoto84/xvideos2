package com.client.xvideos.calculator.converter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.client.xvideos.calculator.converter.molecule.ConverterDetailView
import com.client.xvideos.calculator.converter.molecule.ConverterGridView
import com.client.xvideos.calculator.converter.molecule.ConverterToolHost

/**
 * Экран «Конвертер» Xiaomi MIUI / HyperOS Light Theme.
 * Содержит сетку из 14 категорий и детальный экран конвертации.
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
