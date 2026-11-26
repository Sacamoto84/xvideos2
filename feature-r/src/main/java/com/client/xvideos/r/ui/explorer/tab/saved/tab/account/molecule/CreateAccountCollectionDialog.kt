package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountCollection
import com.client.xvideos.r.network.api.AccountCollectionsApi
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedOperation
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.collectionsKey

@Composable
internal fun CreateAccountCollectionDialog(
    account: RAccountStatus.SignedIn,
    onDismiss: () -> Unit,
    onCreated: (AccountCollection) -> Unit = {},
) {
    val context = LocalContext.current
    AccountCollectionNameDialog(onDismiss = onDismiss) { name, complete ->
        AccountSavedActions.run(
            context, AccountSavedOperation(account, collectionsKey), collectionsKey,
            successMessage = "Создана приватная коллекция R",
            request = { AccountCollectionsApi.create(account, name) },
            onResult = { result ->
                complete(result.isSuccess)
                result.onSuccess(onCreated)
            },
        )
    }
}

@Preview
@Composable
private fun CreateAccountCollectionDialogPreview() {
    CreateAccountCollectionDialog(
        account = RAccountStatus.SignedIn("sample", Long.MAX_VALUE),
        onDismiss = {},
    )
}
