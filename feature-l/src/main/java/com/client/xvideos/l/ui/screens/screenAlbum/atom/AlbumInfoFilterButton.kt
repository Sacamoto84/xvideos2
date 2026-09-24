package com.client.xvideos.l.ui.screens.screenAlbum.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.AlbumDetails

@Composable
fun AlbumInfoFilterButton(
    parsed: AlbumDetails?,
    checked: Boolean,
    hasAnimatedItems: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    if ((parsed?.number_of_animated_pictures ?: 0) <= 0 && !hasAnimatedItems) return

    val titleStyle = remember { Theme.L.Type.rowTitle }
    val textColor = remember { Theme.L.textColor }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Show only animated",
            color = textColor,
            style = titleStyle
        )
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(modifier = Modifier.width(4.dp))
    }
}
