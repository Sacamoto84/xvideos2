package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.collectionDB.ui.COLLECTION_GRID_ROW_SPACING
import com.client.xvideos.common.collectionDB.ui.COLLECTION_GRID_VERTICAL_PADDING
import com.client.xvideos.common.collectionDB.ui.CollectionGridCard
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountCollection
import com.client.xvideos.r.network.toRUserMessage
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.atom.AccountSavedMessage
import com.client.xvideos.r.ui.explorer.tab.saved.tab.redCollectionsGridStyle
import com.client.xvideos.r.ui.ui.lazyrow123.listLoadError
import com.client.xvideos.r.ui.ui.lazyrow123.molecule.RListLoadError
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun AccountCollectionsGrid(
    collections: LazyPagingItems<AccountCollection>,
    account: RAccountStatus.SignedIn?,
    onOpen: (AccountCollection) -> Unit,
) {
    val loadState = collections.loadState
    val failure = listLoadError(loadState)
    if (collections.itemCount == 0) {
        when {
            loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> CircularProgressIndicator()
            failure != null -> RListLoadError(message = failure.toRUserMessage(), onRetry = collections::retry)
            else -> AccountSavedMessage("У вас пока нет коллекций R")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(adaptiveGridColumns()),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = COLLECTION_GRID_VERTICAL_PADDING),
        horizontalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
        verticalArrangement = Arrangement.spacedBy(COLLECTION_GRID_ROW_SPACING),
    ) {
        items(collections.itemCount, key = collections.itemKey { it.folderId }) { index ->
            collections[index]?.let { collection ->
                CollectionGridCard(
                    collection = CollectionGridItem(collection.folderName, collection.previewUrl, collection.contentCount),
                    style = redCollectionsGridStyle,
                    onCollectionClick = { onOpen(collection) },
                    trailing = account?.let { { AccountCollectionMenu(it, collection) } },
                )
            }
        }
        when {
            failure != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                RListLoadError(message = failure.toRUserMessage(), onRetry = collections::retry)
            }
            loadState.append is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview
@Composable
private fun AccountCollectionsGridPreview() {
    val items = flowOf(PagingData.from(listOf(AccountCollection("1", "Избранное", contentCount = 12)))).collectAsLazyPagingItems()
    AccountCollectionsGrid(collections = items, account = null, onOpen = {})
}
