package com.client.xvideos.x.screens.login.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.feature.auth.XSignInStep
import kotlinx.coroutines.delay

private const val SECOND_MS = 1_000L

/**
 * Ход входа в X: крутилка, этап и сколько секунд вход уже идёт.
 *
 * Проверка устройства на сайте может идти заметно дольше обычного запроса —
 * счётчик показывает, что вход не завис.
 *
 * @param step текущий этап входа.
 * @param color цвет крутилки и текста.
 */
@Composable
fun XSignInProgress(
    step: XSignInStep,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(SECOND_MS)
            seconds++
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = color,
            strokeWidth = 2.dp,
        )
        Text(
            text = "${step.text} $seconds с",
            color = color,
            fontSize = 18.sp,
        )
    }
}

@Preview
@Composable
private fun XSignInProgressPreview() {
    XSignInProgress(step = XSignInStep.CAPTCHA, color = Color.LightGray)
}
