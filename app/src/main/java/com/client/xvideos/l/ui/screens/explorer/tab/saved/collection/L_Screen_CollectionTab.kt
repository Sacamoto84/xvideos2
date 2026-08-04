package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import com.client.xvideos.common.theme.LavenderDialog
import com.client.xvideos.common.coil.UrlImage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.collectionDB.model.CollectionGridItem
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.collectionDB.model.CollectionsGridStyle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.featured.saved.LCollectionEntity
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.l.featured.saved.LSmartCollectionCandidate
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.ui.theme.XvideosTheme
import com.composeunstyled.Text
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.DelicateCoroutinesApi
import javax.inject.Inject

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

        val colorDialogConteiner = Theme.tabLevel4


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

@Composable
private fun LCollectionsTopBar(
    selectedCollection: String?,
    sortOrder: LCollectionSortOrder,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
    onSmartCollectionsClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val usePadding = Settings.useCutoutPadding.field.collectAsStateWithLifecycle().value

    Column(
        modifier = Modifier
            .then(
                if (usePadding) Modifier.displayCutoutPadding()
                else Modifier
            )
    )
    {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                //.background(Theme.tabLevel1)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {

                if (!selectedCollection.isNullOrEmpty()) {
                    Text(
                        ">${selectedCollection}",
                        color = Theme.L.primaryColor,
                        fontSize = 18.sp,
                        fontFamily = Theme.L.fontFamilyPopinsRegular
                    )
                }

                if (selectedCollection == null) {
                    Text(
                        sortOrder.title,
                        color = Theme.L.grey2,
                        fontSize = 12.sp,
                        fontFamily = Theme.L.fontFamilyDMsanss
                    )
                }



            }

            if (selectedCollection == null) {

                TextButton(onClick = onSmartCollectionsClick) {
                    Text("Smart", color = Theme.L.primaryColor, style = Theme.L.Type.button)
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Сортировка коллекций",
                            tint = Theme.L.textColor
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = Theme.L.grey3
                    ) {
                        LCollectionSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        order.title,
                                        style = Theme.L.Type.menuItem.copy(
                                            color = if (order == sortOrder) Color.White else Theme.L.grey2
                                        )
                                    )
                                },
                                onClick = {
                                    onSortOrderClick(order)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

    }
}

@Composable
private fun LSmartCollectionsDialog(
    candidates: List<LSmartCollectionCandidate>,
    onDismiss: () -> Unit,
    onCreate: (LSmartCollectionCandidate) -> Unit
) {
    LavenderDialog(
        title = "Smart collections",
        onDismiss = onDismiss,
        content = {
            if (candidates.isEmpty()) {
                Text(
                    "Пока мало метаданных для авто-коллекций. Добавь несколько элементов из альбомов, где есть теги, авторы или общий album id.",
                    color = Theme.L.grey2,
                    style = Theme.L.Type.body
                )
            } else {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(candidates) { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCreate(candidate) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Theme.L.primaryColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(candidate.count.toString(), color = Theme.L.primaryColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(candidate.title, color = Color.Black, style = Theme.L.Type.rowTitle)
                                Text(candidate.subtitle, color = Theme.L.grey2, style = Theme.L.Type.rowSubtitle)
                            }
                        }
                    }
                }
            }
        },
        dismissText = "Закрыть",
    )
}


class ScreenSavedCollectionSM @Inject constructor(
    val savedL: SavedL,
) : ScreenModel {

    val gridState = LazyGridState()

    /**
     * Хост держит полный список PicsDetails коллекции. Раньше здесь копилась
     * запись на каждую открытую за сессию коллекцию и ни одна не вытеснялась.
     * Теперь это LRU: помним состояние нескольких последних, остальные
     * пересоздаются при следующем открытии.
     */
    private val collectionHosts =
        object : LinkedHashMap<String, LazyRowPictureDetailsHost>(MAX_CACHED_HOSTS, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, LazyRowPictureDetailsHost>?
            ): Boolean = size > MAX_CACHED_HOSTS
        }

    fun hostFor(collectionName: String): LazyRowPictureDetailsHost {
        return collectionHosts.getOrPut(collectionName) {
            LazyRowPictureDetailsHost(collectionName)
        }
    }

    private companion object {
        const val MAX_CACHED_HOSTS = 3
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedCollection {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedCollectionSM::class)
    abstract fun bindScreenLSavedCollectionScreenModel(hiltListScreenModel: ScreenSavedCollectionSM): ScreenModel
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PreviewLCollectionsTopBarSelectionNull() {
    XvideosTheme(darkTheme = true) {
        LCollectionsTopBar(
            selectedCollection = null,
            sortOrder = LCollectionSortOrder.RECENT,
            onSortOrderClick = {},
            onSmartCollectionsClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PreviewLCollectionsTopBarWithSelection() {
    XvideosTheme(darkTheme = true) {
        LCollectionsTopBar(
            selectedCollection = "My Private Collection",
            sortOrder = LCollectionSortOrder.NAME,
            onSortOrderClick = {},
            onSmartCollectionsClick = {}
        )
    }
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
