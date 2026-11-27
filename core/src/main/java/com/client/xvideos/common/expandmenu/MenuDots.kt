package com.client.xvideos.common.expandmenu

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Три точки с чёрной подложкой — читаются и на светлой картинке. */
@Composable
internal fun MenuDots() {
    Icon(
        Icons.Default.MoreVert,
        contentDescription = null,
        tint = Color.Black,
        modifier = Modifier.size(24.dp).offset(0.5.dp, 0.5.dp)
    )
    Icon(
        Icons.Default.MoreVert,
        contentDescription = "Действия",
        tint = Color.White,
        modifier = Modifier.size(24.dp)
    )
}

@Preview
@Composable
private fun MenuDotsPreview() {
    MenuDots()
}
