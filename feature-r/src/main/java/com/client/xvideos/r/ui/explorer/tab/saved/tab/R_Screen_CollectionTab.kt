package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.collectionDB.model.CollectionsGridStyle
import com.client.xvideos.common.collectionDB.ui.CollectionsGrid
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.explorer.tab.saved.tab.collection.ScreenCollectionName
import com.client.xvideos.r.ui.explorer.tab.saved.tab.model.R_CollectionDialogActions
import com.client.xvideos.r.ui.explorer.tab.saved.tab.model.R_CollectionDialogData
import com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule.R_CollectionDialogsHost
import com.client.xvideos.ui.theme.XvideosTheme
import timber.log.Timber

object R_Screen_CollectionTab : Screen {

    private fun readResolve(): Any = R_Screen_CollectionTab

    override val key: ScreenKey = "R_Screen_CollectionTab"

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenSavedCollectionSM>()

        val navigator = LocalNavigator.currentOrThrow

        val savedRed = vm.savedRed

        val selectedCollection = savedRed.collections.selectedCollection.collectAsStateWithLifecycle().value

        val onClearSelectedCollection = remember(savedRed) {
            {
                Timber.d("BackHandler SavedCollectionTab")
                savedRed.collections.selectedCollection.value = null
            }
        }
        BackHandler(enabled = selectedCollection != null, onBack = onClearSelectedCollection)

        var itemPendingAction by rememberSaveable { mutableStateOf<String?>(null) }
        var itemPendingRename by rememberSaveable { mutableStateOf<String?>(null) }
        var renameValue by rememberSaveable { mutableStateOf("") }
        var itemPendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

        val onDismissAllDialogs = remember {
            {
                itemPendingAction = null
                itemPendingRename = null
                itemPendingDelete = null
            }
        }
        BackHandler(
            enabled = selectedCollection == null &&
                (itemPendingAction != null || itemPendingRename != null || itemPendingDelete != null),
            onBack = onDismissAllDialogs
        )

        val coverOf: (String) -> String? = remember(savedRed.collections.collectionList) {
            { name ->
                savedRed.collections.collectionList
                    .firstOrNull { it.collection == name }
                    ?.items?.lastOrNull()?.urls?.thumbnail
            }
        }

        val actions = remember(navigator, vm) {
            R_CollectionDialogActions(
                onDismissAction = { itemPendingAction = null },
                onDismissRename = { itemPendingRename = null },
                onDismissDelete = { itemPendingDelete = null },
                onRenameValueChange = { text -> renameValue = text },
                onRenameAction = { pending ->
                    renameValue = pending
                    itemPendingRename = pending
                    itemPendingAction = null
                },
                onShareAction = { pending ->
                    itemPendingAction = null
                    navigator.push(ScreenP2pSend(P2pSendSource.ShareCollectionR(pending)))
                },
                onDeleteAction = { pending ->
                    itemPendingDelete = pending
                    itemPendingAction = null
                },
                onConfirmRename = { pending, targetName ->
                    itemPendingRename = null
                    vm.renameCollection(pending, targetName)
                },
                onConfirmDelete = { pending ->
                    itemPendingDelete = null
                    vm.deleteCollection(pending)
                }
            )
        }

        val dialogData = remember(itemPendingAction, itemPendingRename, itemPendingDelete, renameValue) {
            R_CollectionDialogData(
                itemPendingAction = itemPendingAction,
                itemPendingRename = itemPendingRename,
                itemPendingDelete = itemPendingDelete,
                renameValue = renameValue,
            )
        }

        R_CollectionDialogsHost(
            dialogData = dialogData,
            coverOf = coverOf,
            actions = actions,
        )

        val onCollectionClick: (String) -> Unit = remember(savedRed) { { name -> savedRed.collections.selectedCollection.value = name } }
        val onCollectionLongClick: (String) -> Unit = remember { { name -> itemPendingAction = name } }
        val onCreateNewCollectionClick: () -> Unit = remember(savedRed) { { savedRed.collections.visibleDialogCreateNew = true } }
        val navigationContent: @Composable () -> Unit = remember(selectedCollection) {
            {
                if (selectedCollection != null) {
                    Navigator(ScreenCollectionName(selectedCollection))
                }
            }
        }

        R_SavedCollectionTabContent(
            selectedCollection = selectedCollection,
            collectionList = savedRed.collections.collectionList,
            gridState = vm.gridState,
            onCollectionClick = onCollectionClick,
            onCollectionLongClick = onCollectionLongClick,
            onCreateNewCollectionClick = onCreateNewCollectionClick,
            navigationContent = navigationContent
        )
    }
}

@Composable
fun R_SavedCollectionTabContent(
    selectedCollection: String?,
    collectionList: List<CollectionEntity<GifsInfo>>,
    gridState: LazyGridState,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (String) -> Unit,
    onCreateNewCollectionClick: () -> Unit,
    navigationContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridItems = remember(collectionList) {
        collectionList.map {
            CollectionGridItem(
                name = it.collection,
                previewUrl = it.items.lastOrNull()?.urls?.thumbnail,
                itemsCount = null
            )
        }
    }
    val gridStyle = remember {
        CollectionsGridStyle(
            backgroundColor = Color.Transparent,
            titleColor = Theme.R.colorYellow,
            titleFontFamily = Theme.R.fontFamilyPopinsRegular,
            itemNameColor = Color.White,
            itemSecondaryColor = Color.LightGray,
            itemFontFamily = Theme.R.fontFamilyDMsanss,
            addButtonBackground = Theme.R.colorYellow
        )
    }
    Box(modifier = modifier) {
        CollectionsGrid(
            selectedCollection = selectedCollection,
            collections = gridItems,
            gridState = gridState,
            style = gridStyle,
            onCollectionClick = onCollectionClick,
            onCollectionLongClick = onCollectionLongClick,
            onCreateNewCollectionClick = onCreateNewCollectionClick,
            navigationContent = navigationContent
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun R_SavedCollectionTabPreview() {
    XvideosTheme(darkTheme = true) {
        val sampleCollections = listOf(
            CollectionEntity(
                collection = "Favorites",
                items = emptyList<GifsInfo>()
            )
        )
        R_SavedCollectionTabContent(
            selectedCollection = null,
            collectionList = sampleCollections,
            gridState = rememberLazyGridState(),
            onCollectionClick = {},
            onCollectionLongClick = {},
            onCreateNewCollectionClick = {},
            navigationContent = {}
        )
    }
}
