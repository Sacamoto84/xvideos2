package com.client.xvideos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.vibrate.vibrateWithPatternAndAmplitude

private val ZERO_INSETS = WindowInsets(0, 0, 0, 0)
private val HAPTIC_BUTTON_SHAPE = RoundedCornerShape(14.dp)
private val HAPTIC_BUTTON_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

private val TOP_BAR_BG = Color(0xFF1B1B1B)
private val SUBTITLE_COLOR = Color(0xFFB0B0B0)
private val SCREEN_CONTAINER_COLOR = Color(0xFF2A2A2A)
private val WAVEFORM_CONTAINER_COLOR = Color(0xFF4A3B00)
private val BUTTON_CONTAINER_COLOR = Color(0xFF3A3A3A)
private val DESC_COLOR = Color(0xFFBFBFBF)

private const val TITLE_DEMO = "Haptic Feedback — демо"
private const val LABEL_CUSTOM_WAVEFORM = "Кастомный waveform (мимо Compose, прямой Vibrator)"
private const val LABEL_WAVEFORM_BTN = "Waveform: 255 → пауза → 127"

private data class HapticItem(
    val name: String,
    val desc: String,
    val type: HapticFeedbackType,
)

// Порядок — от самых «полезных» к специфичным.
private val HAPTIC_ITEMS = listOf(
    HapticItem("Confirm", "Подтверждение / успех действия", HapticFeedbackType.Confirm),
    HapticItem("Reject", "Отказ / ошибка действия", HapticFeedbackType.Reject),
    HapticItem("ToggleOn", "Переключатель → ВКЛ", HapticFeedbackType.ToggleOn),
    HapticItem("ToggleOff", "Переключатель → ВЫКЛ", HapticFeedbackType.ToggleOff),
    HapticItem("LongPress", "Долгое нажатие → действие", HapticFeedbackType.LongPress),
    HapticItem("TextHandleMove", "Перемещение хэндла в тексте", HapticFeedbackType.TextHandleMove),
    HapticItem("ContextClick", "Контекстный клик по объекту", HapticFeedbackType.ContextClick),
    HapticItem("KeyboardTap", "Нажатие экранной клавиши", HapticFeedbackType.KeyboardTap),
    HapticItem("VirtualKey", "Нажатие виртуальной кнопки", HapticFeedbackType.VirtualKey),
    HapticItem("GestureEnd", "Завершение жеста", HapticFeedbackType.GestureEnd),
    HapticItem("GestureThresholdActivate", "Жест достиг порога активации", HapticFeedbackType.GestureThresholdActivate),
    HapticItem("SegmentTick", "Шаг по дискретным позициям", HapticFeedbackType.SegmentTick),
    HapticItem("SegmentFrequentTick", "Шаг по множеству мелких позиций", HapticFeedbackType.SegmentFrequentTick),
)

private val SUBTITLE_TEXT = "Нажми кнопку, чтобы почувствовать отклик. Доступно ${HAPTIC_ITEMS.size} типов."

/**
 * Демо-экран для тестирования виброоткликов.
 *
 * Содержит кнопку на каждый поддерживаемый [HapticFeedbackType] (Compose ui 1.11),
 * чтобы вживую сравнить отклики и решить, какой тип под какое действие применять.
 * Внизу — кнопка кастомного waveform-вибро ([vibrateWithPatternAndAmplitude]).
 */
object HapticDemoScreen : Screen {

    private fun readResolve(): Any = HapticDemoScreen

    override val key: ScreenKey = "HapticDemoScreen"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        val scrollState = rememberScrollState()

        val onBack: () -> Unit = remember(navigator) {
            {
                navigator.pop().let {}
            }
        }
        BackHandler(onBack = onBack)

        val onCustomVibrate: () -> Unit = remember(context) {
            {
                vibrateWithPatternAndAmplitude(context)
            }
        }

        Scaffold(
            contentWindowInsets = ZERO_INSETS,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TOP_BAR_BG)
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                ) {
                    Text(
                        text = TITLE_DEMO,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Text(
                        text = SUBTITLE_TEXT,
                        color = SUBTITLE_COLOR,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)
                    )
                }
            },
            containerColor = SCREEN_CONTAINER_COLOR
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
                Text(
                    text = LABEL_CUSTOM_WAVEFORM,
                    color = SUBTITLE_COLOR,
                    fontSize = 13.sp
                )
                Button(
                    onClick = onCustomVibrate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WAVEFORM_CONTAINER_COLOR)
                ) {
                    Text(LABEL_WAVEFORM_BTN, color = Color.White)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun HapticButton(
    index: Int,
    name: String,
    desc: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = HAPTIC_BUTTON_SHAPE,
        colors = ButtonDefaults.buttonColors(containerColor = BUTTON_CONTAINER_COLOR),
        contentPadding = HAPTIC_BUTTON_PADDING
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$index. $name",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = desc,
                color = DESC_COLOR,
                fontSize = 13.sp
            )
        }
    }
}
