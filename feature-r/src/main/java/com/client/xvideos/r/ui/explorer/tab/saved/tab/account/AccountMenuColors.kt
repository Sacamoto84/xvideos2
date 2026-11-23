package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.client.xvideos.common.theme.Theme

@Composable
internal fun accountMenuItemColors(
    containerColor: Color = Theme.ExpandMenu.backgroundColor,
): MenuItemColors {
    val lightBackground = containerColor.luminance() > 0.4f
    val active = if (lightBackground) Color(0xFF1B5C9E) else Theme.R.colorBlue
    val disabled = (if (lightBackground) Theme.ExpandMenu.tintColor else Color.White).copy(alpha = 0.38f)
    return MenuDefaults.itemColors(
        textColor = active,
        leadingIconColor = active,
        disabledTextColor = disabled,
        disabledLeadingIconColor = disabled,
    )
}
