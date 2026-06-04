package com.client.xvideos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.vibrate.vibrateWithPatternAndAmplitude

/**
 * Демо-экран для тестирования виброоткликов.
 *
 * Содержит кнопку на каждый поддерживаемый [HapticFeedbackType] (Compose ui 1.11),
 * чтобы вживую сравнить отклики и решить, какой тип под какое действие применять.
 * Внизу — кнопка кастомного waveform-вибро ([vibrateWithPatternAndAmplitude]).
 */
object HapticDemoScreen : Screen {

    private fun readResolve(): Any = HapticDemoScreen

    private data class HapticItem(
        val name: String,
        val desc: String,
        val type: HapticFeedbackType,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        // Порядок — от самых «полезных» к специфичным.
        val items = remember {
            listOf(
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
        }

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B1B1B))
                ) {
                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier
                            .displayCutoutPadding()
                            .padding(start = 4.dp, top = 4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Haptic Feedback — демо",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Text(
                        text = "Нажми кнопку, чтобы почувствовать отклик. Доступно ${items.size} типов.",
                        color = Color(0xFFB0B0B0),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)
                    )
                }
            },
            containerColor = Color(0xFF2A2A2A)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEachIndexed { index, item ->
                    HapticButton(
                        index = index + 1,
                        name = item.name,
                        desc = item.desc,
                        onClick = { haptic.performHapticFeedback(item.type) }
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Кастомный waveform (мимо Compose, прямой Vibrator)",
                    color = Color(0xFFB0B0B0),
                    fontSize = 13.sp
                )
                Button(
                    onClick = { vibrateWithPatternAndAmplitude(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A3B00))
                ) {
                    Text("Waveform: 255 → пауза → 127", color = Color.White)
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
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
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
                color = Color(0xFFBFBFBF),
                fontSize = 13.sp
            )
        }
    }
}
