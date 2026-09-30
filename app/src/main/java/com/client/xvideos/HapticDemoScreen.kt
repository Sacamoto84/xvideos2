package com.client.xvideos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.vibrate.vibrateWithPatternAndAmplitude
import com.client.xvideos.haptic.HAPTIC_ITEMS
import com.client.xvideos.haptic.atom.HapticButton
import com.client.xvideos.haptic.molecule.CustomWaveformSection
import com.client.xvideos.haptic.molecule.HapticTopBar

/**
 * Демо-экран для тестирования виброоткликов.
 *
 * Содержит кнопку на каждый поддерживаемый [androidx.compose.ui.hapticfeedback.HapticFeedbackType] (Compose ui 1.11),
 * чтобы вживую сравнить отклики и решить, какой тип под какое действие применять.
 * Внизу — кнопка кастомного waveform-вибро ([vibrateWithPatternAndAmplitude]).
 */
object HapticDemoScreen : Screen {

    private fun readResolve(): Any = HapticDemoScreen

    override val key: ScreenKey = "HapticDemoScreen"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val onBack: () -> Unit = remember(navigator) {
            {
                navigator.pop().let {}
            }
        }
        BackHandler(onBack = onBack)

        HapticDemoScreenContent()
    }
}

@Composable
fun HapticDemoScreenContent() {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val onCustomVibrate: () -> Unit = remember(context) {
        {
            vibrateWithPatternAndAmplitude(context)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            HapticTopBar(totalItems = HAPTIC_ITEMS.size)
        },
        containerColor = Color(0xFF2A2A2A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HAPTIC_ITEMS.forEachIndexed { index, item ->
                key(item.name) {
                    val handleClick = remember(haptic, item.type) {
                        {
                            haptic.performHapticFeedback(item.type)
                        }
                    }
                    HapticButton(
                        index = index + 1,
                        name = item.name,
                        desc = item.desc,
                        onClick = handleClick
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            CustomWaveformSection(onCustomVibrate = onCustomVibrate)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
private fun HapticDemoScreenContentPreview() {
    HapticDemoScreenContent()
}
