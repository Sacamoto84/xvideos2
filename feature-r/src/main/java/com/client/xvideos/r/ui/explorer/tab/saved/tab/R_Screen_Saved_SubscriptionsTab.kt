package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.common.saved.SelectedCreator
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.CreatorsHeader
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.DialogSubscriptionDelete
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host

object R_Screen_Saved_SubscriptionsTab : Screen {

    private fun readResolve(): Any = R_Screen_Saved_SubscriptionsTab

    override val key: ScreenKey = "R_Screen_Saved_SubscriptionsTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenSavedSubscriptionsSM = getScreenModel()

        val pager = vm.likedHost.pager.collectAsLazyPagingItems()

        // Используем SnapshotStateList напрямую для реактивности UI
        val selectedListCreator = vm.savedRed.subscriptions.selectedListCreator

        val hasSelectedCreators by remember(selectedListCreator) {
            derivedStateOf { selectedListCreator.any { it.select } }
        }

        var userToDelete by remember { mutableStateOf<SelectedCreator?>(null) }

        val onDismissDelete = remember { { userToDelete = null } }
        val onClearSelectedCreators = remember(selectedListCreator, pager) {
            {
                for (i in selectedListCreator.indices) {
                    if (selectedListCreator[i].select) {
                        selectedListCreator[i] = selectedListCreator[i].copy(select = false)
                    }
                }
                pager.refresh()
            }
        }

        BackHandler(enabled = userToDelete != null, onBack = onDismissDelete)
        BackHandler(enabled = userToDelete == null && hasSelectedCreators, onBack = onClearSelectedCreators)

        val onOpenProfile = remember(navigator, vm) {
            { username: String ->
                vm.likedHost.currentIndexGoto = vm.likedHost.currentIndex
                navigator.push(ScreenRedProfile(username))
            }
        }
        val onSelectCreator = remember(selectedListCreator, pager) {
            { name: String ->
                val index = selectedListCreator.indexOfFirst { it.name == name }
                if (index != -1) {
                    val item = selectedListCreator[index]
                    selectedListCreator[index] = item.copy(select = !item.select)
                    pager.refresh()
                }
            }
        }
        val onLongClick = remember(selectedListCreator) {
            { name: String ->
                userToDelete = selectedListCreator.firstOrNull { it.name == name }
            }
        }

        SubscriptionsTabContent(
            host = vm.likedHost,
            listCreatorSelectedCreator = selectedListCreator,
            onOpenProfile = onOpenProfile,
            onSelectCreator = onSelectCreator,
            onLongClick = onLongClick
        )

        val onConfirmDelete = remember(vm.savedRed, pager) {
            { creatorName: String ->
                vm.savedRed.subscriptions.remove(creatorName)
                userToDelete = null
                pager.refresh()
            }
        }

        DialogSubscriptionDelete(
            user = userToDelete,
            onDismiss = onDismissDelete,
            onConfirm = onConfirmDelete
        )
    }
}

@Composable
fun SubscriptionsTabContent(
    host: LazyRow123Host?,
    listCreatorSelectedCreator: List<SelectedCreator>,
    onOpenProfile: (String) -> Unit,
    onSelectCreator: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (String) -> Unit = {}
) {
    val renderContentBeforeList: @Composable () -> Unit = remember(
        listCreatorSelectedCreator,
        onSelectCreator,
        onLongClick
    ) {
        {
            CreatorsHeader(
                listCreators = listCreatorSelectedCreator,
                onCreatorClick = onSelectCreator,
                onLongClick = onLongClick
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        if (host != null) {
            LazyRow123(
                host = host,
                modifier = Modifier.fillMaxSize(),
                onClickOpenProfile = onOpenProfile,
                contentPadding = PaddingValues(top = getTopInsetDp()),
                contentBeforeList = renderContentBeforeList,
                isRunLike = true
            )
        } else {
            // Fallback for Preview
            Box(modifier = Modifier.padding(top = getTopInsetDp())) {
                renderContentBeforeList()
            }
        }
    }
}

@Preview
@Composable
private fun SubscriptionsTabPreview() {
    SubscriptionsTabContent(
        host = null,
        listCreatorSelectedCreator = listOf(
            SelectedCreator("Creator 1", true, null),
            SelectedCreator("Another One", false, null),
            SelectedCreator("Superstar", true, null)
        ),
        onOpenProfile = {},
        onSelectCreator = {}
    )
}
