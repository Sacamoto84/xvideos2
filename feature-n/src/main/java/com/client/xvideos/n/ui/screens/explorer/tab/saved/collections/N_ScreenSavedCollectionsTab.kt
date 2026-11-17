package com.client.xvideos.n.ui.screens.explorer.tab.saved.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.atom.NCollectionTopBar
import com.client.xvideos.n.ui.molecule.NPictureGrid
import com.client.xvideos.n.ui.screens.picture.N_FullScreenPicture

fun N_ScreenSavedCollectionsTab_AddColumn() {
    ColumnSelect_AddColumn(Settings.n_collectionTab_column_current_count, Settings.n_collectionTab_G_0_4)
}

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

        BackHandler(
            enabled = selectedCollection != null,
            onBack = { vm.closeCollection() }
        )

        val onPictureClick: (NPicture) -> Unit = remember(navigator, currentItems) {
            { picture ->
                val index = currentItems.indexOfFirst { it.id == picture.id }.coerceAtLeast(0)
                navigator.push(N_FullScreenPicture(pictures = currentItems.toList(), initialIndex = index))
            }
        }

        val onPictureDelete: (NPicture) -> Unit = remember(vm) {
            { picture -> vm.removeItem(picture) }
        }

        N_ScreenSavedCollectionsTabContent(
            collections = collections,
            selectedCollection = selectedCollection,
            currentItems = currentItems,
            columns = columns,
            onCollectionClick = { vm.openCollection(it) },
            onCollectionDelete = { vm.deleteCollection(it) },
            onCreateClick = { vm.isCreateDialogOpen = true },
            onBackFromCollection = { vm.closeCollection() },
            onPictureClick = onPictureClick,
            onPictureDelete = onPictureDelete
        )

        DialogNewCollection(
            visible = vm.isCreateDialogOpen,
            onDismiss = { vm.isCreateDialogOpen = false },
            onBlockConfirmed = { name ->
                vm.createCollection(name)
                vm.isCreateDialogOpen = false
            }
        )
    }
}

@Composable
fun N_ScreenSavedCollectionsTabContent(
    collections: List<CollectionEntity<NPicture>>,
    selectedCollection: String?,
    currentItems: List<NPicture>,
    columns: Int,
    onCollectionClick: (String) -> Unit,
    onCollectionDelete: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBackFromCollection: () -> Unit,
    onPictureClick: (NPicture) -> Unit,
    onPictureDelete: (NPicture) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val topInset = getTopInsetDp()

    val gridItems by remember(collections) {
        derivedStateOf {
            collections.map {
                CollectionGridItem(
                    name = it.collection,
                    previewUrl = it.items.firstOrNull()?.thumbnailUrl,
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
            addButtonBackground = Color(0xFFED2553)
        )
    }

    CollectionsGrid(
        selectedCollection = selectedCollection,
        collections = gridItems,
        gridState = gridState,
        style = gridStyle,
        onCollectionClick = onCollectionClick,
        onCollectionLongClick = onCollectionDelete,
        onCreateNewCollectionClick = onCreateClick,
        navigationContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Theme.background)
            ) {
                if (selectedCollection != null) {
                    NCollectionTopBar(
                        collectionName = selectedCollection,
                        itemsCount = currentItems.size,
                        onBack = onBackFromCollection,
                        onDeleteCollection = { onCollectionDelete(selectedCollection) },
                        topInset = topInset
                    )
                }

                if (currentItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "В этой коллекции пока нет картинок",
                            color = Color(0xFFAAAAAA),
                            fontSize = 15.sp
                        )
                    }
                } else {
                    NPictureGrid(
                        items = currentItems,
                        columns = columns,
                        onItemClick = onPictureClick,
                        onItemDelete = onPictureDelete,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
        onCollectionClick = {},
        onCollectionDelete = {},
        onCreateClick = {},
        onBackFromCollection = {},
        onPictureClick = {},
        onPictureDelete = {}
    )
}
