package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountCollection
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountCollectionsGrid
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountSavedHeader
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountSignInPrompt
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.CreateAccountCollectionDialog
import kotlinx.coroutines.flow.Flow

internal object ScreenAccountCollections : Screen {
    override val key: ScreenKey = "RedAccountCollections"

    private fun readResolve(): Any = ScreenAccountCollections

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenAccountCollectionsSM>()
        val state by vm.state.collectAsStateWithLifecycle()
        val ready = state as? AccountSavedContent.Ready<Flow<PagingData<AccountCollection>>>
        val collections = ready?.let { key(it) { it.value.collectAsLazyPagingItems() } }
        val account = signedInAccount()
        var creating by remember(account) { mutableStateOf(false) }

        ScreenAccountCollectionsContent(
            signedOut = state == AccountSavedContent.SignedOut,
            collections = collections,
            account = account,
            onBack = { navigator.pop() },
            onCreateClick = { creating = true },
            onOpenCollection = { collection ->
                navigator.push(ScreenAccountSavedMedia(collection.folderId, collection.folderName))
            },
        )
        if (creating && account != null) {
            CreateAccountCollectionDialog(account, onDismiss = { creating = false })
        }
    }
}

@Composable
internal fun ScreenAccountCollectionsContent(
    signedOut: Boolean,
    collections: LazyPagingItems<AccountCollection>?,
    account: RAccountStatus.SignedIn?,
    onBack: () -> Unit,
    onCreateClick: () -> Unit,
    onOpenCollection: (AccountCollection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(Theme.background)) {
        AccountSavedHeader(
            title = "Коллекции R",
            onBack = onBack,
            onRefresh = collections?.let { it::refresh },
            refreshing = collections?.loadState?.refresh is LoadState.Loading,
            actions = {
                if (account != null) {
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Default.Add, contentDescription = "Создать коллекцию R")
                    }
                }
            },
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                signedOut -> AccountSignInPrompt()
                collections == null -> CircularProgressIndicator()
                else -> key(collections) {
                    AccountCollectionsGrid(collections, account, onOpenCollection)
                }
            }
        }
    }
}

@Preview
@Composable
private fun ScreenAccountCollectionsPreview() {
    ScreenAccountCollectionsContent(
        signedOut = true,
        collections = null,
        account = null,
        onBack = {},
        onCreateClick = {},
        onOpenCollection = {},
    )
}
