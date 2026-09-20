package com.client.xvideos.r.ui.explorer.tab.saved.tab

import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.theme.LavenderDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.P2pSendSource
import com.client.xvideos.common.p2p.ui.ScreenP2pSend
import com.client.xvideos.common.collectionDB.model.CollectionEntity
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.URL1
import com.client.xvideos.r.ui.explorer.tab.saved.tab.collection.ScreenCollectionName
import com.client.xvideos.ui.theme.XvideosTheme
import com.composeunstyled.Text
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

object R_Screen_CollectionTab : Screen {

    private fun readResolve(): Any = R_Screen_CollectionTab

    override val key: ScreenKey = "R_Screen_CollectionTab"

    @Suppress("LongMethod")
    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenSavedCollectionSM>()

        val navigator = LocalNavigator.currentOrThrow

        val savedRed = vm.savedRed

        val selectedCollection = savedRed.collections.selectedCollection.collectAsStateWithLifecycle().value

        BackHandler(enabled = selectedCollection != null) {
            Timber.d("BackHandler SavedCollectionTab")
            savedRed.collections.selectedCollection.value = null
        }

        // Открытый диалог и набранный текст переживают пересоздание
        // композиции: на remember пересоздание Activity молча закрывало их.
        var itemPendingAction by rememberSaveable { mutableStateOf<String?>(null) }
        var itemPendingRename by rememberSaveable { mutableStateOf<String?>(null) }
        var renameValue by rememberSaveable { mutableStateOf("") }
        var itemPendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

        val scope = rememberCoroutineScope()
        BackHandler(
            enabled = selectedCollection == null &&
                (itemPendingAction != null || itemPendingRename != null || itemPendingDelete != null)
        ) {
            itemPendingAction = null
            itemPendingRename = null
            itemPendingDelete = null
        }
        BackHandler(
            enabled = selectedCollection == null &&
                itemPendingAction == null && itemPendingRename == null && itemPendingDelete == null &&
                vm.gridState.firstVisibleItemIndex > 0
        ) {
            scope.launch { vm.gridState.animateScrollToItem(0) }
        }

        fun coverOf(name: String): String? =
            savedRed.collections.collectionList
                .firstOrNull { it.collection == name }
                ?.items?.lastOrNull()?.urls?.thumbnail

        // ---------- Меню действий (long-press) ----------
        itemPendingAction?.let { pending ->
            LavenderDialog(
                title = "Действие с коллекцией",
                onDismiss = { itemPendingAction = null },
                icon = { CollectionCoverIcon(coverOf(pending)) },
                content = {
                    androidx.compose.material3.Text(
                        pending,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text("Переименовать", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                        onClick = {
                            renameValue = pending
                            itemPendingRename = pending
                            itemPendingAction = null
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text("Поделиться (P2P)", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                        onClick = {
                            itemPendingAction = null
                            navigator.push(ScreenP2pSend(P2pSendSource.ShareCollectionR(pending)))
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text("Удалить коллекцию", style = Theme.L.Type.menuItem.copy(color = Color.White)) },
                        onClick = {
                            itemPendingDelete = pending
                            itemPendingAction = null
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Theme.DialogLavande.buttonBackground) }
                    )
                },
            )
        }

        // ---------- Переименование ----------
        itemPendingRename?.let { pending ->
            LavenderDialog(
                title = "Переименовать коллекцию",
                onDismiss = { itemPendingRename = null },
                icon = { CollectionCoverIcon(coverOf(pending)) },
                content = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { androidx.compose.material3.Text("Название коллекции") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Theme.DialogLavande.buttonBackground,
                            unfocusedBorderColor = Color(0x66FFFFFF),
                            focusedLabelColor = Theme.DialogLavande.dismissTextColor,
                            unfocusedLabelColor = Theme.DialogLavande.bodyColor,
                        ),
                    )
                },
                confirmText = "Сохранить",
                onConfirm = {
                    val targetName = renameValue
                    itemPendingRename = null
                    vm.renameCollection(pending, targetName)
                },
            )
        }

        // ---------- Удаление ----------
        itemPendingDelete?.let { pending ->
            LavenderDialog(
                title = "Удалить коллекцию?",
                onDismiss = { itemPendingDelete = null },
                icon = { CollectionCoverIcon(coverOf(pending)) },
                body = buildAnnotatedString {
                    append("Удалить «")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(pending) }
                    append("» из коллекции")
                },
                confirmText = "Удалить",
                onConfirm = {
                    itemPendingDelete = null
                    vm.deleteCollection(pending)
                },
                destructive = true,
            )
        }

        R_SavedCollectionTabContent(
            selectedCollection = selectedCollection,
            collectionList = savedRed.collections.collectionList,
            gridState = vm.gridState,
            onCollectionClick = { savedRed.collections.selectedCollection.value = it },
            onCollectionLongClick = { itemPendingAction = it },
            onCreateNewCollectionClick = { savedRed.collections.visibleDialogCreateNew = true },
            navigationContent = {
                if (selectedCollection != null) {
                    Navigator(ScreenCollectionName(selectedCollection))
                }
            }
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
    navigationContent: @Composable () -> Unit
) {
    com.client.xvideos.common.collectionDB.ui.CollectionsGrid(
        selectedCollection = selectedCollection,
        collections = collectionList.map {
            com.client.xvideos.common.collectionDB.model.CollectionGridItem(
                name = it.collection,
                previewUrl = it.items.lastOrNull()?.urls?.thumbnail,
                itemsCount = null
            )
        },
        gridState = gridState,
        style = com.client.xvideos.common.collectionDB.model.CollectionsGridStyle(
            backgroundColor = Color.Transparent,
            titleColor = Theme.R.colorYellow,
            titleFontFamily = Theme.R.fontFamilyPopinsRegular,
            itemNameColor = Color.White,
            itemSecondaryColor = Color.LightGray,
            itemFontFamily = Theme.R.fontFamilyDMsanss,
            addButtonBackground = Theme.R.colorYellow
        ),
        onCollectionClick = onCollectionClick,
        onCollectionLongClick = onCollectionLongClick,
        onCreateNewCollectionClick = onCreateNewCollectionClick,
        navigationContent = navigationContent
    )
}

@Composable
private fun CollectionCoverIcon(coverUrl: String?) {
    val size = Theme.DialogLavande.iconSize
    if (coverUrl != null) {
        UrlImage(url = coverUrl, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(size))
    } else {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).size(size).background(Color.Gray))
    }
}


@Stable
class ScreenSavedCollectionSM @Inject constructor(
    val block: BlockRed,
    val savedRed: SavedRed,
) : ScreenModel {

    val gridState = LazyGridState()

    /**
     * Переименование коллекции на пуле IO.
     */
    fun renameCollection(oldName: String, newName: String) {
        savedRed.scope.launch(Dispatchers.IO) {
            savedRed.collections.renameCollection(oldName, newName)
        }
    }

    /**
     * Рекурсивное удаление коллекции на пуле IO.
     */
    fun deleteCollection(name: String) {
        savedRed.scope.launch(Dispatchers.IO) {
            savedRed.collections.deleteCollection(name)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedSavedCollection {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenSavedCollectionSM::class)
    abstract fun bindScreenRedSavedCollectionScreenModel(hiltListScreenModel: ScreenSavedCollectionSM): ScreenModel
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun R_SavedCollectionTabPreview() {
    XvideosTheme(darkTheme = true) {
        val sampleCollections = listOf(
            CollectionEntity(
                collection = "Favorites",
                items = listOf(GifsInfo(id = "id", urls = URL1(thumbnail = "")))
            ),
            CollectionEntity(
                collection = "Private",
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
