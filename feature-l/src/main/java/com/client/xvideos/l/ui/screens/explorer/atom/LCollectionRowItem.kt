package com.client.xvideos.l.ui.screens.explorer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme

@Composable
fun LCollectionRowItem(
    name: String,
    previewUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val rowModifier = if (modifier == Modifier) {
        Modifier.fillMaxWidth().padding(4.dp).clip(shape)
    } else {
        modifier.then(Modifier.fillMaxWidth().padding(4.dp).clip(shape))
    }
    Row(
        modifier = rowModifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (previewUrl != null) {
            UrlImage(
                url = previewUrl,
                modifier = Modifier
                    .clip(shape)
                    .size(56.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .size(56.dp)
                    .background(Color(0xFF3D3949)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = name,
                    tint = Theme.DialogLavande.dismissTextColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Theme.L.fontFamilyDMsanss
        )
    }
}

@Preview
@Composable
private fun LCollectionRowItemPreview() {
    LCollectionRowItem(
        name = "Favorites",
        previewUrl = null,
        onClick = {}
    )
}
