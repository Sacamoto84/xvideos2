package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.network.toRUserMessage
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountMembershipMenu
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSignInPrompt
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.CreatorListItem
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.listLoadError
import com.client.xvideos.r.ui.ui.lazyrow123.molecule.RListLoadError

internal object ScreenAccountFollowing : Screen {
    override val key: ScreenKey = "RedAccountFollowing"

    private fun readResolve(): Any = ScreenAccountFollowing

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenAccountFollowingSM>()
        val state by vm.state.collectAsStateWithLifecycle()
        val ready = state as? AccountFollowingState.Ready
        val creators = ready?.let { key(it) { it.creators.collectAsLazyPagingItems() } }
        val onOpenProfile: (String) -> Unit = remember(navigator) { { navigator.push(ScreenRedProfile(it)) } }

        Column(Modifier.fillMaxSize().background(Theme.background)) {
            SavedTabHeader(
                title = "Подписки RedGIFs",
                topCutout = getTopInsetDp(),
                navigation = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                trailing = {
                    if (creators != null) {
                        IconButton(onClick = creators::refresh, enabled = creators.loadState.refresh !is LoadState.Loading) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Обновить список авторов")
                        }
                    }
                },
            )

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state == AccountFollowingState.SignedOut -> AccountSignInPrompt()
                    creators == null -> CircularProgressIndicator()
                    else -> key(ready) { AccountFollowingList(creators, onOpenProfile) }
                }
            }
        }
    }
}

@Composable
private fun AccountFollowingList(creators: LazyPagingItems<UserInfo>, onOpenProfile: (String) -> Unit) {
    val loadState = creators.loadState
    val failure = listLoadError(loadState)
    if (creators.itemCount == 0) {
        when {
            loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> CircularProgressIndicator()
            failure != null -> RListLoadError(message = failure.toRUserMessage(), onRetry = creators::retry)
            else -> FollowingMessage("У вас пока нет подписок на авторов RedGIFs")
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

@Composable
private fun FollowingMessage(text: String) {
    Text(text, modifier = Modifier.padding(horizontal = 20.dp), color = Color.White, textAlign = TextAlign.Center)
}
