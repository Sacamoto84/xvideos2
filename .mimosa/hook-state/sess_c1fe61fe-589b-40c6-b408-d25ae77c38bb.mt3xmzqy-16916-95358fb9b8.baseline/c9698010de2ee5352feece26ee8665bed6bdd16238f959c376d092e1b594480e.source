package com.client.xvideos.screenSettings.components

import com.client.xvideos.common.theme.Theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R
import com.client.xvideos.screenSettings.DialogButton
import kotlin.math.roundToInt

internal val SettingsScreenBackground = Theme.L.grey6
internal val SettingsTopBarColor = Theme.L.grey5
internal val SettingsCardColor = Theme.L.grey4
internal val SettingsAccentColor = Theme.L.primaryColor
internal val SettingsRowTextPrimary = Theme.L.grey0
internal val SettingsRowTextSecondary = Theme.L.grey1
internal val SettingsDividerColor = Theme.L.grey3
internal val WhatsAppGreen = SettingsAccentColor

private val LocalSettingsInGroup = staticCompositionLocalOf { false }

@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
        color = SettingsRowTextSecondary,
        style = Theme.L.Type.caption.copy(color = SettingsRowTextSecondary)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun SettingsSectionTitlePreview() = SettingsPreview {
    SettingsSectionTitle("Защита")
}

@Composable
fun SettingsDivider() {
    val inGroup = LocalSettingsInGroup.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (inGroup) Modifier else Modifier.padding(horizontal = 12.dp))
            .background(SettingsCardColor)
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = SettingsDividerColor
        )
    }
}

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SettingsCardColor)
    ) {
        CompositionLocalProvider(LocalSettingsInGroup provides true) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun SettingsDividerPreview() = SettingsPreview {
    SettingsDivider()
}

@Composable
fun SettingsListItem(
    @DrawableRes icon: Int,
    text: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val inGroup = LocalSettingsInGroup.current
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (inGroup) {
                    Modifier
                } else {
                    Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                }
            )
            .background(SettingsCardColor)
            .then(clickableModifier)
            .heightIn(min = 58.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon)
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = SettingsRowTextPrimary,
                style = Theme.L.Type.rowTitle.copy(color = SettingsRowTextPrimary)
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = SettingsRowTextSecondary,
                    style = Theme.L.Type.rowSubtitle.copy(color = SettingsRowTextSecondary)
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun SettingsListItemPreview() = SettingsPreview {
    SettingsListItem(
        icon = R.drawable.icon_red,
        text = "Название",
        subtitle = "Подзаголовок"
    )
}

@Composable
fun SettingsIcon(@DrawableRes icon: Int) {
    Box(
        modifier = Modifier
            .size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = SettingsRowTextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun SettingsIconPreview() = SettingsPreview {
    SettingsIcon(R.drawable.icon_red)
}

@Composable
fun SettingsValueRow(
    @DrawableRes icon: Int,
    text: String,
    value: String
) {
    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = value
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun SettingsValueRowPreview() = SettingsPreview {
    SettingsValueRow(
        icon = R.drawable.icon_red,
        text = "RAM кеш",
        value = "128 MB"
    )
}

@Composable
fun SettingsSwitchRow(
    @DrawableRes icon: Int,
    text: String,
    subtitle: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = subtitle,
        trailing = {
            Switch(
                checked = value,
                onCheckedChange = onValueChange
            )
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun SettingsSwitchRowPreview() = SettingsPreview {
    var checked by remember { mutableStateOf(true) }
    SettingsSwitchRow(
        icon = R.drawable.icon_red,
        text = "Переключатель",
        subtitle = if (checked) "Вкл" else "Выкл",
        value = checked,
        onValueChange = { checked = it }
    )
}

@Composable
fun SettingsButtonRowWithDialog(
    @DrawableRes icon: Int,
    text: String,
    value: String,
    textDialogTitle: String,
    textDialogBody: String,
    textDialogButton: String,
    composable: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    DialogButton(
        visible = visible,
        title = textDialogTitle,
        body = textDialogBody,
        buttonText = textDialogButton,
        onDismiss = { visible = false },
        onBlockConfirmed = { onClick() },
        composable = composable
    )

    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = value,
        trailing = {
            TextButton(onClick = { visible = true }) {
                Text(value, color = WhatsAppGreen)
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun SettingsButtonRowWithDialogPreview() = SettingsPreview {
    SettingsButtonRowWithDialog(
        icon = R.drawable.icon_red,
        text = "Очистить",
        value = "Сброс",
        textDialogTitle = "Подтвердить",
        textDialogBody = "Очистить кеш?",
        textDialogButton = "Очистить",
        onClick = {}
    )
}

@Composable
fun IntSliderSetting(
    text: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    suffix: String,
    @DrawableRes icon: Int = R.drawable.icon_red,
    enabled: Boolean = true,
    onValueChangeFinished: (Int) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val currentValue = snapSliderValue(sliderValue, min, max, step)
    val steps = ((max - min) / step - 1).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsListItem(
            icon = icon,
            text = text,
            subtitle = "$currentValue$suffix"
        )
        Slider(
            value = currentValue.toFloat(),
            onValueChange = { sliderValue = snapSliderValue(it, min, max, step).toFloat() },
            onValueChangeFinished = {
                onValueChangeFinished(snapSliderValue(sliderValue, min, max, step))
            },
            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
            valueRange = min.toFloat()..max.toFloat(),
            steps = steps,
            enabled = enabled
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF353535)
@Composable
private fun IntSliderSettingPreview() = SettingsPreview {
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

fun snapSliderValue(value: Float, min: Int, max: Int, step: Int): Int {
    val safeStep = step.coerceAtLeast(1)
    val shifted = (value.roundToInt() - min).coerceAtLeast(0)
    return (min + ((shifted + safeStep / 2) / safeStep) * safeStep).coerceIn(min, max)
}
