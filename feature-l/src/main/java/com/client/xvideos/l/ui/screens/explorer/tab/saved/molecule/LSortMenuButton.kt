package com.client.xvideos.l.ui.screens.explorer.tab.saved.molecule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Сортировка в L: значок и выбранный порядок — одна кнопка, меню открывает
 * нажатие и на значок, и на текст. Поле 12dp вокруг значка — при отступе ряда
 * 4dp значок на 16dp от края, как стрелка «назад» в шапках.
 *
 * @param options подписи порядков в меню; выбран [selectedIndex].
 */
@Composable
fun LSortMenuButton(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = { expanded = true })
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = contentDescription,
                tint = Theme.L.textColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                options.getOrElse(selectedIndex) { "" },
                color = Theme.L.grey2,
                fontSize = 16.sp,
                fontFamily = Theme.L.fontFamilyDMsanss,
                maxLines = 1
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Theme.L.grey3
        ) {
            val baseStyle = Theme.L.Type.menuItem
            options.forEachIndexed { index, title ->
                key(title) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                title,
                                style = baseStyle.copy(color = if (index == selectedIndex) Color.White else Theme.L.grey2)
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelectIndex(index)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun LSortMenuButtonPreview() {
    LSortMenuButton(
        options = persistentListOf("Сначала новые", "Сначала старые"),
        selectedIndex = 0,
        onSelectIndex = {},
        contentDescription = "Порядок"
    )
}
