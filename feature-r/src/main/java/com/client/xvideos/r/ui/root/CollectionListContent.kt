package com.client.xvideos.r.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.URL1

@Composable
internal fun ColumnScope.CollectionListContent(
    collectionList: List<CollectionEntity<GifsInfo>>,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (collectionList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет коллекций",
                color = Theme.DialogLavande.bodyColor,
                fontFamily = Theme.R.fontFamilyDMsanss,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 420.dp)
        ) {
            items(
                items = collectionList,
                key = { it.collection },
                contentType = { "collection_item" }
            ) { item ->
                CollectionRowItem(
                    item = item,
                    onSelectCollection = onSelectCollection
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEBE6EE)
@Composable
private fun CollectionListContentPreview() {
    Column(Modifier.padding(16.dp)) {
        CollectionListContent(
            collectionList = listOf(
                CollectionEntity(
                    collection = "Favorites",
                    items = listOf(GifsInfo(id = "trtt", urls = URL1(thumbnail = "")))
                ),
                CollectionEntity(
                    collection = "Funny",
                    items = emptyList()
                )
            ),
            onSelectCollection = {}
        )
    }
}
