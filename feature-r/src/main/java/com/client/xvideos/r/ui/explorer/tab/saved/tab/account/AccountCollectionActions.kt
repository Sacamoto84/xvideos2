package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountCollection
import com.client.xvideos.r.network.api.AccountCollectionsApi
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.SavedItemActionsMenu
import com.client.xvideos.r.ui.ui.lazyrow123.listLoadError

private val collections = AccountSavedKey(AccountSavedList.COLLECTIONS)

internal fun collectionItems(id: String) = AccountSavedKey(AccountSavedList.COLLECTION_ITEMS, id)

@Composable
internal fun AccountCollectionMenu(account: RAccountStatus.SignedIn, collection: AccountCollection) {
    val context = LocalContext.current
    val operation = AccountSavedOperation(account, collectionItems(collection.folderId))
    val pending by AccountSavedActions.pending.collectAsStateWithLifecycle()
    val busy = operation in pending
    var rename by remember(collection.folderId) { mutableStateOf(false) }
    var delete by remember(collection.folderId) { mutableStateOf(false) }
    SavedItemActionsMenu(enabled = !busy) { dismiss ->
        DropdownMenuItem(
            text = { Text("Переименовать") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = { dismiss(); rename = true },
        )
        DropdownMenuItem(
            text = { Text("Удалить коллекцию") },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            onClick = { dismiss(); delete = true },
        )
    }
    if (rename) {
        AccountCollectionNameDialog(
            initialName = collection.folderName,
            onDismiss = { rename = false },
        ) { name, complete ->
            AccountSavedActions.run(
                context, operation, collections, successMessage = "Коллекция переименована",
                request = { AccountCollectionsApi.rename(account, collection.folderId, name) },
                onResult = { complete(it.isSuccess) },
            )
        }
    }
    if (delete) {
        AlertDialog(
            onDismissRequest = { if (!busy) delete = false },
            title = { Text("Удалить коллекцию?") },
            text = { Text("Коллекция «${collection.folderName}» будет удалена из RedGIFs.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    AccountSavedActions.run(
                        context, operation, collections, collectionItems(collection.folderId),
                        successMessage = "Коллекция удалена",
                        request = { AccountCollectionsApi.delete(account, collection.folderId) },
                        onResult = { if (it.isSuccess) delete = false },
                    )
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { delete = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun AccountCollectionNameDialog(
    initialName: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, (Boolean) -> Unit) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    val trimmed = name.trim()
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (initialName == null) "Новая коллекция RedGIFs" else "Переименовать коллекцию") },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                enabled = !saving,
                singleLine = true,
                label = { Text("Название") },
            )
        },
        confirmButton = {
            TextButton(enabled = !saving && trimmed.isNotEmpty() && trimmed != initialName, onClick = {
                saving = true
                onSave(trimmed) { saved ->
                    saving = false
                    if (saved) onDismiss()
                }
            }) {
                Text(if (initialName == null) "Создать" else "Сохранить")
            }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
internal fun CreateAccountCollectionDialog(
    account: RAccountStatus.SignedIn,
    onDismiss: () -> Unit,
    onCreated: (AccountCollection) -> Unit = {},
) {
    val context = LocalContext.current
    AccountCollectionNameDialog(onDismiss = onDismiss) { name, complete ->
        AccountSavedActions.run(
            context, AccountSavedOperation(account, collections), collections,
            successMessage = "Создана приватная коллекция RedGIFs",
            request = { AccountCollectionsApi.create(account, name) },
            onResult = { result ->
                complete(result.isSuccess)
                result.onSuccess(onCreated)
            },
        )
    }
}

@Composable
internal fun AddToAccountCollectionDialog(
    account: RAccountStatus.SignedIn,
    gifId: String,
    redApi: RedApi,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pagerFlow = remember(redApi, account) { accountCollectionsPager(redApi, account).flow }
    val items = pagerFlow.collectAsLazyPagingItems()
    val pending by AccountSavedActions.pending.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<AccountCollection?>(null) }
    val busy = selection != null

    fun add(collection: AccountCollection) {
        selection = collection
        val list = collectionItems(collection.folderId)
        AccountSavedActions.run(
            context, AccountSavedOperation(account, list), list, collections,
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
        title = { Text("В коллекцию RedGIFs") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item {
                    DropdownMenuItem(
                        text = { Text("Новая коллекция") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
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
