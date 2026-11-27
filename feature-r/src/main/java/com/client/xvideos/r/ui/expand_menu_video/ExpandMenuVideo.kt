package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.AccountCollectionsApi
import com.client.xvideos.r.network.api.AccountMembership
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedKey
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedList
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.AccountSavedOperation
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.accountMenuItemColors
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.collectionItems
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AccountMembershipMenuItem
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.molecule.AddToAccountCollectionDialog
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.signedInAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandMenuVideo(
    block: () -> BlockRed,
    redApi: () -> RedApi,
    savedRed: () -> SavedRed,
    downloadRed: () -> DownloadRed,
    modifier: Modifier = Modifier,
    item: GifsInfo? = null,
    onClick: () -> Unit = {},
    onRunLike: () -> Unit = {},
    onRefresh: () -> Unit = {},
    isCollection : Boolean = false,
    haptic : ()->Unit = {},
    accountLikes: Boolean = false,
    accountCollectionId: String? = null,
) {

    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val navigator = cafe.adriel.voyager.navigator.LocalNavigator.current
    val currentNavigator by rememberUpdatedState(navigator)
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentLifecycleOwner by rememberUpdatedState(lifecycleOwner)
    val account = signedInAccount()
    var choosingAccountCollection by remember(account, item?.id) { mutableStateOf(false) }
    val gifId = item?.id?.takeIf { it.isNotBlank() }

    if (choosingAccountCollection && account != null && gifId != null) {
        AddToAccountCollectionDialog(account, gifId, redApi()) { choosingAccountCollection = false }
    }

    // Локальный state этого экземпляра меню — диалог показывает только тот item, где кликнули.
    var chooserItem by remember { mutableStateOf<GifsInfo?>(null) }

    LaunchedEffect(expanded) {
        if (expanded) {
            haptic.invoke()
        }
    }

    chooserItem?.let { ci ->
        com.client.xvideos.common.p2p.ui.P2pSendChooserDialog(
            onSystem = { downloadRed.invoke().downloadItemAndShare(appContext, ci) },
            onP2p = {
                // Шлём только метаданные (.info + .jpg) — видео получатель стримит по URL.
                downloadRed.invoke().shareMetaByP2p(ci) { bundle ->
                    if (currentLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        currentNavigator?.push(
                            com.client.xvideos.common.p2p.ui.ScreenP2pSend(
                                com.client.xvideos.common.p2p.P2pSendSource.Ready(bundle)
                            )
                        )
                    }
                }
            },
            onDismiss = { chooserItem = null },
        )
    }

    ExpandMenuVideoContent(
        expanded = expanded,
        onExpandedChange = {
            if (it) onClick.invoke()
            expanded = it
        },
        onDismissRequest = { expanded = false },
        modifier = modifier
    ) {
        if (account != null && gifId != null) {
            AccountMembershipMenuItem(account, AccountMembership.LIKE, gifId, knownMember = true.takeIf { accountLikes }) {
                expanded = false
            }
            DropdownMenuItem(
                text = { Text("В коллекцию R…") },
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                colors = accountMenuItemColors(),
                onClick = {
                    expanded = false
                    choosingAccountCollection = true
                },
            )
            if (accountCollectionId != null) {
                val list = collectionItems(accountCollectionId)
                val operation = AccountSavedOperation(account, list)
                val pending by AccountSavedActions.pending.collectAsStateWithLifecycle()
                DropdownMenuItem(
                    text = { Text("Убрать из коллекции") },
                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null) },
                    colors = accountMenuItemColors(),
                    enabled = operation !in pending,
                    onClick = {
                        expanded = false
                        AccountSavedActions.run(
                            context, operation, list, AccountSavedKey(AccountSavedList.COLLECTIONS),
                            successMessage = "Убрано из коллекции R",
                            request = { AccountCollectionsApi.setItem(account, accountCollectionId, gifId, added = false) },
                        )
                    },
                )
            }
        }
        DropdownMenuItem_Like(item, onRunLike, savedRed) { expanded = false }
        DropdownMenuItem_Download(item, onClick = { downloadRed.invoke().downloadItem(it) }) { expanded = false }
        DropdownMenuItem_Share(item, onClick = { chooserItem = it }) { expanded = false }
        DropdownMenuItem_SaveToGallery(item, onClick = { downloadRed.invoke().saveToGallery(it) }) { expanded = false }
        DropdownMenuItem_Block(item = item, block = block) { expanded = false }
        DropdownMenuItem_Follow(item, redApi, savedRed) { expanded = false }
        DropdownMenuItem_AddCollection(item, savedRed) { expanded = false }
        if(isCollection) DropdownMenuItem_RemoveFromCollection(item, onRefresh, savedRed) { expanded = false }
        DropdownMenuItem_Subscription(item, redApi, savedRed) { expanded = false }
    }
}
