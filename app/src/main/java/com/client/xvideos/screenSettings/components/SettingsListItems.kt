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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.R
import com.client.xvideos.screenSettings.DialogButton
import kotlin.math.roundToInt

// Google Material 3 Dark Theme tokens (matching Chrome & Android 14+ Settings)
internal val SettingsScreenBackground = Color(0xFF1B1B1F)
internal val SettingsTopBarColor = SettingsScreenBackground
internal val SettingsCardColor = Color(0xFF3A373E)
internal val SettingsAccentColor = Color(0xFFC4C0FD)
internal val SettingsOnAccentColor = Color(0xFF2E2961)
internal val SettingsRowTextPrimary = Color(0xFFE5E2E9)
internal val SettingsRowTextSecondary = Color(0xFFA6A4AC)
internal val SettingsDividerColor = Color(0x2E79747E)
internal val WhatsAppGreen = SettingsAccentColor
internal val settingsCardShape = RoundedCornerShape(24.dp)

private val SWITCH_UNCHECKED_THUMB = Color(0xFF938F99)
private val SWITCH_UNCHECKED_TRACK = Color(0xFF48464F)
private val SWITCH_UNCHECKED_BORDER = Color(0xFF79747E)

private val SETTINGS_ICON_SIZE = 24.dp
private val SETTINGS_ITEM_MIN_HEIGHT = 60.dp
private val SETTINGS_DIVIDER_THICKNESS = 0.5.dp
private val SETTINGS_DIVIDER2_HEIGHT = 2.dp
private val SETTINGS_GROUP_HORIZONTAL_PADDING = 16.dp
private val SETTINGS_SECTION_TITLE_START_PADDING = 24.dp
private val SETTINGS_SECTION_TITLE_TOP_PADDING = 20.dp
private val SETTINGS_SECTION_TITLE_BOTTOM_PADDING = 8.dp
private val SETTINGS_SECTION_TITLE_END_PADDING = 24.dp
private val SETTINGS_SECTION_TITLE_FONT_SIZE = 14.sp
private val SETTINGS_SECTION_TITLE_LETTER_SPACING = 0.1.sp
private val SETTINGS_DEFAULT_START_INDENT = 56.dp
private val SETTINGS_ROW_TITLE_FONT_SIZE = 16.sp
private val SETTINGS_ROW_TITLE_LINE_HEIGHT = 22.sp
private val SETTINGS_ROW_SUBTITLE_FONT_SIZE = 14.sp
private val SETTINGS_ROW_SUBTITLE_LINE_HEIGHT = 18.sp
private val SETTINGS_ITEM_HORIZONTAL_PADDING = 16.dp
private val SETTINGS_ITEM_VERTICAL_PADDING = 13.dp
private val SETTINGS_ITEM_ICON_SPACER_WIDTH = 16.dp
private val SETTINGS_ITEM_SUBTITLE_SPACER_HEIGHT = 3.dp
private val SETTINGS_ITEM_TRAILING_SPACER_WIDTH = 12.dp
private val SETTINGS_SLIDER_WITH_ICON_START_PADDING = 56.dp
private val SETTINGS_SLIDER_WITHOUT_ICON_START_PADDING = 16.dp
private val SETTINGS_SLIDER_END_PADDING = 16.dp
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val COLUMN_VERTICAL_ARRANGEMENT = Arrangement.Center
private val ICON_BOX_ALIGNMENT = Alignment.Center
private val DIVIDER2_MODIFIER = Modifier
    .fillMaxWidth()
    .height(SETTINGS_DIVIDER2_HEIGHT)
    .background(SettingsScreenBackground)
private val SLIDER_COLUMN_BASE_MODIFIER = Modifier.fillMaxWidth()
private val ICON_SIZE_MODIFIER = Modifier.size(SETTINGS_ICON_SIZE)

private val FONT_WEIGHT_NORMAL = FontWeight.Normal
private val FONT_WEIGHT_MEDIUM = FontWeight.Medium
private val COLOR_TRANSPARENT = Color.Transparent
private val ITEM_ICON_SPACER_MODIFIER = Modifier.width(SETTINGS_ITEM_ICON_SPACER_WIDTH)
private val ITEM_SUBTITLE_SPACER_MODIFIER = Modifier.height(SETTINGS_ITEM_SUBTITLE_SPACER_HEIGHT)
private val ITEM_TRAILING_SPACER_MODIFIER = Modifier.width(SETTINGS_ITEM_TRAILING_SPACER_WIDTH)

private val LocalSettingsInGroup = staticCompositionLocalOf { false }

@Composable
fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val style = remember(Theme.L.Type.caption) {
        Theme.L.Type.caption.copy(
            color = SettingsAccentColor,
            fontSize = SETTINGS_SECTION_TITLE_FONT_SIZE,
            fontWeight = FONT_WEIGHT_MEDIUM,
            letterSpacing = SETTINGS_SECTION_TITLE_LETTER_SPACING
        )
    }
    Text(
        text = text,
        modifier = modifier.padding(
            start = SETTINGS_SECTION_TITLE_START_PADDING,
            top = SETTINGS_SECTION_TITLE_TOP_PADDING,
            bottom = SETTINGS_SECTION_TITLE_BOTTOM_PADDING,
            end = SETTINGS_SECTION_TITLE_END_PADDING
        ),
        color = SettingsAccentColor,
        style = style
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsSectionTitlePreview() = SettingsPreview {
    SettingsSectionTitle("Защита")
}

@Composable
fun SettingsDivider(startIndent: androidx.compose.ui.unit.Dp = SETTINGS_DEFAULT_START_INDENT) {
    val inGroup = LocalSettingsInGroup.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (inGroup) Modifier else Modifier.padding(horizontal = SETTINGS_GROUP_HORIZONTAL_PADDING))
            .background(SettingsCardColor)
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(start = startIndent, end = SETTINGS_GROUP_HORIZONTAL_PADDING),
            thickness = SETTINGS_DIVIDER_THICKNESS,
            color = SettingsDividerColor
        )
    }
}

@Composable
fun SettingsDivider2() {
    Spacer(DIVIDER2_MODIFIER)
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SETTINGS_GROUP_HORIZONTAL_PADDING)
            .clip(settingsCardShape)
            .background(SettingsCardColor)
    ) {
        CompositionLocalProvider(LocalSettingsInGroup provides true) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsDividerPreview() = SettingsPreview {
    SettingsDivider()
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsDivider2Preview() = SettingsPreview {
    SettingsDivider2()
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsGroupPreview() = SettingsPreview {
    SettingsGroup {
        SettingsListItem(
            icon = R.drawable.icon_red,
            text = "Первый пункт",
            subtitle = "Описание первого пункта"
        )
        SettingsDivider()
        SettingsListItem(
            icon = R.drawable.icon_red,
            text = "Второй пункт",
            subtitle = "Описание второго пункта"
        )
    }
}

@Composable
fun SettingsListItem(
    @DrawableRes icon: Int = 0,
    text: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val inGroup = LocalSettingsInGroup.current
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val titleStyle = remember(Theme.L.Type.rowTitle) {
        Theme.L.Type.rowTitle.copy(
            color = SettingsRowTextPrimary,
            fontSize = SETTINGS_ROW_TITLE_FONT_SIZE,
            fontWeight = FONT_WEIGHT_NORMAL,
            lineHeight = SETTINGS_ROW_TITLE_LINE_HEIGHT
        )
    }
    val subtitleStyle = remember(Theme.L.Type.rowSubtitle) {
        Theme.L.Type.rowSubtitle.copy(
            color = SettingsRowTextSecondary,
            fontSize = SETTINGS_ROW_SUBTITLE_FONT_SIZE,
            fontWeight = FONT_WEIGHT_NORMAL,
            lineHeight = SETTINGS_ROW_SUBTITLE_LINE_HEIGHT
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (inGroup) {
                    Modifier
                } else {
                    Modifier
                        .padding(horizontal = SETTINGS_ITEM_HORIZONTAL_PADDING)
                        .clip(settingsCardShape)
                }
            )
            .background(SettingsCardColor)
            .then(clickableModifier)
            .heightIn(min = SETTINGS_ITEM_MIN_HEIGHT)
            .padding(horizontal = SETTINGS_ITEM_HORIZONTAL_PADDING, vertical = SETTINGS_ITEM_VERTICAL_PADDING),
        verticalAlignment = ROW_VERTICAL_ALIGNMENT
    ) {
        if (icon != 0) {
            SettingsIcon(icon)
            Spacer(ITEM_ICON_SPACER_MODIFIER)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = COLUMN_VERTICAL_ARRANGEMENT
        ) {
            Text(
                text = text,
                color = SettingsRowTextPrimary,
                style = titleStyle
            )
            if (subtitle != null) {
                Spacer(ITEM_SUBTITLE_SPACER_MODIFIER)
                Text(
                    text = subtitle,
                    color = SettingsRowTextSecondary,
                    style = subtitleStyle
                )
            }
        }
        if (trailing != null) {
            Spacer(ITEM_TRAILING_SPACER_MODIFIER)
            trailing()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
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
        modifier = ICON_SIZE_MODIFIER,
        contentAlignment = ICON_BOX_ALIGNMENT
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = SettingsRowTextSecondary,
            modifier = ICON_SIZE_MODIFIER
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsIconPreview() = SettingsPreview {
    SettingsIcon(R.drawable.icon_red)
}

@Composable
fun SettingsValueRow(
    @DrawableRes icon: Int = 0,
    text: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = value,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
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
    @DrawableRes icon: Int = 0,
    text: String,
    subtitle: String,
    value: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onValueChange: (Boolean) -> Unit,
) {
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = SettingsOnAccentColor,
        checkedTrackColor = SettingsAccentColor,
        checkedBorderColor = COLOR_TRANSPARENT,
        uncheckedThumbColor = SWITCH_UNCHECKED_THUMB,
        uncheckedTrackColor = SWITCH_UNCHECKED_TRACK,
        uncheckedBorderColor = SWITCH_UNCHECKED_BORDER
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

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun SettingsSwitchRowPreview() = SettingsPreview {
    var checked by remember { mutableStateOf(true) }
    SettingsSwitchRow(
        icon = R.drawable.icon_red,
        text = "Переключатель",
        subtitle = if (checked) "Вкл" else "Выкл",
        value = checked,
        onValueChange = { isChecked -> checked = isChecked }
    )
}

@Composable
fun SettingsButtonRowWithDialog(
    @DrawableRes icon: Int = 0,
    text: String,
    value: String,
    textDialogTitle: String,
    textDialogBody: String,
    textDialogButton: String,
    composable: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val onDismiss = remember { { visible = false } }
    val onOpen = remember { { visible = true } }

    DialogButton(
        visible = visible,
        title = textDialogTitle,
        body = textDialogBody,
        buttonText = textDialogButton,
        onDismiss = onDismiss,
        onBlockConfirmed = onClick,
        composable = composable
    )

    val trailingContent: @Composable () -> Unit = remember(onOpen, value) {
        {
            TextButton(onClick = onOpen) {
                Text(value, color = SettingsAccentColor, fontWeight = FONT_WEIGHT_MEDIUM)
            }
        }
    }

    SettingsListItem(
        icon = icon,
        text = text,
        subtitle = null,
        trailing = trailingContent
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
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

    Column(modifier = SLIDER_COLUMN_BASE_MODIFIER) {
        SettingsListItem(
            icon = icon,
            text = text,
            subtitle = "$currentValue$suffix"
        )
        Slider(
            value = currentValue.toFloat(),
            onValueChange = { rawValue ->
                sliderValue = snapSliderValue(rawValue, min, max, step).toFloat()
            },
            onValueChangeFinished = onFinished,
            modifier = Modifier.padding(
                start = if (icon != 0) SETTINGS_SLIDER_WITH_ICON_START_PADDING else SETTINGS_SLIDER_WITHOUT_ICON_START_PADDING,
                end = SETTINGS_SLIDER_END_PADDING
            ),
            valueRange = min.toFloat()..max.toFloat(),
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccentColor,
                activeTrackColor = SettingsAccentColor,
                inactiveTrackColor = SettingsDividerColor
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
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
