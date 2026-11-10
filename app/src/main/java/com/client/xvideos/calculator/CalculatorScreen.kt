package com.client.xvideos.calculator

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.calculator.converter.ConverterScreen
import com.client.xvideos.calculator.molecule.CalculatorDisplay
import com.client.xvideos.calculator.molecule.CalculatorKeypad
import com.client.xvideos.calculator.molecule.CalculatorTab
import com.client.xvideos.calculator.molecule.CalculatorTopBar
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.adaptive.currentWidthClass

/**
 * Экран-камуфляж «Калькулятор» в актуальном светлом дизайне Xiaomi MIUI / HyperOS.
 *
 * Содержит:
 * - Основной и инженерный калькулятор с тактильным откликом;
 * - Конвертер величин, систем счисления и валют с 14 категориями;
 * - Интегрированные финансовые и бытовые инструменты (ипотека, ИМТ, возраст, скидка).
 *
 * При наборе секретного PIN-кода и нажатии «=» разблокирует приложение через [onUnlock].
 */
@Composable
fun CalculatorScreen(
    onUnlock: suspend (String) -> Boolean,
    onUnlockFailed: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val state = rememberSaveable(saver = CalculatorState.Saver) { CalculatorState() }

    var selectedTab by rememberSaveable { mutableStateOf(CalculatorTab.CALCULATOR) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                state.resetState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (selectedTab != CalculatorTab.CALCULATOR) {
        BackHandler { selectedTab = CalculatorTab.CALCULATOR }
    } else {
        val canBackspace = state.displayValue == CalculatorState.ERROR_TEXT || (!state.isNewEntry && state.displayValue != "0")
        val canClear = !canBackspace && !state.isAllClear

        val onBackspaceAction = remember(state, haptic) { { state.onBackspace(haptic) } }
        val onClearAction = remember(state, haptic) { { state.onClear(haptic) } }
        val onBackAction = remember(onBack) { { onBack() } }

        BackHandler(enabled = canBackspace, onBack = onBackspaceAction)
        BackHandler(enabled = canClear, onBack = onClearAction)
        BackHandler(enabled = !canBackspace && !canClear, onBack = onBackAction)
    }

    val widthClass = currentWidthClass()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .background(CalculatorColors.XIAOMI_BG)
            .displayCutoutPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val keypadWidth = keypadWidthDp(widthClass, maxWidth.value.toInt(), maxHeight.value.toInt())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (keypadWidth != null) Modifier.adaptiveContentWidth(keypadWidth.dp) else Modifier)
        ) {
            CalculatorTopBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            when (selectedTab) {
                CalculatorTab.CALCULATOR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        CalculatorDisplay(
                            displayValue = state.displayValue,
                            expressionHistory = state.expressionHistory,
                            isScientific = state.isScientific,
                            isRad = state.isRad,
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorKeypad(
                            state = state,
                            scope = scope,
                            haptic = haptic,
                            onUnlock = onUnlock,
                            onUnlockFailed = onUnlockFailed
                        )
                    }
                }
                CalculatorTab.CONVERTER -> {
                    ConverterScreen(
                        onNavigateBackToCalc = { selectedTab = CalculatorTab.CALCULATOR },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CalculatorScreenPreview() {
    CalculatorScreen(onUnlock = { true })
}
