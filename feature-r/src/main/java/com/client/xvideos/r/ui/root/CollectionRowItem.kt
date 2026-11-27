package com.client.xvideos.r.ui.root

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.GifsInfo

@Composable
internal fun CollectionRowItem(
    item: CollectionEntity<GifsInfo>,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(item.collection, onSelectCollection) {
        { onSelectCollection(item.collection) }
    }
    val itemTextStyle = remember(Theme.R.fontFamilyDMsanss) {
        TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Theme.R.fontFamilyDMsanss
        )
    }
    val itemShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(itemShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.items.isNotEmpty()) {
            UrlImage(
                url = item.items.last().urls.thumbnail,
                modifier = Modifier
                    .clip(itemShape)
                    .size(56.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(itemShape)
                    .size(56.dp)
                    .background(Color(0xFF3D3949)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = item.collection,
                    tint = Theme.DialogLavande.dismissTextColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.collection,
            style = itemTextStyle
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionRowItemPreview() {
    CollectionRowItem(
        item = CollectionEntity(collection = "Favorites", items = emptyList()),
        onSelectCollection = {}
    )
}
