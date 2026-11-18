package com.client.xvideos.n.ui.molecule

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
import com.client.xvideos.common.ui.HintTooltip
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Кнопка выпадающего меню сортировки (аналог LSortMenuButton из раздела L).
 */
@Composable
fun NSortMenuButton(
    options: ImmutableList<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        HintTooltip(text = contentDescription) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = { expanded = true })
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = options.getOrElse(selectedIndex) { "" },
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Theme.tabLevel1
        ) {
            options.forEachIndexed { index, title ->
                key(index) {
                    val isSelected = index == selectedIndex
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = title,
                                color = if (isSelected) Color(0xFFED2553) else Color.White,
                                fontSize = 14.sp
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

@Preview
@Composable
private fun NSortMenuButtonPreview() {
    NSortMenuButton(
        options = persistentListOf("Сначала новые", "Сначала старые", "По названию"),
        selectedIndex = 0,
        onSelectIndex = {},
        contentDescription = "Сортировка"
    )
}
