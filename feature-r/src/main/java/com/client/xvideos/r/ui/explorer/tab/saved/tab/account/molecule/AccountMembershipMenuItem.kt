package com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedKey
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedOperation
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.accountMenuItemColors
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.savedList
import com.client.xvideos.r.ui.niche.atom.NICHE_JOIN_LABEL
import com.client.xvideos.r.ui.niche.atom.NICHE_LEAVE_LABEL

@Composable
internal fun AccountMembershipMenuItem(
    account: RAccountStatus.SignedIn,
    kind: AccountMembership,
    id: String,
    knownMember: Boolean? = null,
    dismiss: () -> Unit,
) {
    val context = LocalContext.current
    val list = AccountSavedKey(kind.savedList)
    val revisionFlow = remember(list) { AccountSavedActions.revision(list) }
    val revision by revisionFlow.collectAsStateWithLifecycle(initialValue = 0)
    val pending by AccountSavedActions.pending.collectAsStateWithLifecycle()
    val operation = AccountSavedOperation(account, AccountSavedKey(kind.savedList, id))
    var retry by remember(account, kind, id) { mutableIntStateOf(0) }
    var membership by remember(account, kind, id, knownMember, retry, revision) {
        mutableStateOf(knownMember?.let { Result.success(it) })
    }
    LaunchedEffect(account, kind, id, knownMember, retry, revision) {
        if (knownMember == null) membership = AccountSavedActions.member(account, kind, id)
    }

    val result = membership
    val member = result?.getOrNull()
    if (member == null) {
        val action = kind.pendingLabel()
        DropdownMenuItem(
            text = { Text(if (result == null) "$action · Загрузка…" else "$action · Повторить") },
            colors = accountMenuItemColors(),
            enabled = result != null,
            onClick = { retry++ },
        )
        return
    }

    val spec = kind.itemSpec(member, knownMember)
    DropdownMenuItem(
        text = { Text(spec.label) },
        leadingIcon = { Icon(spec.icon, contentDescription = null) },
        colors = accountMenuItemColors(),
        enabled = operation !in pending,
        onClick = {
            dismiss()
            AccountSavedActions.run(
                context, operation, list,
                successMessage = spec.successMessage,
                request = { kind.set(account, id, !member) },
            )
        },
    )
}

private data class MembershipItemSpec(
    val icon: ImageVector,
    val label: String,
    val successMessage: String,
)

private fun AccountMembership.pendingLabel(): String = when (this) {
    AccountMembership.LIKE -> "Лайк в R"
    AccountMembership.AUTHOR -> "Подписка в R"
    AccountMembership.NICHE -> "Участие в группе R"
}

private fun AccountMembership.itemSpec(member: Boolean, knownMember: Boolean?): MembershipItemSpec = when (this) {
    AccountMembership.LIKE -> if (member) {
        MembershipItemSpec(
            icon = Icons.Filled.Favorite,
            label = if (knownMember == true) "Убрать лайк" else "Убрать лайк в R",
            successMessage = "Лайк R убран",
        )
    } else {
        MembershipItemSpec(Icons.Filled.FavoriteBorder, "Лайк в R", "Лайк в R поставлен")
    }
    AccountMembership.AUTHOR -> if (member) {
        MembershipItemSpec(
            icon = Icons.Filled.PersonRemove,
            label = if (knownMember == true) "Отписаться" else "Отписаться в R",
            successMessage = "Подписка R отменена",
        )
    } else {
        MembershipItemSpec(Icons.Filled.PersonAdd, "Подписаться в R", "Подписка R оформлена")
    }
    AccountMembership.NICHE -> if (member) {
        MembershipItemSpec(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            label = if (knownMember == true) NICHE_LEAVE_LABEL else "$NICHE_LEAVE_LABEL R",
            successMessage = "Вы вышли из группы R",
        )
    } else {
        MembershipItemSpec(Icons.Filled.GroupAdd, "$NICHE_JOIN_LABEL R", "Вы вступили в группу R")
    }
}

@Preview
@Composable
private fun AccountMembershipMenuItemPreview() {
    AccountMembershipMenuItem(
        account = RAccountStatus.SignedIn("sample", "preview"),
        kind = AccountMembership.LIKE,
        id = "sample",
        knownMember = true,
        dismiss = {},
    )
}
