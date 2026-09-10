package com.client.xvideos.r.ui.explorer.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey

object  FavoritesTab : Screen {

    private fun readResolve(): Any = FavoritesTab

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {

        val haptic = LocalHapticFeedback.current

        Column {
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)}) { Text("TextHandleMove") }
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.ContextClick)}) { Text("ContextClick") }
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.Reject)}) { Text("Reject") }
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)}) { Text("ToggleOn") }

            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.Confirm)}) { Text("Confirm") }
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)}) { Text("GestureEnd") }
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)}) { Text("KeyboardTap") }
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)}) { Text("GestureThresholdActivate") }

            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)}) { Text("SegmentTicktivate") }
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress)}) { Text("LongPress") }
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)}) { Text("ToggleOff") }

        }
    }
}

