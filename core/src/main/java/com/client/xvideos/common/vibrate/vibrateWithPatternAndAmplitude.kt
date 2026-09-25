package com.client.xvideos.common.vibrate

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import timber.log.Timber

private val VIBRATION_PATTERN = longArrayOf(0, 25, 50, 50)
private val VIBRATION_AMPLITUDES = intArrayOf(0, 255, 0, 127)

fun vibrateWithPatternAndAmplitude(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    runCatching {
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(VIBRATION_PATTERN, VIBRATION_AMPLITUDES, -1)
        } else {
            VibrationEffect.createWaveform(VIBRATION_PATTERN, -1)
        }
        vibrator.vibrate(effect)
    }.onFailure { e ->
        Timber.w(e, "vibrateWithPatternAndAmplitude: ошибка воспроизведения вибрации")
    }
}
