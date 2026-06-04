package com.client.xvideos.common.settings.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.json.JsonTypes
import com.client.xvideos.common.settings.element.SettingElementList
import com.composeunstyled.Text
import com.skydoves.compose.stability.runtime.TraceRecomposition

@Composable
fun Config_G_0_4(text: String = "123453232", setting : SettingElementList<Boolean>) {

    val list =  setting.field.collectAsStateWithLifecycle().value
    val visibleIndices = remember(list) { list.indices.filter { it in 1..4 } }

    //val selectedOptions = remember { mutableStateListOf(false, false, true, true, false) }

    Row(
        modifier = Modifier.padding(horizontal = 8.dp).padding(vertical = 2.dp).height(48.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, modifier = Modifier.width(64.dp), style = styleTextConfig)

        MultiChoiceSegmentedButtonRow(
            modifier = Modifier.padding(start = 16.dp).fillMaxWidth()
        ) {
            visibleIndices.forEachIndexed { buttonIndex, settingIndex ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = buttonIndex,
                        count = visibleIndices.size
                    ),
                    checked = list[settingIndex],
                    onCheckedChange = {
                        val a = list.toMutableList()
                        a[settingIndex] = a[settingIndex].not()
                        setting.setValue(a)
                    },

                    //icon = { SegmentedButtonDefaults.Icon(selectedOptions[index]) },

                    label = {
                        TabBarPoints(settingIndex, list[settingIndex])
                    }
                )
            }
        }

    }


}


@Composable
private fun TabBarPoints(count: Int, screenType: Boolean) {
    val safeCount = count.takeIf { it in 1..4 } ?: 2
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row {
            repeat(safeCount) {
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .clip(CircleShape)
                        .size(4.dp)
                        .background(if (screenType) Color.White else Color.Gray)
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
            typeToken = JsonTypes.listOf(Boolean::class.javaObjectType),
            default = listOf(false, true, true, true, true)
        )
    }
    Config_G_0_4("777", setting)
}
