package com.client.xvideos.common.expandmenu

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.client.xvideos.common.theme.Theme

/**
 * Пункт выпадающего меню элемента: иконка слева, подпись, отступы как у остальных пунктов.
 *
 * Оформление пункта (tint иконки, стиль текста, contentPadding) повторялось в каждом
 * DropdownMenuItem_* в L и R — здесь оно задано один раз, а конкретные пункты
 * описывают только иконку, подпись и своё действие.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandMenuActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        leadingIcon = { Icon(icon, contentDescription = "", tint = Theme.ExpandMenu.tintColor) },
        text = { Text(text, style = Theme.ExpandMenu.style) },
        onClick = onClick,
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}
