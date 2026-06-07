package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.collectionDB.ui.CollectionGridItem
import com.client.xvideos.common.collectionDB.ui.CollectionsGrid
import com.client.xvideos.common.collectionDB.ui.CollectionsGridStyle
import com.client.xvideos.l.featured.saved.LCollectionEntity
import com.client.xvideos.l.featured.saved.LCollectionSortOrder
import com.client.xvideos.l.featured.saved.LSmartCollectionCandidate
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.composeunstyled.Text
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber
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

        val selectedCollection = savedL.collection.currentCollectionName

        BackHandler(enabled = selectedCollection != null) {
            Timber.i("iii BackHandler SavedCollectionTab")
            savedL.collection.currentCollectionName = null
        }

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
            AlertDialog(
                onDismissRequest = { itemPendingAction = null },
                title = {
                    Text(
                        "Действие с коллекцией",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Theme.L.textColor
                    )
                },
                text = {
                    Column {
                        Text(pending, fontSize = 16.sp, color = Theme.L.grey2)
                        DropdownMenuItem(
                            text = { Text("Переименовать", style = Theme.L.Type.menuItem) },
                            onClick = {
                                renameValue = pending
                                itemPendingRename = pending
                                itemPendingAction = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить коллекцию", style = Theme.L.Type.menuItem.copy(color = Theme.L.red)) },
                            onClick = {
                                itemPendingDelete = pending
                                itemPendingAction = null
                            }
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { itemPendingAction = null }
                    ) { Text("Отмена", style = Theme.L.Type.button.copy(color = Theme.L.primaryColor)) }
                },
                containerColor = Theme.L.grey5,
                titleContentColor = Theme.L.textColor,
                textContentColor = Theme.L.textColor
            )
        }

        itemPendingRename?.let { pending ->
            AlertDialog(
                onDismissRequest = { itemPendingRename = null },
                title = {
                    Text(
                        "Переименовать коллекцию",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Theme.L.textColor
                    )
                },
                text = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        singleLine = true,
                        textStyle = Theme.L.Type.body
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (savedL.collection.renameCollection(pending, renameValue)) {
                                itemPendingRename = null
                            }
                        }
                    ) { Text("Сохранить", style = Theme.L.Type.button.copy(color = Theme.L.primaryColor)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { itemPendingRename = null }
                    ) { Text("Отмена", style = Theme.L.Type.button.copy(color = Theme.L.primaryColor)) }
                },
                containerColor = Theme.L.grey5,
                titleContentColor = Theme.L.textColor,
                textContentColor = Theme.L.textColor
            )
        }

        /* ---------- Диалог подтверждения ---------- */
        itemPendingDelete?.let { pending ->
            AlertDialog(

                onDismissRequest = { itemPendingDelete = null },

                title = {
                    Text(
                        "Удалить коллекцию?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Theme.L.textColor
                    )
                },

                text = {
                    Text(buildAnnotatedString {
                        append("Удалить «")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(pending) }
                        append("» из коллекции")
                    }, fontSize = 16.sp, color = Theme.L.textColor)
                },

                confirmButton = {
                    TextButton(
                        onClick = {
                            savedL.collection.deleteCollection(pending)
                            itemPendingDelete = null
                        }
                    ) { Text("Удалить", style = Theme.L.Type.button.copy(color = Theme.L.red)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { itemPendingDelete = null }
                    ) { Text("Отмена", style = Theme.L.Type.button.copy(color = Theme.L.primaryColor)) }
                },

                containerColor = Theme.L.grey5,
                titleContentColor = Theme.L.textColor,
                textContentColor = Theme.L.textColor
            )
        }

        L_SavedCollectionTabContent(
            selectedCollection = selectedCollection,
            collectionList = savedL.collection.collectionList,
            sortOrder = savedL.collection.sortOrder,
            gridState = vm.gridState,
            onSortOrderClick = { savedL.collection.applySortOrder(it) },
            onSmartCollectionsClick = { smartCollectionsVisible = true },
            onCollectionClick = { savedL.collection.setCollection(it) },
            onCollectionLongClick = { itemPendingAction = it },
            onCreateNewCollectionClick = { savedL.collection.visibleDialogCreateNew = true },
            navigationContent = {
                if (selectedCollection != null) {
                    L_CollectionNameContent(
                        collectionName = selectedCollection,
                        savedL = savedL,
                        host = vm.hostFor(selectedCollection)
                    )
                }
            }
        )
    }
}

@Composable
fun L_SavedCollectionTabContent(
    selectedCollection: String?,
    collectionList: List<LCollectionEntity>,
    sortOrder: LCollectionSortOrder,
    gridState: LazyGridState,
    onSortOrderClick: (LCollectionSortOrder) -> Unit,
    onSmartCollectionsClick: () -> Unit,
    onCollectionClick: (String) -> Unit,
    onCollectionLongClick: (String) -> Unit,
    onCreateNewCollectionClick: () -> Unit,
    navigationContent: @Composable () -> Unit
) {
    CollectionsGrid(
        selectedCollection = selectedCollection,
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
            itemNameColor = androidx.compose.ui.graphics.Color.White,
            itemSecondaryColor = androidx.compose.ui.graphics.Color.LightGray,
            itemFontFamily = Theme.L.fontFamilyDMsanss,
            addButtonBackground = Theme.L.primaryColor
        ),
        onCollectionClick = onCollectionClick,
        onCollectionLongClick = onCollectionLongClick,
        onCreateNewCollectionClick = onCreateNewCollectionClick,
        topBar = {
            LCollectionsTopBar(
                selectedCollection = selectedCollection,
                sortOrder = sortOrder,
                onSortOrderClick = onSortOrderClick,
                onSmartCollectionsClick = onSmartCollectionsClick
            )
        },
        navigationContent = navigationContent
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.background)
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                ">Коллекция>${selectedCollection.orEmpty()}",
                color = Theme.L.primaryColor,
                fontSize = 18.sp,
                fontFamily = Theme.L.fontFamilyPopinsRegular
            )
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
                Icon(Icons.Default.Add, contentDescription = null, tint = Theme.L.primaryColor)
                Spacer(Modifier.width(4.dp))
                Text("Smart", color = Theme.L.primaryColor, style = Theme.L.Type.button)
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Theme.L.textColor)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = Theme.L.grey5
                ) {
                    LCollectionSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    order.title,
                                    style = Theme.L.Type.menuItem.copy(
                                        color = if (order == sortOrder) Theme.L.primaryColor else Theme.L.textColor
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
}

@Composable
private fun LSmartCollectionsDialog(
    candidates: List<LSmartCollectionCandidate>,
    onDismiss: () -> Unit,
    onCreate: (LSmartCollectionCandidate) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Smart collections",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Theme.L.textColor
            )
        },
        text = {
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
                                Text(candidate.title, color = Color.White, style = Theme.L.Type.rowTitle)
                                Text(candidate.subtitle, color = Theme.L.grey2, style = Theme.L.Type.rowSubtitle)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", style = Theme.L.Type.button.copy(color = Theme.L.primaryColor))
            }
        },
        containerColor = Theme.L.grey5,
        titleContentColor = Theme.L.textColor,
        textContentColor = Theme.L.textColor
    )
}


class ScreenSavedCollectionSM @Inject constructor(
    val savedL: SavedL,
) : ScreenModel {

    val gridState = LazyGridState()
    private val collectionHosts = mutableMapOf<String, LazyRowPictureDetailsHost>()

    fun hostFor(collectionName: String): LazyRowPictureDetailsHost {
        return collectionHosts.getOrPut(collectionName) {
            LazyRowPictureDetailsHost(collectionName)
        }
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
