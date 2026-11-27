package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountCollection
import com.client.xvideos.r.network.api.AccountCollectionsApi
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedOperation
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.accountActionMessage
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.accountCollectionsPager
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.accountMenuItemColors
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.collectionItems
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.collectionsKey
import com.client.xvideos.r.ui.ui.lazyrow123.listLoadError
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun AddToAccountCollectionDialog(
    account: RAccountStatus.SignedIn,
    gifId: String,
    redApi: RedApi?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val pagerFlow = remember(redApi, account, isPreview) {
        if (isPreview || redApi == null) flowOf(PagingData.from(listOf(AccountCollection("1", "Избранное"))))
        else accountCollectionsPager(redApi, account).flow
    }
    val items = pagerFlow.collectAsLazyPagingItems()
    val pending by AccountSavedActions.pending.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<AccountCollection?>(null) }
    val busy = selection != null
    val menuColors = accountMenuItemColors(AlertDialogDefaults.containerColor)

    fun add(collection: AccountCollection) {
        selection = collection
        val list = collectionItems(collection.folderId)
        AccountSavedActions.run(
            context, AccountSavedOperation(account, list), list, collectionsKey,
            successMessage = "Добавлено в «${collection.folderName}»",
            request = { AccountCollectionsApi.setItem(account, collection.folderId, gifId, added = true) },
            onResult = {
                selection = null
                if (it.isSuccess) onDismiss()
            },
        )
    }

    if (creating) {
        CreateAccountCollectionDialog(account, onDismiss = { creating = false }) { collection ->
            items.refresh()
            add(collection)
        }
        return
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("В коллекцию R") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item {
                    DropdownMenuItem(
                        text = { Text("Новая коллекция") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        colors = menuColors,
                        enabled = !busy,
                        onClick = { creating = true },
                    )
                }
                items(items.itemCount, key = items.itemKey { it.folderId }) { index ->
                    items[index]?.let { collection ->
                        val operation = AccountSavedOperation(account, collectionItems(collection.folderId))
                        DropdownMenuItem(
                            text = { Text(collection.folderName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            colors = menuColors,
                            enabled = !busy && operation !in pending,
                            onClick = { add(collection) },
                        )
                    }
                }
                val failure = listLoadError(items.loadState)
                if (failure != null) item {
                    Text(failure.accountActionMessage())
                    TextButton(enabled = !busy, onClick = items::retry) { Text("Повторить") }
                }
                if (items.loadState.refresh is LoadState.Loading || items.loadState.append is LoadState.Loading || busy) {
                    item {
                        Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Preview
@Composable
private fun AddToAccountCollectionDialogPreview() {
    AddToAccountCollectionDialog(
        account = RAccountStatus.SignedIn("sample", "preview"),
        gifId = "sample_id",
        redApi = null,
        onDismiss = {},
    )
}
