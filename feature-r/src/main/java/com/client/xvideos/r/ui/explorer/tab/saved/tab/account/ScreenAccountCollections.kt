package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.collectionDB.ui.COLLECTION_GRID_ROW_SPACING
import com.client.xvideos.common.collectionDB.ui.COLLECTION_GRID_VERTICAL_PADDING
import com.client.xvideos.common.collectionDB.ui.CollectionGridCard
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveGridColumns
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountCollection
import com.client.xvideos.r.network.toRUserMessage
import com.client.xvideos.r.ui.explorer.tab.saved.tab.redCollectionsGridStyle
import com.client.xvideos.r.ui.ui.lazyrow123.listLoadError
import com.client.xvideos.r.ui.ui.lazyrow123.molecule.RListLoadError

internal object ScreenAccountCollections : Screen {
    override val key: ScreenKey = "RedAccountCollections"

    private fun readResolve(): Any = ScreenAccountCollections

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenAccountCollectionsSM>()
        val state by vm.state.collectAsStateWithLifecycle()
        val ready = state as? AccountSavedContent.Ready
        val collections = ready?.let { key(it) { it.value.collectAsLazyPagingItems() } }
        val account = signedInRedgifsAccount()
        var creating by remember(account) { mutableStateOf(false) }

        Column(Modifier.fillMaxSize().background(Theme.background)) {
            AccountSavedHeader(
                title = "Коллекции RedGIFs",
                onBack = { navigator.pop() },
                onRefresh = collections?.let { it::refresh },
                refreshing = collections?.loadState?.refresh is LoadState.Loading,
                actions = {
                    if (account != null) {
                        IconButton(onClick = { creating = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Создать коллекцию RedGIFs")
                        }
                    }
                },
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    state == AccountSavedContent.SignedOut -> AccountSavedMessage("Войдите в RedGIFs в настройках R")
                    collections == null -> CircularProgressIndicator()
                    else -> key(ready) {
                        AccountCollectionsGrid(collections, account) { collection ->
                            navigator.push(ScreenAccountSavedMedia(collection.folderId, collection.folderName))
                        }
                    }
                }
            }
        }
        if (creating && account != null) CreateAccountCollectionDialog(account, onDismiss = { creating = false })
    }
}

@Composable
private fun AccountCollectionsGrid(
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
            else -> AccountSavedMessage("У вас пока нет коллекций RedGIFs")
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
