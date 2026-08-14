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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.URL1
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun DialogCollection(
    visible: Boolean,
    onDismiss: () -> Unit,
    onClickNewCollection: () -> Unit,
    onSelectCollection: (String) -> Unit = {},
    savedRed: () -> SavedRed
) {
    if (!visible) return

    LavenderDialog(
        title = "Добавить в коллекцию",
        onDismiss = onDismiss,
        content = {
            CollectionListContent(
                collectionList = savedRed().collections.collectionList,
                onSelectCollection = onSelectCollection
            )
        },
        confirmText = "Создать",
        onConfirm = {
            onClickNewCollection()
            onDismiss()
        },
    )
}

@Composable
private fun ColumnScope.CollectionListContent(
    collectionList: List<CollectionEntity<GifsInfo>>,
    onSelectCollection: (String) -> Unit,
) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 420.dp)
    ) {
        items(collectionList, key = { it.collection }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 4.dp)
                    .clickable(onClick = { onSelectCollection(item.collection) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.items.isNotEmpty()) {
                    UrlImage(
                        url = item.items.last().urls.thumbnail,
                        modifier = Modifier.clip(RoundedCornerShape(25)).size(72.dp)
                    )
                } else {
                    Box(Modifier.clip(RoundedCornerShape(25)).size(72.dp).background(Color.Gray))
                }
                Spacer(Modifier.width(8.dp))
                Text(item.collection, color = Color.Black, fontFamily = Theme.R.fontFamilyDMsanss)
            }
        }
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
