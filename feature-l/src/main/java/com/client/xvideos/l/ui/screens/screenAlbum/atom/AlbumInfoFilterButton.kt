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

private val FILTER_SPACER_WIDTH = 4.dp
private const val LABEL_SHOW_ONLY_ANIMATED = "Show only animated"

@Composable
fun AlbumInfoFilterButton(
    parsed: AlbumDetails?,
    checked: Boolean,
    hasAnimatedItems: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if ((parsed?.number_of_animated_pictures ?: 0) <= 0 && !hasAnimatedItems) return

    val titleStyle = remember(Theme.L.Type.rowTitle) { Theme.L.Type.rowTitle }
    val textColor = remember(Theme.L.textColor) { Theme.L.textColor }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = LABEL_SHOW_ONLY_ANIMATED,
            color = textColor,
            style = titleStyle
        )
        Spacer(modifier = Modifier.width(FILTER_SPACER_WIDTH))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(modifier = Modifier.width(FILTER_SPACER_WIDTH))
    }
}
