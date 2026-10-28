package com.client.xvideos.screenSettings.molecule

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.screenSettings.atom.SettingsOutlinedButton
import com.client.xvideos.screenSettings.components.SettingsAccentColor
import com.client.xvideos.screenSettings.components.SettingsDestructiveColor
import com.client.xvideos.screenSettings.components.SettingsListItem
import com.client.xvideos.screenSettings.components.SettingsPreview

/**
 * Строка настроек с кнопкой с обводкой справа.
 *
 * @param value текст кнопки.
 * @param subtitle строка под заголовком; `null` — заголовок один.
 * @param color цвет кнопки: [SettingsDestructiveColor] для выхода и очистки.
 */
@Composable
fun SettingsButtonRow(
    @DrawableRes icon: Int = 0,
    text: String,
    value: String,
    subtitle: String? = null,
    color: Color = SettingsAccentColor,
    onClick: () -> Unit
) {
    val trailingContent: @Composable () -> Unit = remember(onClick, value, color) {
        {
            SettingsOutlinedButton(text = value, onClick = onClick, color = color)
        }
    }

    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = subtitle,
        trailing = trailingContent
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
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
