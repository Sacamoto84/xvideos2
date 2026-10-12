package com.client.xvideos.applock.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.calculator.CalculatorScreen
import com.client.xvideos.common.applock.AppLockScreen

/**
 * Содержимое экрана замка: форма кода или калькулятор-маскировка.
 *
 * @param onUnlock проверяет код и при успехе снимает замок. Вызывается обоими
 *   вариантами, поэтому сам обязан учитывать блокировку ввода: калькулятор её
 *   не показывает и не проверяет.
 * @param onUnlockFailed неудачная попытка на калькуляторе. Форма кода считает
 *   свои ошибки сама.
 * @param secretLost замок взведён, а проверить код нечем. Показывается обычным
 *   экраном и при маскировке: калькулятор молча не принимал бы верный код, и
 *   владелец не узнал бы почему.
 */
@Composable
fun AppLockGate(
    isCamouflage: Boolean,
    secretLost: Boolean,
    onUnlock: suspend (String) -> Boolean,
    onUnlockFailed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isCamouflage && !secretLost) {
            CalculatorScreen(
                onUnlock = onUnlock,
                onUnlockFailed = onUnlockFailed,
                onBack = onBack
            )
        } else {
            BackHandler(onBack = onBack)
            AppLockScreen(onUnlock = onUnlock, secretLost = secretLost)
        }
    }
}

@Preview
@Composable
private fun AppLockGatePreview() {
    AppLockGate(
        isCamouflage = false,
        secretLost = false,
        onUnlock = { true },
        onUnlockFailed = {},
        onBack = {}
    )
}
