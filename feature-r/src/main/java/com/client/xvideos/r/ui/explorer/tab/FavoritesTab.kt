package com.client.xvideos.r.ui.explorer.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey

object FavoritesTab : Screen {

    private fun readResolve(): Any = FavoritesTab

    override val key: ScreenKey = "RedFavoritesTab"

    @Composable
    override fun Content() {
        val haptic = LocalHapticFeedback.current
        FavoritesTabContent(haptic = haptic)
    }
}

@Composable
fun FavoritesTabContent(
    haptic: HapticFeedback?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove) }) { Text("TextHandleMove") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.ContextClick) }) { Text("ContextClick") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.Reject) }) { Text("Reject") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.ToggleOn) }) { Text("ToggleOn") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.Confirm) }) { Text("Confirm") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.GestureEnd) }) { Text("GestureEnd") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.KeyboardTap) }) { Text("KeyboardTap") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) }) { Text("GestureThresholdActivate") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.SegmentTick) }) { Text("SegmentTick") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.LongPress) }) { Text("LongPress") }
        Button(onClick = { haptic?.performHapticFeedback(HapticFeedbackType.ToggleOff) }) { Text("ToggleOff") }
    }
}

@Preview
@Composable
private fun FavoritesTabPreview() {
    FavoritesTabContent(haptic = null)
}
