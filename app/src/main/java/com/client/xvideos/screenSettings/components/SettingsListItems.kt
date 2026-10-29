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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.R
import com.client.xvideos.screenSettings.DialogButton
import com.client.xvideos.screenSettings.molecule.SettingsButtonRow
import kotlin.math.roundToInt

// Поверхности и текст — нейтральные серые приложения (Theme), без синего и
// сиреневого оттенка Material 3: настройки не должны выглядеть отдельным
// приложением. Лавандовый остаётся только акцентом.
internal val SettingsScreenBackground = Theme.background
internal val SettingsTopBarColor = SettingsScreenBackground
internal val SettingsCardColor = Theme.tabLevel2
internal val SettingsAccentColor = Color(0xFFC4C0FD)
internal val SettingsOnAccentColor = Color(0xFF2E2961)
internal val SettingsRowTextPrimary = Theme.Text.primary
internal val SettingsRowTextSecondary = Theme.Text.secondary
internal val SettingsDividerColor = Color(0x2E7A7A7A)

/** Невыбранные переключатели и радиокнопки. */
internal val SettingsUncheckedColor = Theme.L.grey2

/** Кнопки выхода и очистки. */
internal val SettingsDestructiveColor = Color(0xFFFF7A7A)
internal val WhatsAppGreen = SettingsAccentColor
private val SettingsCardCorner = 24.dp
internal val settingsCardShape = RoundedCornerShape(SettingsCardCorner)

// Зазор и углы между блоками группы — как у сегментированных списков Material 3.
private val SettingsGroupGapHeight = 2.dp
private val SettingsGroupInnerCorner = 4.dp

private val SETTINGS_ITEM_CORE_MODIFIER = Modifier
    .background(SettingsCardColor)
    .heightIn(min = 60.dp)
    .padding(horizontal = 16.dp, vertical = 13.dp)

private val SETTINGS_ITEM_IN_GROUP_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .then(SETTINGS_ITEM_CORE_MODIFIER)

private val SETTINGS_ITEM_STANDALONE_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp)
    .clip(settingsCardShape)
    .then(SETTINGS_ITEM_CORE_MODIFIER)

private val SETTINGS_GROUP_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp)

private val LocalSettingsInGroup = staticCompositionLocalOf { false }

/** Метка разделителя, который в [SettingsGroup] становится зазором между блоками. */
private object SettingsGroupGap

/** Фон блоков группы: [SettingsGroup] обрезает его по форме всех блоков сразу. */
private val SettingsGroupBackground: @Composable () -> Unit = {
    Box(Modifier.background(SettingsCardColor))
}

@Composable
fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val style = remember(Theme.L.Type.caption) {
        Theme.L.Type.caption.copy(
            color = SettingsAccentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp
        )
    }
    val titleModifier = modifier.padding(
        start = 24.dp,
        top = 20.dp,
        bottom = 8.dp,
        end = 24.dp
    )
    Text(
        text = text,
        modifier = titleModifier,
        color = SettingsAccentColor,
        style = style
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SettingsSectionTitlePreview() = SettingsPreview {
    SettingsSectionTitle("Защита")
}

/**
 * Разделитель строк. Прямо в [SettingsGroup] он становится зазором между
 * блоками; внутри вложенного блока (дерево папок бэкапа) остаётся линией.
 */
@Composable
fun SettingsDivider(startIndent: androidx.compose.ui.unit.Dp = 56.dp) {
    val inGroup = LocalSettingsInGroup.current
    val containerModifier = if (inGroup) {
        Modifier
            .layoutId(SettingsGroupGap)
            .fillMaxWidth()
            .background(SettingsCardColor)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(SettingsCardColor)
    }
    val dividerModifier = Modifier.padding(start = startIndent, end = 16.dp)
    Box(
        modifier = containerModifier
    ) {
        HorizontalDivider(
            modifier = dividerModifier,
            thickness = 0.5.dp,
            color = SettingsDividerColor
        )
    }
}

/** Разделитель полосой фона экрана; прямо в [SettingsGroup] — зазор между блоками. */
@Composable
fun SettingsDivider2() {
    Spacer(
        Modifier
            .layoutId(SettingsGroupGap)
            .fillMaxWidth()
            .height(SettingsGroupGapHeight)
            .background(SettingsScreenBackground)
    )
}

/**
 * Группа строк, как в настройках Android: строки, разделённые
 * [SettingsDivider] или [SettingsDivider2], — отдельные блоки с зазором.
 * Внешние углы группы большие, углы между блоками маленькие. Строки без
 * разделителя между ними — один блок.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        contents = listOf(
            SettingsGroupBackground,
            { CompositionLocalProvider(LocalSettingsInGroup provides true) { content() } }
        ),
        modifier = modifier.then(SETTINGS_GROUP_BASE_MODIFIER),
        measurePolicy = SettingsGroupMeasurePolicy
    )
}

/** Строка группы и её форма: углы скруглены только на краях блока. */
private class SettingsGroupRow(val placeable: Placeable, val y: Int, val shape: Shape)

/** Блок группы по вертикали, в пикселях, с радиусами верхних и нижних углов. */
private data class SettingsGroupBlock(
    val top: Float,
    val bottom: Float,
    val topCorner: Float,
    val bottomCorner: Float
)

/** Форма фона группы: все блоки сразу, зазоры между ними пустые. */
private data class SettingsGroupBackgroundShape(val blocks: List<SettingsGroupBlock>) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        blocks.forEach { block ->
            val top = CornerRadius(block.topCorner)
            val bottom = CornerRadius(block.bottomCorner)
            path.addRoundRect(
                RoundRect(
                    left = 0f,
                    top = block.top,
                    right = size.width,
                    bottom = block.bottom,
                    topLeftCornerRadius = top,
                    topRightCornerRadius = top,
                    bottomRightCornerRadius = bottom,
                    bottomLeftCornerRadius = bottom
                )
            )
        }
        return Outline.Generic(path)
    }
}

private object SettingsGroupMeasurePolicy : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult {
        val blocks = measureBlocks(measurables[1], constraints.copy(minWidth = 0, minHeight = 0))
        val gap = SettingsGroupGapHeight.roundToPx()
        val rows = ArrayList<SettingsGroupRow>()
        val blockBounds = ArrayList<SettingsGroupBlock>(blocks.size)
        var y = 0
        blocks.forEachIndexed { blockIndex, block ->
            if (blockIndex > 0) y += gap
            val topCorner = if (blockIndex == 0) SettingsCardCorner else SettingsGroupInnerCorner
            val bottomCorner = if (blockIndex == blocks.lastIndex) SettingsCardCorner else SettingsGroupInnerCorner
            val blockTop = y
            block.forEachIndexed { rowIndex, placeable ->
                val rowTop = if (rowIndex == 0) topCorner else 0.dp
                val rowBottom = if (rowIndex == block.lastIndex) bottomCorner else 0.dp
                val shape = RoundedCornerShape(
                    topStart = rowTop,
                    topEnd = rowTop,
                    bottomEnd = rowBottom,
                    bottomStart = rowBottom
                )
                rows += SettingsGroupRow(placeable, y, shape)
                y += placeable.height
            }
            blockBounds += SettingsGroupBlock(blockTop.toFloat(), y.toFloat(), topCorner.toPx(), bottomCorner.toPx())
        }
        val width = constraints.constrainWidth(rows.maxOfOrNull { it.placeable.width } ?: 0)
        val height = constraints.constrainHeight(y)
        val background = measurables[0].first().measure(Constraints.fixed(width, height))
        val backgroundShape = SettingsGroupBackgroundShape(blockBounds)
        return layout(width, height) {
            background.placeWithLayer(0, 0) {
                shape = backgroundShape
                clip = true
            }
            // Каждая строка обрезана своей формой: иначе её фон и рябь нажатия
            // закрыли бы скруглённые углы блока.
            rows.forEach { row ->
                row.placeable.placeWithLayer(0, row.y) {
                    shape = row.shape
                    clip = true
                }
            }
        }
    }

    /** Строки по блокам: разделитель-метка закрывает блок и сам не рисуется. */
    private fun measureBlocks(measurables: List<Measurable>, constraints: Constraints): List<List<Placeable>> {
        val blocks = ArrayList<MutableList<Placeable>>()
        var startNewBlock = true
        measurables.forEach { measurable ->
            if (measurable.layoutId == SettingsGroupGap) {
                startNewBlock = true
            } else {
                val placeable = measurable.measure(constraints)
                if (startNewBlock) {
                    blocks += mutableListOf(placeable)
                } else {
                    blocks.last() += placeable
                }
                startNewBlock = false
            }
        }
        return blocks
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SettingsDividerPreview() = SettingsPreview {
    SettingsDivider()
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SettingsDivider2Preview() = SettingsPreview {
    SettingsDivider2()
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
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
    val baseModifier = if (inGroup) SETTINGS_ITEM_IN_GROUP_BASE_MODIFIER else SETTINGS_ITEM_STANDALONE_BASE_MODIFIER
    val rowModifier = if (onClick != null) {
        modifier.then(baseModifier).clickable(onClick = onClick)
    } else {
        modifier.then(baseModifier)
    }

    val titleStyle = remember(Theme.L.Type.rowTitle) {
        Theme.L.Type.rowTitle.copy(
            color = SettingsRowTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp
        )
    }
    val subtitleStyle = remember(Theme.L.Type.rowSubtitle) {
        Theme.L.Type.rowSubtitle.copy(
            color = SettingsRowTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp
        )
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != 0) {
            SettingsIcon(icon)
            Spacer(Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = SettingsRowTextPrimary,
                style = titleStyle
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = SettingsRowTextSecondary,
                    style = subtitleStyle
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
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
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = SettingsRowTextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
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

@Preview(showBackground = true, backgroundColor = 0xFF262626)
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

@Preview(showBackground = true, backgroundColor = 0xFF262626)
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

/**
 * [SettingsButtonRow], нажатие которой сначала спрашивает подтверждение.
 *
 * Кнопка красная: подтверждением закрыты выход из профиля и очистка.
 */
@Composable
fun SettingsButtonRowWithDialog(
    @DrawableRes icon: Int = 0,
    text: String,
    value: String,
    textDialogTitle: String,
    textDialogBody: String,
    textDialogButton: String,
    subtitle: String? = null,
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
        onBlockConfirmed = onClick
    )

    SettingsButtonRow(
        icon = icon,
        text = text,
        value = value,
        subtitle = subtitle,
        color = SettingsDestructiveColor,
        onClick = onOpen
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
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

@Preview(showBackground = true, backgroundColor = 0xFF262626)
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
