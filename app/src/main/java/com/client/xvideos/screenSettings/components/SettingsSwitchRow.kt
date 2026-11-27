package com.client.xvideos.screenSettings.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.R
import com.client.xvideos.common.theme.Theme

@Composable
fun SettingsSwitchRow(
    text: String,
    subtitle: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = 0,
    enabled: Boolean = true,
) {
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = SettingsOnAccentColor,
        checkedTrackColor = SettingsAccentColor,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = SettingsUncheckedColor,
        uncheckedTrackColor = Theme.tabLevel3,
        uncheckedBorderColor = Theme.Text.muted
    )

    val trailingContent: @Composable () -> Unit = remember(value, enabled, onValueChange, switchColors) {
        {
            Switch(
                checked = value,
                enabled = enabled,
                onCheckedChange = onValueChange,
                colors = switchColors
            )
        }
    }

    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = subtitle,
        trailing = trailingContent,
        modifier = modifier
    )
}

@Preview
@Composable
private fun SettingsSwitchRowPreview() {
    var checked by remember { mutableStateOf(true) }
    SettingsSwitchRow(
        icon = R.drawable.icon_red,
        text = "Переключатель",
        subtitle = if (checked) "Вкл" else "Выкл",
        value = checked,
        onValueChange = { isChecked -> checked = isChecked }
    )
}
