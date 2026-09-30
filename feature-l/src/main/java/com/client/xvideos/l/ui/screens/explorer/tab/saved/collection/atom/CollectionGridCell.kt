package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

private val CELL_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(horizontal = 8.dp, vertical = 4.dp)

@Composable
fun CollectionGridCell(
    collection: CollectionGridItem,
    style: CollectionsGridStyle,
    shape: RoundedCornerShape,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleClick = remember(collection.name, onClick) { { onClick(collection.name) } }
    val handleLongClick = remember(collection.name, onLongClick) { { onLongClick(collection.name) } }
    val countText = remember(collection.itemsCount) {
        collection.itemsCount?.let { "Элементов: $it" }
    }
    val rowModifier = if (modifier == Modifier) {
        CELL_BASE_MODIFIER
    } else {
        modifier.then(CELL_BASE_MODIFIER)
    }.combinedClickable(
        onClick = handleClick,
        onLongClick = handleLongClick
    )
    val previewModifier = Modifier
        .clip(shape)
        .size(72.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val previewUrl = collection.previewUrl
        if (previewUrl != null) {
            UrlImage(
                url = previewUrl,
                modifier = previewModifier
            )
        } else {
            Box(
                modifier = previewModifier
                    .background(style.placeholderColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                collection.name,
                color = style.itemNameColor,
                fontFamily = style.itemFontFamily
            )
            if (countText != null) {
                Text(
                    countText,
                    color = style.itemSecondaryColor,
                    fontSize = 12.sp,
                    fontFamily = style.itemFontFamily
                )
            }
        }
    }
}

@Preview
@Composable
private fun CollectionGridCellPreview() {
    CollectionGridCell(
        collection = CollectionGridItem("Favorites", null, 12),
        style = CollectionsGridStyle(
            backgroundColor = Color(0xFF262626),
            titleColor = Color.White,
            titleFontFamily = FontFamily.Default,
            itemNameColor = Color.White,
            itemSecondaryColor = Color(0xFFB0B0B0),
            itemFontFamily = FontFamily.Default,
            addButtonBackground = Color(0xFF3A3A3A),
            addButtonIconColor = Color.White,
            placeholderColor = Color(0xFF4A4A4A),
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = {},
        onLongClick = {}
    )
}
