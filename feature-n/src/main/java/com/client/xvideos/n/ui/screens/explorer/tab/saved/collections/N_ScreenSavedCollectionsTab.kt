package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.collectionDB.model.CollectionsGridStyle
import com.client.xvideos.common.collectionDB.ui.CollectionsGrid
import com.client.xvideos.common.collectionDB.ui.DialogNewCollection
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.molecule.NCollectionDetailContent
import com.client.xvideos.n.ui.molecule.NCollectionDialogs
import com.client.xvideos.n.ui.molecule.NCollectionDialogsHost
import com.client.xvideos.n.ui.molecule.NPictureItemExpandMenu
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.model.NCollectionActions
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.model.NCollectionDialogData
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery
import com.client.xvideos.n.ui.screens.picture.N_FullScreenPicture

/**
 * Вкладка пользовательских коллекций страниц манги.
 */
object N_ScreenSavedCollectionsTab : Screen {

    private fun readResolve(): Any = N_ScreenSavedCollectionsTab

    override val key: ScreenKey = "N_ScreenSavedCollectionsTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenSavedNCollectionsSM>()

        val columns by vm.columns.collectAsStateWithLifecycle()
        val collections = vm.collections
        val selectedCollection = vm.currentCollectionName
        val currentItems = vm.currentCollectionItems

        var itemPendingAction by rememberSaveable { mutableStateOf<String?>(null) }
        var itemPendingRename by rememberSaveable { mutableStateOf<String?>(null) }
        var itemPendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
        var renameValue by rememberSaveable { mutableStateOf("") }

        val hasDialog = itemPendingAction != null || itemPendingRename != null || itemPendingDelete != null
        BackHandler(enabled = hasDialog) {
            itemPendingAction = null
            itemPendingRename = null
            itemPendingDelete = null
        }
        BackHandler(enabled = selectedCollection != null && !hasDialog) {
            vm.closeCollection()
        }

        val dialogData = remember(itemPendingAction, itemPendingRename, itemPendingDelete, renameValue) {
            NCollectionDialogData(
                itemPendingAction = itemPendingAction,
                itemPendingRename = itemPendingRename,
                itemPendingDelete = itemPendingDelete,
                renameValue = renameValue
            )
        }

        NCollectionDialogsHost(
            dialogData = dialogData,
            collections = collections,
            onDismissAction = { itemPendingAction = null },
            onDismissRename = { itemPendingRename = null },
            onDismissDelete = { itemPendingDelete = null },
            onRenameAction = { pending ->
                renameValue = pending
                itemPendingRename = pending
                itemPendingAction = null
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
            },
            coverUrlProvider = vm::getCollectionCoverUrl
        )

        val pictureMenuContent: @Composable (NPicture) -> Unit = { picture ->
            NPictureItemExpandMenu(
                picture = picture,
                isLiked = vm.isLiked(picture),
                onOpenGallery = { navigator.push(ScreenNGallery(picture.galleryId)) },
                onToggleLike = { vm.toggleLike(picture) },
                onAddToCollection = { vm.openAddToCollection(picture) },
                onSetCover = { vm.setCover(picture) },
                onRemoveFromCollection = { vm.removeItem(picture) }
            )
        }

        val actions = remember(vm) {
            NCollectionActions(
                onCollectionClick = { vm.openCollection(it) },
                onCollectionLongClick = { name -> itemPendingAction = name },
                onRenameCollection = { name ->
                    renameValue = name
                    itemPendingRename = name
                },
                onDeleteCollection = { name -> itemPendingDelete = name },
                onCreateClick = { vm.isCreateDialogOpen = true },
                onBackFromCollection = { vm.closeCollection() }
            )
        }

        N_ScreenSavedCollectionsTabContent(
            collections = collections,
            selectedCollection = selectedCollection,
            currentItems = currentItems,
            columns = columns,
            actions = actions,
            onPictureClick = { picture ->
                val index = currentItems.indexOfFirst { it.id == picture.id }.coerceAtLeast(0)
                navigator.push(N_FullScreenPicture(pictures = currentItems.toList(), initialIndex = index))
            },
            pictureMenuContent = pictureMenuContent,
            coverUrlProvider = vm::getCollectionCoverUrl
        )

        DialogNewCollection(
            visible = vm.isCreateDialogOpen,
            onDismiss = { vm.isCreateDialogOpen = false },
            onBlockConfirmed = { name ->
                vm.createCollection(name)
                vm.isCreateDialogOpen = false
            }
        )

        NCollectionDialogs(
            isSelectOpen = vm.isSelectOtherCollectionOpen,
            isCreateOpen = vm.isCreateOtherCollectionOpen,
            collections = collections,
            onDismiss = {
                vm.isSelectOtherCollectionOpen = false
                vm.isCreateOtherCollectionOpen = false
            },
            onOpenCreate = {
                vm.isSelectOtherCollectionOpen = false
                vm.isCreateOtherCollectionOpen = true
            },
            onCreateCollection = { name -> vm.createCollectionAndAddTarget(name) },
            onSelectCollection = { name -> vm.addTargetToCollection(name) }
        )
    }
}

@Composable
fun N_ScreenSavedCollectionsTabContent(
    collections: List<CollectionEntity<NPicture>>,
    selectedCollection: String?,
    currentItems: List<NPicture>,
    columns: Int,
    actions: NCollectionActions,
    onPictureClick: (NPicture) -> Unit,
    pictureMenuContent: @Composable (NPicture) -> Unit,
    modifier: Modifier = Modifier,
    coverUrlProvider: (String, List<NPicture>) -> String? = { _, items -> items.firstOrNull()?.thumbnailUrl }
) {
    val gridState = rememberLazyGridState()
    val topInset = getTopInsetDp()

    val gridItems by remember(collections, coverUrlProvider) {
        derivedStateOf {
            collections.map {
                CollectionGridItem(
                    name = it.collection,
                    previewUrl = coverUrlProvider(it.collection, it.items),
                    itemsCount = it.items.size
                )
            }
        }
    }

    val gridStyle = remember {
        CollectionsGridStyle(
            backgroundColor = Theme.background,
            titleColor = Color(0xFFED2553),
            titleFontFamily = FontFamily.Default,
            itemNameColor = Color.White,
            itemSecondaryColor = Color.LightGray,
            itemFontFamily = FontFamily.Default,
            addButtonBackground = Theme.L.primaryColor
        )
    }

    CollectionsGrid(
        selectedCollection = selectedCollection,
        collections = gridItems,
        gridState = gridState,
        style = gridStyle,
        onCollectionClick = actions.onCollectionClick,
        onCollectionLongClick = actions.onCollectionLongClick,
        onCreateNewCollectionClick = actions.onCreateClick,
        navigationContent = {
            if (selectedCollection != null) {
                NCollectionDetailContent(
                    collectionName = selectedCollection,
                    items = currentItems,
                    columns = columns,
                    topInset = topInset,
                    onBack = actions.onBackFromCollection,
                    onRenameCollection = { actions.onRenameCollection(selectedCollection) },
                    onDeleteCollection = { actions.onDeleteCollection(selectedCollection) },
                    onPictureClick = onPictureClick,
                    pictureMenuContent = pictureMenuContent,
                    modifier = modifier
                )
            }
        }
    )
}

@Preview
@Composable
private fun N_ScreenSavedCollectionsTabContentPreview() {
    N_ScreenSavedCollectionsTabContent(
        collections = emptyList(),
        selectedCollection = null,
        currentItems = emptyList(),
        columns = 2,
        actions = NCollectionActions(
            onCollectionClick = {},
            onCollectionLongClick = {},
            onRenameCollection = {},
            onDeleteCollection = {},
            onCreateClick = {},
            onBackFromCollection = {}
        ),
        onPictureClick = {},
        pictureMenuContent = {}
    )
}
