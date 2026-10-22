package com.client.xvideos.x.screens.videoplayer.model

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Панель плеера — цель касания: касание внутри неё не уходит к видео под ней,
 * где свайп перематывает на ±5 с и тап ставит паузу. Ничего не забирает, так
 * что кнопки и ползунок панели работают как раньше.
 *
 * Без этого всё, что на панели не кнопка и не полоса ползунка в 12dp (поля,
 * метки времени, фон), доставалось видео: промах по бегунку на несколько dp
 * перематывал на ±5 с вместо перетаскивания. Прямое попадание в видео к тому
 * же отключало у ползунка расширенную зону касания в 48dp (финальное ревью
 * плана свайпа, I1).
 */
fun Modifier.panelTouchTarget(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
}
