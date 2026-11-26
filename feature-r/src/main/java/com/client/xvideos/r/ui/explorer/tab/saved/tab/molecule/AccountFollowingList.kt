package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.network.toRUserMessage
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.atom.AccountSavedMessage
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountMembershipMenu
import com.client.xvideos.r.ui.ui.lazyrow123.listLoadError
import com.client.xvideos.r.ui.ui.lazyrow123.molecule.RListLoadError
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun AccountFollowingList(creators: LazyPagingItems<UserInfo>, onOpenProfile: (String) -> Unit) {
    val loadState = creators.loadState
    val failure = listLoadError(loadState)
    if (creators.itemCount == 0) {
        when {
            loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> CircularProgressIndicator()
            failure != null -> RListLoadError(message = failure.toRUserMessage(), onRetry = creators::retry)
            else -> AccountSavedMessage("У вас пока нет подписок на авторов R")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(Theme.Content.itemSpacing),
    ) {
        items(creators.itemCount, key = creators.itemKey { it.username }) { index ->
            creators[index]?.let { creator ->
                CreatorListItem(
                    item = creator,
                    onClick = onOpenProfile,
                    modifier = Modifier.adaptiveContentWidth(),
                    trailing = { AccountMembershipMenu(AccountMembership.AUTHOR, creator.username, knownMember = true) },
                )
            }
        }
        when {
            failure != null -> item { RListLoadError(message = failure.toRUserMessage(), onRetry = creators::retry) }
            loadState.append is LoadState.Loading -> item {
                Box(Modifier.fillParentMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview
@Composable
private fun AccountFollowingListPreview() {
    val items = flowOf(PagingData.from(listOf(UserInfo(username = "sample_creator", name = "Sample")))).collectAsLazyPagingItems()
    AccountFollowingList(creators = items, onOpenProfile = {})
}
