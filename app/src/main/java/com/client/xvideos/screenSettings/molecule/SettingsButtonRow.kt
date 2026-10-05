package com.client.xvideos.screenSettings.molecule

import androidx.annotation.DrawableRes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка настроек с текстовой кнопкой справа.
 *
 * @param value текст кнопки.
 * @param subtitle строка под заголовком; `null` — заголовок один.
 */
@Composable
fun SettingsButtonRow(
    @DrawableRes icon: Int = 0,
    text: String,
    value: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val trailingContent: @Composable () -> Unit = remember(onClick, value) {
        {
            TextButton(onClick = onClick) {
                Text(value, color = SettingsAccentColor, fontWeight = FontWeight.Medium)
            }
        }
    }

    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = subtitle,
        trailing = trailingContent
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsButtonRowPreview() = SettingsPreview {
    SettingsButtonRow(
        icon = R.drawable.icon_red,
        text = "Профиль",
        value = "Войти",
        subtitle = "Вход не выполнен",
        onClick = {}
    )
}
