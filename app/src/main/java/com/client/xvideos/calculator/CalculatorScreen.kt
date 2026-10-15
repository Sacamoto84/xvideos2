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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.calculator.molecule.CalculatorDisplay
import com.client.xvideos.calculator.molecule.CalculatorKeypad
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.adaptive.currentWidthClass

/**
 * Экран-камуфляж «Калькулятор».
 *
 * Выполнен в виде нативного системного калькулятора с равномерной сеткой кнопок 4×5.
 * При вводе верного PIN-кода и нажатии «=» разблокирует приложение через [onUnlock].
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

    val canBackspace = state.displayValue == CalculatorState.ERROR_TEXT || (!state.isNewEntry && state.displayValue != "0")
    val canClear = !canBackspace && !state.isAllClear

    val onBackspaceAction = remember(state, haptic) { { state.onBackspace(haptic) } }
    val onClearAction = remember(state, haptic) { { state.onClear(haptic) } }
    val onBackAction = remember(onBack) { { onBack() } }

    BackHandler(enabled = canBackspace, onBack = onBackspaceAction)
    BackHandler(enabled = canClear, onBack = onClearAction)
    BackHandler(enabled = !canBackspace && !canClear, onBack = onBackAction)

    val widthClass = currentWidthClass()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .background(Color.Black)
            .displayCutoutPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // На телефоне null: Column получает ровно прежнюю геометрию.
        val keypadWidth = keypadWidthDp(widthClass, maxWidth.value.toInt(), maxHeight.value.toInt())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (keypadWidth != null) Modifier.adaptiveContentWidth(keypadWidth.dp) else Modifier),
            verticalArrangement = Arrangement.Bottom
        ) {
            CalculatorDisplay(
                displayValue = state.displayValue,
                expressionHistory = state.expressionHistory,
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
}

@Preview
@Composable
private fun CalculatorScreenPreview() {
    CalculatorScreen(onUnlock = { true })
}
