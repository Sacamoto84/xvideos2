package com.client.xvideos.screenSettings

import com.client.xvideos.common.util.defaultSharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.element.SettingElementList
import com.skydoves.compose.stability.runtime.TraceRecomposition

import androidx.compose.runtime.key

private val ROW_HORIZONTAL_PADDING = 8.dp
private val ROW_VERTICAL_PADDING = 2.dp
private val ROW_HEIGHT = 48.dp
private val LABEL_WIDTH = 64.dp
private val SEGMENT_START_PADDING = 16.dp
private val TAB_BAR_SIZE = 24.dp
private val POINT_SPACING = 2.dp
private val POINT_SIZE = 4.dp
private val POINT_ACTIVE_COLOR = Color.White
private val POINT_INACTIVE_COLOR = Color.Gray
private val POINT_SHAPE = CircleShape
private val LABEL_TEXT_MODIFIER = Modifier.width(LABEL_WIDTH)
private val SEGMENT_ROW_MODIFIER = Modifier.padding(start = SEGMENT_START_PADDING).fillMaxWidth()
private val POINT_BASE_MODIFIER = Modifier
    .padding(end = POINT_SPACING)
    .clip(POINT_SHAPE)
    .size(POINT_SIZE)

private val CONFIG_ROW_BASE_MODIFIER = Modifier
    .padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = ROW_VERTICAL_PADDING)
    .height(ROW_HEIGHT)
    .fillMaxWidth()

private val ROW_HORIZONTAL_ARRANGEMENT = Arrangement.SpaceBetween
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val BOX_ALIGNMENT_CENTER = Alignment.Center

@Composable
fun Config_G_0_4(
    text: String = "123453232",
    setting: SettingElementList<Boolean>,
    modifier: Modifier = Modifier,
) {

    val list by setting.field.collectAsStateWithLifecycle()
    val visibleIndices = remember(list) { list.indices.filter { it in 1..4 } }

    Row(
        modifier = modifier.then(CONFIG_ROW_BASE_MODIFIER),
        horizontalArrangement = ROW_HORIZONTAL_ARRANGEMENT,
        verticalAlignment = ROW_VERTICAL_ALIGNMENT
    ) {
        Text(text, modifier = LABEL_TEXT_MODIFIER, style = styleTextConfig)

        val onToggleIndex: (Int) -> Unit = remember(setting, list) {
            { settingIndex ->
                val updatedList = list.toMutableList()
                updatedList[settingIndex] = updatedList[settingIndex].not()
                setting.setValue(updatedList)
            }
        }

        MultiChoiceSegmentedButtonRow(
            modifier = SEGMENT_ROW_MODIFIER
        ) {
            visibleIndices.forEachIndexed { buttonIndex, settingIndex ->
                key(settingIndex) {
                    val isChecked = list[settingIndex]
                    val handleCheckedChange: (Boolean) -> Unit = remember(onToggleIndex, settingIndex) {
                        { _ -> onToggleIndex(settingIndex) }
                    }
                    val labelContent: @Composable () -> Unit = remember(settingIndex, isChecked) {
                        { TabBarPoints(settingIndex, isChecked) }
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = buttonIndex,
                            count = visibleIndices.size
                        ),
                        checked = isChecked,
                        onCheckedChange = handleCheckedChange,
                        label = labelContent
                    )
                }
            }
        }

    }

}


@Composable
private fun TabBarPoints(
    count: Int,
    screenType: Boolean,
    modifier: Modifier = Modifier,
) {
    val safeCount = count.takeIf { it in 1..4 } ?: 2
    val pointColor = if (screenType) POINT_ACTIVE_COLOR else POINT_INACTIVE_COLOR
    Box(
        modifier = modifier.size(TAB_BAR_SIZE),
        contentAlignment = BOX_ALIGNMENT_CENTER
    ) {
        Row {
            repeat(safeCount) {
                Box(
                    modifier = POINT_BASE_MODIFIER.background(pointColor)
                )
            }
        }
    }
}


@TraceRecomposition
@Preview(showBackground = false)
@Composable
fun PreviewConfig_G_0_4() {
    // We create a local instance of SettingElementList for the preview to avoid 
    // UninitializedPropertyAccessException from Settings.pref
    val context = LocalContext.current
    val setting = remember {
        SettingElementList<Boolean>(
            sharedPrefs = context.defaultSharedPreferences(),
            name = "l_likesTab_G_0_4",
            default = listOf(false, true, true, true, true)
        )
    }
    Config_G_0_4("777", setting)
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun TabBarPointsPreview() {
    Row {
        TabBarPoints(count = 2, screenType = true)
        TabBarPoints(count = 3, screenType = false)
        TabBarPoints(count = 4, screenType = true)
    }
}
