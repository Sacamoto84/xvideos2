package com.client.xvideos.l.ui.screens.screenAlbum.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.AlbumDetails

@Composable
fun AlbumInfoFilterButton(parsed: AlbumDetails?, checked : Boolean, onCheckedChange : (Boolean)->Unit ) {

    if (parsed?.number_of_animated_pictures == 0) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            "Show only animated",
            color = Theme.L.textColor,
            style = Theme.L.Type.rowTitle
        )
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
            checked,
                onCheckedChange = { onCheckedChange(it) },
        )
        Spacer(modifier = Modifier.width(4.dp))
    }
}
