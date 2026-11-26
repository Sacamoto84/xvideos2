package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountCollection
import com.client.xvideos.r.network.api.AccountCollectionsApi
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedOperation
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.accountMenuItemColors
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.collectionItems
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.collectionsKey
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.SavedItemActionsMenu

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
            colors = accountMenuItemColors(),
            onClick = { dismiss(); rename = true },
        )
        DropdownMenuItem(
            text = { Text("Удалить коллекцию") },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            colors = accountMenuItemColors(),
            onClick = { dismiss(); delete = true },
        )
    }
    if (rename) {
        AccountCollectionNameDialog(
            initialName = collection.folderName,
            onDismiss = { rename = false },
        ) { name, complete ->
            AccountSavedActions.run(
                context, operation, collectionsKey, successMessage = "Коллекция переименована",
                request = { AccountCollectionsApi.rename(account, collection.folderId, name) },
                onResult = { complete(it.isSuccess) },
            )
        }
    }
    if (delete) {
        AlertDialog(
            onDismissRequest = { if (!busy) delete = false },
            title = { Text("Удалить коллекцию?") },
            text = { Text("Коллекция «${collection.folderName}» будет удалена из R.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    AccountSavedActions.run(
                        context, operation, collectionsKey, collectionItems(collection.folderId),
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

@Preview
@Composable
private fun AccountCollectionMenuPreview() {
    AccountCollectionMenu(
        account = RAccountStatus.SignedIn("sample", Long.MAX_VALUE),
        collection = AccountCollection(folderId = "1", folderName = "Избранное"),
    )
}
