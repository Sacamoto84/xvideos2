package com.client.xvideos.xlive.ui.root.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.xlive.model.XLiveGender
import com.client.xvideos.xlive.ui.root.atom.XLiveGenderIcon

/**
 * Переключатель пола у нижних вкладок — тот же вид, что у переключателя
 * категории X: кнопка со значком текущего пола, по нажатию — меню, по долгому
 * нажатию — подсказка, что это за кнопка.
 *
 * @param current выбранный пол.
 * @param onSelect выбор пункта меню, в том числе текущего.
 */
@Composable
fun XLiveGenderSwitch(
    current: XLiveGender,
    onSelect: (XLiveGender) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        HintTooltip(text = "Пол комнат: ${current.title}") {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                // Приглушён до яркости неактивных вкладок, как у X: кнопку жмут редко.
                XLiveGenderIcon(gender = current, modifier = Modifier.alpha(ICON_ALPHA))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF2A2A30),
        ) {
            XLiveGender.entries.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(text = gender.title, color = Color.White) },
                    leadingIcon = { XLiveGenderIcon(gender = gender) },
                    onClick = {
                        expanded = false
                        onSelect(gender)
                    },
                    modifier = if (gender == current) Modifier.background(Color(0xFF3A3A42)) else Modifier,
                )
            }
        }
    }
}

private const val ICON_ALPHA = 0.5f

@Preview
@Composable
private fun XLiveGenderSwitchPreview() {
    XLiveGenderSwitch(current = XLiveGender.FEMALE, onSelect = {})
}
