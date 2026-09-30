package com.client.xvideos.r.ui.manager_block.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.GifsInfo

@Composable
fun BlockedUserRow(
    item: GifsInfo,
    onUnblock: (GifsInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleUnblock = remember(item, onUnblock) { { onUnblock(item) } }
    Row(
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .fillMaxWidth()
            .height(128.dp)
            .background(Color.Transparent)
            .border(1.dp, Theme.R.colorBorderGray, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UrlImage(
            item.urls.thumbnail,
            modifier = Modifier.aspectRatio(1f),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = item.userName,
                color = Theme.Text.primary,
                fontSize = 16.sp,
                fontFamily = Theme.R.fontFamilyPopinsMedium
            )
            Text(
                text = item.id,
                color = Theme.Text.secondary,
                fontSize = 13.sp,
                fontFamily = Theme.R.fontFamilyDMsanss
            )
        }

        IconButton(
            onClick = handleUnblock,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Разблокировать",
                tint = Color(0xFFFF7A7A)
            )
        }
    }
}

@Preview
@Composable
private fun BlockedUserRowPreview() {
    BlockedUserRow(
        item = GifsInfo(
            id = "gif_123",
            userName = "BlockedCreator"
        ),
        onUnblock = {}
    )
}
