package com.client.xvideos.r.ui.explorer.tab.saved.tab.account

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.auth.RAccountStatus
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.SavedItemActionsMenu

@Composable
internal fun AccountMembershipMenu(
    kind: AccountMembership,
    id: String,
    modifier: Modifier = Modifier,
    knownMember: Boolean? = null,
) {
    val account = signedInRedgifsAccount() ?: return
    if (id.isBlank()) return
    val pending by AccountSavedActions.pending.collectAsStateWithLifecycle()
    val operation = AccountSavedOperation(account, AccountSavedKey(kind.savedList, id))
    SavedItemActionsMenu(modifier = modifier, enabled = operation !in pending) { dismiss ->
        AccountMembershipMenuItem(account, kind, id, knownMember, dismiss)
    }
}

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
        val action = when (kind) {
            AccountMembership.LIKE -> "Лайк на RedGIFs"
            AccountMembership.AUTHOR -> "Подписка на RedGIFs"
            AccountMembership.NICHE -> "Группа на RedGIFs"
        }
        DropdownMenuItem(
            text = { Text(if (result == null) "$action · Загрузка…" else "$action · Повторить") },
            enabled = result != null,
            onClick = { retry++ },
        )
        return
    }

    val (icon, label) = when (kind) {
        AccountMembership.LIKE -> if (member) Icons.Filled.Favorite to "Убрать лайк на RedGIFs"
        else Icons.Filled.FavoriteBorder to "Лайк на RedGIFs"
        AccountMembership.AUTHOR -> if (member) Icons.Filled.PersonRemove to
            (if (knownMember == true) "Отписаться" else "Отписаться на RedGIFs")
        else Icons.Filled.PersonAdd to "Подписаться на RedGIFs"
        AccountMembership.NICHE -> if (member) Icons.AutoMirrored.Filled.ExitToApp to "Выйти из группы"
        else Icons.Filled.GroupAdd to "Вступить на RedGIFs"
    }
    DropdownMenuItem(
        text = { Text(if (knownMember == true && kind == AccountMembership.LIKE) "Убрать лайк" else label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        enabled = operation !in pending,
        onClick = {
            dismiss()
            AccountSavedActions.run(
                context, operation, list, successMessage = when (kind) {
                    AccountMembership.LIKE -> if (member) "Лайк RedGIFs убран" else "Лайк на RedGIFs поставлен"
                    AccountMembership.AUTHOR -> if (member) "Подписка RedGIFs отменена" else "Подписка RedGIFs оформлена"
                    AccountMembership.NICHE -> if (member) "Вы вышли из группы RedGIFs" else "Вы вступили в группу RedGIFs"
                },
                request = { kind.set(account, id, !member) },
            )
        },
    )
}
