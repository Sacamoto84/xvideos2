package com.client.xvideos.haptic

import androidx.compose.ui.hapticfeedback.HapticFeedbackType

data class HapticItem(
    val name: String,
    val desc: String,
    val type: HapticFeedbackType,
)

// Порядок — от самых «полезных» к специфичным.
val HAPTIC_ITEMS = listOf(
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
