package com.client.xvideos.screenSettings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun IntSliderSetting(
    text: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    suffix: String,
    @DrawableRes icon: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (Int) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val currentValue = snapSliderValue(sliderValue, min, max, step)
    val steps = ((max - min) / step - 1).coerceAtLeast(0)

    val onFinished = remember(sliderValue, min, max, step, onValueChangeFinished) {
        {
            onValueChangeFinished(snapSliderValue(sliderValue, min, max, step))
        }
    }
    val onSliderChange: (Float) -> Unit = remember(min, max, step) {
        { rawValue ->
            sliderValue = snapSliderValue(rawValue, min, max, step).toFloat()
        }
    }
    val valueRange = remember(min, max) { min.toFloat()..max.toFloat() }
    val subtitleText = remember(currentValue, suffix) { "$currentValue$suffix" }

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsListItem(
            icon = icon,
            text = text,
            subtitle = subtitleText
        )
        val sliderModifier = if (icon != 0) {
            Modifier.padding(start = 56.dp, end = 16.dp)
        } else {
            Modifier.padding(start = 16.dp, end = 16.dp)
        }
        val sliderColors = SliderDefaults.colors(
            thumbColor = SettingsAccentColor,
            activeTrackColor = SettingsAccentColor,
            inactiveTrackColor = SettingsDividerColor
        )
        Slider(
            value = currentValue.toFloat(),
            onValueChange = onSliderChange,
            onValueChangeFinished = onFinished,
            modifier = sliderModifier,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = sliderColors
        )
    }
}

@Preview
@Composable
private fun IntSliderSettingPreview() {
    IntSliderSetting(
        text = "RAM кеш",
        value = 10,
        min = 5,
        max = 50,
        step = 5,
        suffix = "%",
        onValueChangeFinished = {}
    )
}
