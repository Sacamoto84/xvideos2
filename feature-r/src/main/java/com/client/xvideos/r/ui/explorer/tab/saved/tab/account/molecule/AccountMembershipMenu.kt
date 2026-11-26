package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedKey
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedOperation
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.savedList
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.signedInAccount
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.SavedItemActionsMenu

@Composable
internal fun AccountMembershipMenu(
    kind: AccountMembership,
    id: String,
    modifier: Modifier = Modifier,
    knownMember: Boolean? = null,
) {
    val account = signedInAccount() ?: return
    if (id.isBlank()) return
    val pending by AccountSavedActions.pending.collectAsStateWithLifecycle()
    val operation = AccountSavedOperation(account, AccountSavedKey(kind.savedList, id))
    SavedItemActionsMenu(modifier = modifier, enabled = operation !in pending) { dismiss ->
        AccountMembershipMenuItem(account, kind, id, knownMember, dismiss)
    }
}

@Preview
@Composable
private fun AccountMembershipMenuPreview() {
    AccountMembershipMenu(kind = AccountMembership.NICHE, id = "sample")
}
