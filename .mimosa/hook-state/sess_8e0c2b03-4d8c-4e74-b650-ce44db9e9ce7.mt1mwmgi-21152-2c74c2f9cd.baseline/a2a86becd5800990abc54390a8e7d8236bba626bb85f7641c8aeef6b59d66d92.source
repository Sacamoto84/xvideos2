package com.client.xvideos.screenSettings

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Отображает строку конфигурации с меткой и значением.
 *
 * @param text Текст метки, который будет отображаться в левой части строки.
 * @param value Текст значения, который будет отображаться в правой части строки.
 */
@Composable
fun ConfigText(text: String, value: String) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp).padding(vertical = 2.dp).height(48.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = styleTextConfig)
        Text(value, style = Theme.L.Type.rowValue)
    }
}

@Preview
@Composable
fun ConfigTextPreview() {
    ConfigText(text = "Sample Text", value = "Sample Value")
}

