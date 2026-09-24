package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.coil.UrlImage
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.collectionDB.model.CollectionsGridStyle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.featured.saved.LCollectionEntity
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
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

        // Back обрабатывается внутри L_CollectionNameContent (через onExitCollection),
        // когда коллекция открыта. Отдельный BackHandler на уровне таба не нужен.

        // Открытый диалог и набранный текст переживают пересоздание
        // композиции: на remember пересоздание Activity молча закрывало их.
        var itemPendingAction by rememberSaveable { mutableStateOf<String?>(null) }
        var itemPendingRename by rememberSaveable { mutableStateOf<String?>(null) }
        var itemPendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
        var renameValue by rememberSaveable { mutableStateOf("") }

        BackHandler(
            enabled = selectedCollection == null &&
                (itemPendingAction != null || itemPendingRename != null || itemPendingDelete != null)
        ) {
            itemPendingAction = null
            itemPendingRename = null
            itemPendingDelete = null
        }

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

        val dialogData = CollectionDialogData(
            itemPendingAction = itemPendingAction,
            itemPendingRename = itemPendingRename,
            itemPendingDelete = itemPendingDelete,
            renameValue = renameValue,
        )

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
            { savedL.collection.applySortOrder(it) }
        }
        val onCollectionClick: (String) -> Unit = remember(savedL) {
            { savedL.collection.setCollection(it) }
        }
        val onCollectionLongClick: (String) -> Unit = remember {
            { itemPendingAction = it }
        }
        val onCreateNewCollectionClick: () -> Unit = remember(savedL) {
            { savedL.collection.visibleDialogCreateNew = true }
        }
        val onExitCollection: () -> Unit = remember(savedL) {
            { savedL.collection.exitCollection() }
        }

        // Декларативная навигация таба: список коллекций <-> открытая коллекция.
        // Переключение через AnimatedContent с плавным fading переходом.
        AnimatedContent(
            targetState = selectedCollection,
            transitionSpec = {
                fadeIn(animationSpec = tween(220))
                    .togetherWith(fadeOut(animationSpec = tween(180)))
            },
            label = "LCollectionTabNavigation"
        ) { collectionName ->
            if (collectionName == null) {
                L_SavedCollectionTabContent(
                    collectionList = savedL.collection.collectionList,
                    sortOrder = savedL.collection.sortOrder,
                    gridState = vm.gridState,
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
}

@Composable
fun L_SavedCollectionTabContent(
    collectionList: List<LCollectionEntity>,
    sortOrder: LCollectionSortOrder,
    gridState: LazyGridState,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (String) -> Unit,
    onCreateNewCollectionClick: () -> Unit
) {
    val gridItems = remember(collectionList) {
        collectionList.map {
            CollectionGridItem(
                name = it.collection,
                previewUrl = it.previewUrl,
                itemsCount = it.itemsCount
            )
        }
    }
    val gridStyle = remember {
        CollectionsGridStyle(
            backgroundColor = Theme.background,
            titleColor = Theme.L.primaryColor,
            titleFontFamily = Theme.L.fontFamilyPopinsRegular,
            itemNameColor = Color.White,
            itemSecondaryColor = Color.LightGray,
            itemFontFamily = Theme.L.fontFamilyDMsanss,
            addButtonBackground = Theme.L.primaryColor
        )
    }
    val topBarContent: @Composable () -> Unit = remember(sortOrder, onSortOrderClick) {
        {
            LCollectionsTopBar(
                selectedCollection = null,
                sortOrder = sortOrder,
                onSortOrderClick = onSortOrderClick,
            )
        }
    }
    CollectionsGrid(
        collections = gridItems,
        gridState = gridState,
        style = gridStyle,
        onCollectionClick = onCollectionClick,
        onCollectionLongClick = onCollectionLongClick,
        onCreateNewCollectionClick = onCreateNewCollectionClick,
        topBar = topBarContent
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PreviewL_SavedCollectionTabContent() {
    XvideosTheme(darkTheme = true) {
        L_SavedCollectionTabContent(
            collectionList = listOf(
                LCollectionEntity("Favorites", null, 10, 0, 0, false),
                LCollectionEntity("Travel", null, 5, 0, 0, false),
                LCollectionEntity("Work", null, 2, 0, 0, false)
            ),
            sortOrder = LCollectionSortOrder.RECENT,
            gridState = rememberLazyGridState(),
            onSortOrderClick = {},
            onCollectionClick = {},
            onCollectionLongClick = {},
            onCreateNewCollectionClick = {}
        )
    }
}

@Immutable
private data class CollectionDialogData(
    val itemPendingAction: String? = null,
    val itemPendingRename: String? = null,
    val itemPendingDelete: String? = null,
    val renameValue: String = "",
)

@Composable
private fun CollectionDialogsHost(
    dialogData: CollectionDialogData,
    collectionList: List<LCollectionEntity>,
    onDismissAction: () -> Unit,
    onDismissRename: () -> Unit,
    onDismissDelete: () -> Unit,
    onRenameAction: (String) -> Unit,
    onShareAction: (String) -> Unit,
    onDeleteAction: (String) -> Unit,
    onConfirmRename: (String, String) -> Unit,
    onConfirmDelete: (String) -> Unit,
) {
    dialogData.itemPendingAction?.let { pending ->
        val cover = collectionList
            .firstOrNull { it.collection == pending }?.previewUrl
        val handleRename = remember(pending, onRenameAction) { { onRenameAction(pending) } }
        val handleShare = remember(pending, onShareAction) { { onShareAction(pending) } }
        val handleDelete = remember(pending, onDeleteAction) { { onDeleteAction(pending) } }

        CollectionActionDialog(
            pending = pending,
            coverUrl = cover,
            onDismiss = onDismissAction,
            onRename = handleRename,
            onShare = handleShare,
            onDelete = handleDelete
        )
    }

    dialogData.itemPendingRename?.let { pending ->
        val handleConfirmRename = remember(pending, onConfirmRename) {
            { targetName: String -> onConfirmRename(pending, targetName) }
        }
        CollectionRenameDialog(
            initialValue = dialogData.renameValue,
            onDismiss = onDismissRename,
            onConfirm = handleConfirmRename
        )
    }

    dialogData.itemPendingDelete?.let { pending ->
        val handleConfirmDelete = remember(pending, onConfirmDelete) {
            { onConfirmDelete(pending) }
        }
        CollectionDeleteDialog(
            pending = pending,
            onDismiss = onDismissDelete,
            onConfirm = handleConfirmDelete
        )
    }
}

@Composable
private fun CollectionActionDialog(
    pending: String,
    coverUrl: String?,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    LavenderDialog(
        title = "Действие с коллекцией",
        onDismiss = onDismiss,
        icon = {
            val iconSize = Theme.DialogLavande.iconSize
            if (coverUrl != null) {
                UrlImage(url = coverUrl, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(iconSize))
            } else {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).size(iconSize).background(Color.Gray))
            }
        },
        content = {
            Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    pending,
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            DropdownMenuItem(
                text = { Text("Переименовать", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                onClick = onRename,
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
            )

            DropdownMenuItem(
                text = { Text("Поделиться (P2P)", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                onClick = onShare,
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
            )

            DropdownMenuItem(
                text = { Text("Удалить коллекцию", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                onClick = onDelete,
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
            )
        }
    )
}

@Composable
private fun CollectionRenameDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var renameValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    val onValueChange: (String) -> Unit = remember { { renameValue = it } }
    val onConfirmClick: () -> Unit = remember(onConfirm) { { onConfirm(renameValue) } }

    LavenderDialog(
        title = "Переименовать коллекцию",
        onDismiss = onDismiss,
        content = {
            OutlinedTextField(
                value = renameValue,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название коллекции") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Theme.DialogLavande.buttonBackground,
                    unfocusedBorderColor = Color(0x66FFFFFF),
                    focusedLabelColor = Theme.DialogLavande.dismissTextColor,
                    unfocusedLabelColor = Theme.DialogLavande.bodyColor,
                )
            )
        },
        confirmText = "Сохранить",
        onConfirm = onConfirmClick
    )
}

@Composable
private fun CollectionDeleteDialog(
    pending: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    LavenderDialog(
        title = "Удалить коллекцию?",
        onDismiss = onDismiss,
        body = buildAnnotatedString {
            append("Удалить «")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending) }
            append("» из коллекции")
        },
        confirmText = "Удалить",
        onConfirm = onConfirm,
        destructive = true
    )
}
