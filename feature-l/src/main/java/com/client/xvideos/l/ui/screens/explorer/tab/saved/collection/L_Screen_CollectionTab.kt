package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule.CollectionDialogData
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule.CollectionDialogsHost
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule.L_CollectionNameContent
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule.L_SavedCollectionTabContent
import com.client.xvideos.ui.theme.XvideosTheme

object L_Screen_CollectionTab : Screen {

    private fun readResolve(): Any = L_Screen_CollectionTab

    override val key: ScreenKey = "L_Screen_CollectionTab"

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenSavedCollectionSM>()
        val savedL = vm.savedL
        val navigator = LocalNavigator.currentOrThrow

        val selectedCollection = savedL.collection.currentCollectionName

        var itemPendingAction by rememberSaveable { mutableStateOf<String?>(null) }
        var itemPendingRename by rememberSaveable { mutableStateOf<String?>(null) }
        var itemPendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
        var renameValue by rememberSaveable { mutableStateOf("") }

        val onBackToResetDialogs = remember {
            {
                itemPendingAction = null
                itemPendingRename = null
                itemPendingDelete = null
            }
        }
        BackHandler(
            enabled = selectedCollection == null &&
                (itemPendingAction != null || itemPendingRename != null || itemPendingDelete != null),
            onBack = onBackToResetDialogs
        )

        val onDismissActionDialog: () -> Unit = remember { { itemPendingAction = null } }
        val onDismissRenameDialog: () -> Unit = remember { { itemPendingRename = null } }
        val onDismissDeleteDialog: () -> Unit = remember { { itemPendingDelete = null } }

        val onRenameAction: (String) -> Unit = remember {
            { pending ->
                renameValue = pending
                itemPendingRename = pending
                itemPendingAction = null
            }
        }
        val onShareAction: (String) -> Unit = remember(navigator) {
            { pending ->
                itemPendingAction = null
                navigator.push(ScreenP2pSend(P2pSendSource.ShareCollection(pending)))
            }
        }
        val onDeleteAction: (String) -> Unit = remember {
            { pending ->
                itemPendingDelete = pending
                itemPendingAction = null
            }
        }

        val onConfirmRename: (String, String) -> Unit = remember(vm) {
            { pending, targetName ->
                itemPendingRename = null
                vm.renameCollection(pending, targetName)
            }
        }
        val onConfirmDelete: (String) -> Unit = remember(vm) {
            { pending ->
                itemPendingDelete = null
                vm.deleteCollection(pending)
            }
        }

        val dialogData = remember(itemPendingAction, itemPendingRename, itemPendingDelete, renameValue) {
            CollectionDialogData(
                itemPendingAction = itemPendingAction,
                itemPendingRename = itemPendingRename,
                itemPendingDelete = itemPendingDelete,
                renameValue = renameValue,
            )
        }

        CollectionDialogsHost(
            dialogData = dialogData,
            collectionList = savedL.collection.collectionList,
            onDismissAction = onDismissActionDialog,
            onDismissRename = onDismissRenameDialog,
            onDismissDelete = onDismissDeleteDialog,
            onRenameAction = onRenameAction,
            onShareAction = onShareAction,
            onDeleteAction = onDeleteAction,
            onConfirmRename = onConfirmRename,
            onConfirmDelete = onConfirmDelete,
        )

        val onSortOrderClick: (LCollectionSortOrder) -> Unit = remember(savedL) {
            { order -> savedL.collection.applySortOrder(order) }
        }
        val onCollectionClick: (String) -> Unit = remember(savedL) {
            { name -> savedL.collection.setCollection(name) }
        }
        val onCollectionLongClick: (String) -> Unit = remember {
            { name -> itemPendingAction = name }
        }
        val onCreateNewCollectionClick: () -> Unit = remember(savedL) {
            { savedL.collection.visibleDialogCreateNew = true }
        }
        val onExitCollection: () -> Unit = remember(savedL) {
            { savedL.collection.exitCollection() }
        }

        CollectionTabContent(
            selectedCollection = selectedCollection,
            savedL = savedL,
            gridState = vm.gridState,
            onSortOrderClick = onSortOrderClick,
            onCollectionClick = onCollectionClick,
            onCollectionLongClick = onCollectionLongClick,
            onCreateNewCollectionClick = onCreateNewCollectionClick,
            onExitCollection = onExitCollection
        )
    }
}

@Composable
fun CollectionTabContent(
    selectedCollection: String?,
    savedL: SavedL,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (String) -> Unit,
    onCreateNewCollectionClick: () -> Unit,
    onExitCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = selectedCollection,
        transitionSpec = {
            fadeIn(animationSpec = tween(220))
                .togetherWith(fadeOut(animationSpec = tween(180)))
        },
        label = "LCollectionTabNavigation",
        modifier = modifier
    ) { collectionName ->
        if (collectionName == null) {
            L_SavedCollectionTabContent(
                collectionList = savedL.collection.collectionList,
                sortOrder = savedL.collection.sortOrder,
                gridState = gridState,
                onSortOrderClick = onSortOrderClick,
                onCollectionClick = onCollectionClick,
                onCollectionLongClick = onCollectionLongClick,
                onCreateNewCollectionClick = onCreateNewCollectionClick
            )
        } else {
            L_CollectionNameContent(
                collectionName = collectionName,
                savedL = savedL,
                onExitCollection = onExitCollection
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun CollectionTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        L_SavedCollectionTabContent(
            collectionList = emptyList(),
            sortOrder = LCollectionSortOrder.RECENT,
            gridState = rememberLazyGridState(),
            onSortOrderClick = {},
            onCollectionClick = {},
            onCollectionLongClick = {},
            onCreateNewCollectionClick = {}
        )
    }
}
