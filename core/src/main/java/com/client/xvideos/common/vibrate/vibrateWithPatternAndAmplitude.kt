package com.client.xvideos.common.vibrate

import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import android.os.Build

fun vibrateWithPatternAndAmplitude(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    //val pattern = longArrayOf(0, 200, 100, 300, 400, 500) // Пауза, вибрация, пауза, вибрация и т.д.
    //val amplitudes = intArrayOf(0, 255, 0, 128, 0, 64) // Амплитуды для каждого сегмента (0 - пауза, 255 - максимум)

    val pattern = longArrayOf(0, 25, 50, 50)    // Немедленно, вибрация на 100 мс, пауза 50 мс, снова вибрация на 100 мс
    val amplitudes = intArrayOf(0, 255, 0, 127) // Амплитуды для каждого сегмента (0 - пауза, 255 - максимум)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val vibrationEffect = VibrationEffect.createWaveform(pattern, amplitudes, -1) // -1 - без повторения
        vibrator.vibrate(vibrationEffect)
    } else {
        // Для старых версий API амплитуду задать нельзя, можно только использовать стандартный вибрационный паттерн
        vibrator.vibrate(pattern, -1)
    }
}
