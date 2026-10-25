package com.client.xvideos.xlive.ui.search.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.ui.XLiveColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val ChipShape = RoundedCornerShape(16.dp)
private const val ACTIVE_BACKGROUND_ALPHA = 0.18f

/**
 * Фильтр поиска: чип «Название: значение» с меню вариантов. Выбранный фильтр —
 * в цвете раздела, «любой» — серый. Первый вариант в [options] — «без отбора».
 *
 * Открыто ли меню — состояние самого чипа: после ухода с экрана оно не нужно.
 *
 * @param selected номер выбранного варианта в [options].
 */
@Composable
fun XLiveFilterChip(
    label: String,
    options: ImmutableList<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = selected != 0

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(ChipShape)
                .background(if (active) XLiveColors.Accent.copy(alpha = ACTIVE_BACKGROUND_ALPHA) else XLiveColors.Pill)
                .border(1.dp, if (active) XLiveColors.Accent else XLiveColors.ChipBorder, ChipShape)
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Text(
                text = "$label: ${options.getOrElse(selected) { "" }}",
                color = if (active) Color.White else XLiveColors.PillContent,
                fontSize = 13.sp,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = XLiveColors.PillContent,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, title ->
                DropdownMenuItem(
                    text = {
                        Text(text = title, fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun XLiveFilterChipPreview() {
    XLiveFilterChip(label = "Возраст", options = persistentListOf("Любой", "18–21"), selected = 1, onSelect = {})
}
