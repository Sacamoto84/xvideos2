package com.client.xvideos.common.collectionDB.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.collectionDB.model.CollectionsGridStyle
import com.client.xvideos.common.theme.Theme

@Composable
fun CollectionGridCard(
    collection: CollectionGridItem,
    style: CollectionsGridStyle,
    onCollectionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCollectionLongClick: ((String) -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val handleClick = remember(collection.name, onCollectionClick) { { onCollectionClick(collection.name) } }
    val handleLongClick = remember(collection.name, onCollectionLongClick) {
        onCollectionLongClick?.let { action -> { action(collection.name) } }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = handleClick,
                onLongClick = handleLongClick,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (collection.previewUrl != null) {
            UrlImage(
                url = collection.previewUrl,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .size(72.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .size(72.dp)
                    .background(style.placeholderColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(if (trailing == null) Modifier else Modifier.weight(1f)) {
            Text(
                collection.name,
                color = style.itemNameColor,
                fontFamily = style.itemFontFamily
            )
            collection.itemsCount?.let { count ->
                Text(
                    "Элементов: $count",
                    color = style.itemSecondaryColor,
                    fontSize = 12.sp,
                    fontFamily = style.itemFontFamily
                )
            }
        }
        trailing?.invoke()
    }
}

@Preview
@Composable
private fun CollectionGridCardPreview() {
    CollectionGridCard(
        collection = CollectionGridItem("Избранное", previewUrl = null, itemsCount = 24),
        style = CollectionsGridStyle(
            backgroundColor = Theme.background,
            titleColor = Color.White,
            titleFontFamily = FontFamily.Default,
            itemNameColor = Color.White,
            itemSecondaryColor = Color.LightGray,
            itemFontFamily = FontFamily.Default,
            addButtonBackground = Theme.R.colorYellow,
        ),
        onCollectionClick = {},
    )
}
