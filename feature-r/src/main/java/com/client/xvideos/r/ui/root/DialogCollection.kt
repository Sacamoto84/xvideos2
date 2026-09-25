package com.client.xvideos.r.ui.root

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.URL1
import com.client.xvideos.ui.theme.XvideosTheme

private val COLLECTION_ITEM_SHAPE = RoundedCornerShape(12.dp)
private val COLLECTION_THUMBNAIL_SIZE = 56.dp
private val FOLDER_ICON_SIZE = 28.dp
private val FOLDER_PLACEHOLDER_BG = Color(0xFF3D3949)
private val ITEM_OUTER_PADDING = 4.dp
private val ITEM_INNER_HORIZONTAL_PADDING = 8.dp
private val ITEM_INNER_VERTICAL_PADDING = 6.dp
private val ITEM_SPACER_WIDTH = 12.dp
private val EMPTY_BOX_VERTICAL_PADDING = 32.dp
private val LAZY_COLUMN_MIN_HEIGHT = 120.dp
private val LAZY_COLUMN_MAX_HEIGHT = 420.dp

private val EMPTY_BOX_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(vertical = EMPTY_BOX_VERTICAL_PADDING)
private val LAZY_COLUMN_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .heightIn(min = LAZY_COLUMN_MIN_HEIGHT, max = LAZY_COLUMN_MAX_HEIGHT)
private val ITEM_ROW_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .padding(ITEM_OUTER_PADDING)
    .clip(COLLECTION_ITEM_SHAPE)
private val ITEM_ROW_CONTENT_PADDING_MODIFIER = Modifier
    .padding(horizontal = ITEM_INNER_HORIZONTAL_PADDING, vertical = ITEM_INNER_VERTICAL_PADDING)
private val THUMBNAIL_BASE_MODIFIER = Modifier
    .clip(COLLECTION_ITEM_SHAPE)
    .size(COLLECTION_THUMBNAIL_SIZE)
private val FOLDER_PLACEHOLDER_BASE_MODIFIER = Modifier
    .clip(COLLECTION_ITEM_SHAPE)
    .size(COLLECTION_THUMBNAIL_SIZE)
    .background(FOLDER_PLACEHOLDER_BG)
private val FOLDER_ICON_MODIFIER = Modifier.size(FOLDER_ICON_SIZE)
private val ITEM_SPACER_MODIFIER = Modifier.width(ITEM_SPACER_WIDTH)

private const val TEXT_ADD_TO_COLLECTION = "Добавить в коллекцию"
private const val TEXT_NO_COLLECTIONS = "Нет коллекций"
private const val TEXT_CREATE = "Создать"
private const val CONTENT_TYPE_COLLECTION_ITEM = "collection_item"

@Composable
fun DialogCollection(
    visible: Boolean,
    onDismiss: () -> Unit,
    onClickNewCollection: () -> Unit,
    onSelectCollection: (String) -> Unit = {},
    savedRed: () -> SavedRed
) {
    if (!visible) return

    val onConfirmCreate = remember(onClickNewCollection, onDismiss) {
        {
            onClickNewCollection()
            onDismiss()
        }
    }

    LavenderDialog(
        title = TEXT_ADD_TO_COLLECTION,
        onDismiss = onDismiss,
        content = {
            CollectionListContent(
                collectionList = savedRed().collections.collectionList,
                onSelectCollection = onSelectCollection
            )
        },
        confirmText = TEXT_CREATE,
        onConfirm = onConfirmCreate,
    )
}

@Composable
private fun ColumnScope.CollectionListContent(
    collectionList: List<CollectionEntity<GifsInfo>>,
    onSelectCollection: (String) -> Unit,
) {
    if (collectionList.isEmpty()) {
        Box(
            modifier = EMPTY_BOX_BASE_MODIFIER,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = TEXT_NO_COLLECTIONS,
                color = Theme.DialogLavande.bodyColor,
                fontFamily = Theme.R.fontFamilyDMsanss,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = LAZY_COLUMN_BASE_MODIFIER
        ) {
            items(
                items = collectionList,
                key = { it.collection },
                contentType = { CONTENT_TYPE_COLLECTION_ITEM }
            ) { item ->
                CollectionRowItem(
                    item = item,
                    onSelectCollection = onSelectCollection
                )
            }
        }
    }
}

@Composable
private fun CollectionRowItem(
    item: CollectionEntity<GifsInfo>,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = remember(item.collection, onSelectCollection) {
        { onSelectCollection(item.collection) }
    }
    Row(
        modifier = modifier
            .then(ITEM_ROW_BASE_MODIFIER)
            .clickable(onClick = onClick)
            .then(ITEM_ROW_CONTENT_PADDING_MODIFIER),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.items.isNotEmpty()) {
            UrlImage(
                url = item.items.last().urls.thumbnail,
                modifier = THUMBNAIL_BASE_MODIFIER
            )
        } else {
            Box(
                modifier = FOLDER_PLACEHOLDER_BASE_MODIFIER,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = item.collection,
                    tint = Theme.DialogLavande.dismissTextColor,
                    modifier = FOLDER_ICON_MODIFIER
                )
            }
        }
        Spacer(ITEM_SPACER_MODIFIER)
        Text(
            text = item.collection,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Theme.R.fontFamilyDMsanss
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEBE6EE)
@Composable
private fun DialogCollectionPreview() {
    XvideosTheme {
        Column(Modifier.padding(16.dp)) {
            CollectionListContent(
                collectionList = listOf(
                    CollectionEntity(
                        collection = "Favorites",
                        items = listOf(GifsInfo(id = "trtt", urls = URL1(thumbnail = "")))
                    ),
                    CollectionEntity(
                        collection = "Funny",
                        items = emptyList<GifsInfo>()
                    )
                ),
                onSelectCollection = {}
            )
        }
    }
}
