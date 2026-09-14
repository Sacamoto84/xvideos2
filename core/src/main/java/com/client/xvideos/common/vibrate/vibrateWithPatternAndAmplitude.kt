package com.client.xvideos.common.vibrate

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import timber.log.Timber

fun vibrateWithPatternAndAmplitude(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    if (!vibrator.hasVibrator()) return

    val pattern = longArrayOf(0, 25, 50, 50)
    val amplitudes = intArrayOf(0, 255, 0, 127)

    runCatching {
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(pattern, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(pattern, -1)
        }
        vibrator.vibrate(effect)
    }.onFailure { e ->
        Timber.w(e, "vibrateWithPatternAndAmplitude: ошибка воспроизведения вибрации")
    }
}
