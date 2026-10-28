package com.client.xvideos.screenSettings.atom

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsDestructiveColor

private const val DISABLED_ALPHA = 0.38f

/**
 * Кнопка в правой части строки настроек: овал с обводкой цвета текста.
 *
 * Обводка показывает, что это кнопка, а не значение строки.
 *
 * @param color цвет текста и обводки: [SettingsAccentColor] или
 *   [SettingsDestructiveColor] для выхода и очистки.
 */
@Composable
internal fun SettingsOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SettingsAccentColor,
    enabled: Boolean = true,
) {
    val shownColor = if (enabled) color else color.copy(alpha = DISABLED_ALPHA)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = BorderStroke(1.dp, shownColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color,
            disabledContentColor = shownColor
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

@Preview
@Composable
private fun SettingsOutlinedButtonPreview() {
    SettingsOutlinedButton(text = "Выйти", onClick = {}, color = SettingsDestructiveColor)
}
