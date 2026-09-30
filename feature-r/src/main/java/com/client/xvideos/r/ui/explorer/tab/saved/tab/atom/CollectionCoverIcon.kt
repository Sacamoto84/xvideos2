package com.client.xvideos.r.ui.explorer.tab.saved.tab.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme

@Composable
fun CollectionCoverIcon(
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    val size = Theme.DialogLavande.iconSize
    val imageModifier = modifier
        .clip(RoundedCornerShape(8.dp))
        .size(size)
    if (coverUrl != null) {
        UrlImage(url = coverUrl, modifier = imageModifier)
    } else {
        Box(imageModifier.background(Color.Gray))
    }
}

@Preview
@Composable
private fun CollectionCoverIconPreview() {
    CollectionCoverIcon(coverUrl = null)
}
