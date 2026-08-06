package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.collectionDB.model.CollectionsGridStyle
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.featured.saved.LCollectionEntity
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.ui.theme.XvideosTheme
import com.composeunstyled.Text
import kotlinx.coroutines.DelicateCoroutinesApi

object L_Screen_CollectionTab : Screen {

    private fun readResolve(): Any = L_Screen_CollectionTab

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenSavedCollectionSM>()

        val savedL = vm.savedL

        val navigator = LocalNavigator.currentOrThrow

        val selectedCollection = savedL.collection.currentCollectionName

        // Back обрабатывается внутри L_CollectionNameContent (через onExitCollection),
        // когда коллекция открыта. Отдельный BackHandler на уровне таба не нужен.

        var itemPendingAction by remember { mutableStateOf<String?>(null) }
        var itemPendingRename by remember { mutableStateOf<String?>(null) }
        var itemPendingDelete by remember { mutableStateOf<String?>(null) }
        var renameValue by remember { mutableStateOf("") }

        var smartCollectionsVisible by remember { mutableStateOf(false) }

        LaunchedEffect(smartCollectionsVisible) {
            if (smartCollectionsVisible) {
                savedL.collection.refreshSmartCollectionCandidates()
            }
        }

        if (smartCollectionsVisible) {
            LSmartCollectionsDialog(
                candidates = savedL.collection.smartCollectionCandidates,
                onDismiss = { smartCollectionsVisible = false },
                onCreate = { candidate ->
                    savedL.collection.createSmartCollection(candidate)
                    smartCollectionsVisible = false
                }
            )
        }



        itemPendingAction?.let { pending ->

            LavenderDialog(
                title = "Действие с коллекцией",
                onDismiss = { itemPendingAction = null },
                icon = {
                    val cover = savedL.collection.collectionList
                        .firstOrNull { it.collection == pending }?.previewUrl
                    val iconSize = Theme.DialogLavande.iconSize
                    if (cover != null) {
                        UrlImage(url = cover, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(iconSize))
                    } else {
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).size(iconSize).background(Color.Gray))
                    }
                },
                content = {
                    Text(pending, fontSize = 16.sp, color = Theme.L.b0)

                    DropdownMenuItem(
                        text = { Text("Переименовать", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = {
                            renameValue = pending
                            itemPendingRename = pending
                            itemPendingAction = null
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
                    )

                    DropdownMenuItem(
                        text = { Text("Поделиться (P2P)", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = {
                            itemPendingAction = null
                            navigator.push(ScreenP2pSend(P2pSendSource.ShareCollection(pending)))
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
                    )

                    DropdownMenuItem(
                        text = { Text("Удалить коллекцию", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = {
                            itemPendingDelete = pending
                            itemPendingAction = null
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
                    )
                },
            )
        }

        itemPendingRename?.let { pending ->
            LavenderDialog(
                title = "Переименовать коллекцию",
                onDismiss = { itemPendingRename = null },
                content = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        singleLine = true,
                        textStyle = Theme.L.Type.body
                    )
                },
                confirmText = "Сохранить",
                onConfirm = {
                    if (savedL.collection.renameCollection(pending, renameValue)) {
                        itemPendingRename = null
                    }
                },
            )
        }
        /* ---------- Диалог подтверждения ---------- */
        itemPendingDelete?.let { pending ->
            LavenderDialog(
                title = "Удалить коллекцию?",
                onDismiss = { itemPendingDelete = null },
                body = buildAnnotatedString {
                    append("Удалить «")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending) }
                    append("» из коллекции")
                },
                confirmText = "Удалить",
                onConfirm = {
                    savedL.collection.deleteCollection(pending)
                    itemPendingDelete = null
                },
                destructive = true,
            )
        }

        // Таб показывает ЛИБО список коллекций, ЛИБО одну открытую коллекцию.
        // Раньше открытая коллекция рендерилась ВНУТРИ Scaffold-а грида
        // (вложенные Scaffold + двойной topBar + мигание). Теперь это сосед.
        if (selectedCollection == null) {
            L_SavedCollectionTabContent(
                collectionList = savedL.collection.collectionList,
                sortOrder = savedL.collection.sortOrder,
                gridState = vm.gridState,
                onSortOrderClick = { savedL.collection.applySortOrder(it) },
                onSmartCollectionsClick = { smartCollectionsVisible = true },
                onCollectionClick = { savedL.collection.setCollection(it) },
                onCollectionLongClick = { itemPendingAction = it },
                onCreateNewCollectionClick = { savedL.collection.visibleDialogCreateNew = true }
            )
        } else {
            // Открытая коллекция — отдельный Screen (свой ScreenModel/host/lifecycle)
            // во вложенном Navigator. Back внутри сбрасывает currentCollectionName.
            Navigator(ScreenCollectionName(selectedCollection))
        }
    }
}

@Composable
fun L_SavedCollectionTabContent(
    collectionList: List<LCollectionEntity>,
    sortOrder: LCollectionSortOrder,
    gridState: LazyGridState,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
    onSmartCollectionsClick: () -> Unit,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (String) -> Unit,
    onCreateNewCollectionClick: () -> Unit
) {
    CollectionsGrid(
        collections = collectionList.map {
            CollectionGridItem(
                name = it.collection,
                previewUrl = it.previewUrl,
                itemsCount = it.itemsCount
            )
        },
        gridState = gridState,
        style = CollectionsGridStyle(
            backgroundColor = Theme.background,
            titleColor = Theme.L.primaryColor,
            titleFontFamily = Theme.L.fontFamilyPopinsRegular,
            itemNameColor = Color.White,
            itemSecondaryColor = Color.LightGray,
            itemFontFamily = Theme.L.fontFamilyDMsanss,
            addButtonBackground = Theme.L.primaryColor
        ),
        onCollectionClick = onCollectionClick,
        onCollectionLongClick = onCollectionLongClick,
        onCreateNewCollectionClick = onCreateNewCollectionClick,
        topBar = {
            LCollectionsTopBar(
                selectedCollection = null,
                sortOrder = sortOrder,
                onSortOrderClick = onSortOrderClick,
                onSmartCollectionsClick = onSmartCollectionsClick
            )
        }
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
            onSmartCollectionsClick = {},
            onCollectionClick = {},
            onCollectionLongClick = {},
            onCreateNewCollectionClick = {}
        )
    }
}
