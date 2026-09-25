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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val TITLE_DIALOG_ACTION = "Действие с коллекцией"
private const val TEXT_RENAME = "Переименовать"
private const val TEXT_SHARE = "Поделиться (P2P)"
private const val TEXT_DELETE = "Удалить коллекцию"
private const val TITLE_RENAME = "Переименовать коллекцию"
private const val LABEL_COLLECTION_NAME = "Название коллекции"
private const val BUTTON_SAVE = "Сохранить"
private const val BUTTON_DELETE = "Удалить"
private const val TITLE_DELETE = "Удалить коллекцию?"
private const val TEXT_DELETE_PREFIX = "Удалить «"
private const val TEXT_DELETE_SUFFIX = "» из коллекции"
private val TEXT_FIELD_BORDER_ALPHA_COLOR = Color(0x66FFFFFF)
private val COVER_CORNER_RADIUS = 8.dp
private val COVER_ICON_SHAPE = RoundedCornerShape(COVER_CORNER_RADIUS)
private val SPAN_STYLE_BOLD = SpanStyle(fontWeight = FontWeight.Bold)
private val TEXT_FIELD_MODIFIER = Modifier.fillMaxWidth()

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

        // Открытый диалог и набранный текст переживают пересоздание
        // композиции: на remember пересоздание Activity молча закрывало их.
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
        val onDismissAction: () -> Unit = remember { { itemPendingAction = null } }
        val onDismissRename: () -> Unit = remember { { itemPendingRename = null } }
        val onDismissDelete: () -> Unit = remember { { itemPendingDelete = null } }
        val onRenameValueChange: (String) -> Unit = remember { { text -> renameValue = text } }

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
                navigator.push(ScreenP2pSend(P2pSendSource.ShareCollectionR(pending)))
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
            onDismissAction = onDismissAction,
            onDismissRename = onDismissRename,
            onDismissDelete = onDismissDelete,
            onRenameValueChange = onRenameValueChange,
            onRenameAction = onRenameAction,
            onShareAction = onShareAction,
            onDeleteAction = onDeleteAction,
            onConfirmRename = onConfirmRename,
            onConfirmDelete = onConfirmDelete,
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

@androidx.compose.runtime.Immutable
private data class R_CollectionDialogData(
    val itemPendingAction: String?,
    val itemPendingRename: String?,
    val itemPendingDelete: String?,
    val renameValue: String,
)

@Composable
private fun R_CollectionDialogsHost(
    dialogData: R_CollectionDialogData,
    coverOf: (String) -> String?,
    onDismissAction: () -> Unit,
    onDismissRename: () -> Unit,
    onDismissDelete: () -> Unit,
    onRenameValueChange: (String) -> Unit,
    onRenameAction: (String) -> Unit,
    onShareAction: (String) -> Unit,
    onDeleteAction: (String) -> Unit,
    onConfirmRename: (String, String) -> Unit,
    onConfirmDelete: (String) -> Unit,
) {
    // ---------- Меню действий (long-press) ----------
    dialogData.itemPendingAction?.let { pending ->
        val onRenameClick = remember(pending, onRenameAction) { { onRenameAction(pending) } }
        val onShareClick = remember(pending, onShareAction) { { onShareAction(pending) } }
        val onDeleteClick = remember(pending, onDeleteAction) { { onDeleteAction(pending) } }
        val menuItemStyle = remember { Theme.L.Type.menuItem.copy(color = Color.White) }
        val iconTint = Theme.DialogLavande.buttonBackground
        LavenderDialog(
            title = TITLE_DIALOG_ACTION,
            onDismiss = onDismissAction,
            icon = { CollectionCoverIcon(coverOf(pending)) },
            content = {
                androidx.compose.material3.Text(
                    pending,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                DropdownMenuItem(
                    text = { androidx.compose.material3.Text(TEXT_RENAME, style = menuItemStyle) },
                    onClick = onRenameClick,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = iconTint) }
                )
                DropdownMenuItem(
                    text = { androidx.compose.material3.Text(TEXT_SHARE, style = menuItemStyle) },
                    onClick = onShareClick,
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = iconTint) }
                )
                DropdownMenuItem(
                    text = { androidx.compose.material3.Text(TEXT_DELETE, style = menuItemStyle) },
                    onClick = onDeleteClick,
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = iconTint) }
                )
            },
        )
    }

    // ---------- Переименование ----------
    dialogData.itemPendingRename?.let { pending ->
        val onConfirm = remember(pending, dialogData.renameValue, onConfirmRename) {
            { onConfirmRename(pending, dialogData.renameValue) }
        }
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedBorderColor = Theme.DialogLavande.buttonBackground,
            unfocusedBorderColor = TEXT_FIELD_BORDER_ALPHA_COLOR,
            focusedLabelColor = Theme.DialogLavande.dismissTextColor,
            unfocusedLabelColor = Theme.DialogLavande.bodyColor,
        )
        LavenderDialog(
            title = TITLE_RENAME,
            onDismiss = onDismissRename,
            icon = { CollectionCoverIcon(coverOf(pending)) },
            content = {
                OutlinedTextField(
                    value = dialogData.renameValue,
                    onValueChange = onRenameValueChange,
                    singleLine = true,
                    modifier = TEXT_FIELD_MODIFIER,
                    label = { androidx.compose.material3.Text(LABEL_COLLECTION_NAME) },
                    colors = textFieldColors,
                )
            },
            confirmText = BUTTON_SAVE,
            onConfirm = onConfirm,
        )
    }

    // ---------- Удаление ----------
    dialogData.itemPendingDelete?.let { pending ->
        val onConfirm = remember(pending, onConfirmDelete) {
            { onConfirmDelete(pending) }
        }
        val dialogBody = remember(pending) {
            buildAnnotatedString {
                append(TEXT_DELETE_PREFIX)
                withStyle(SPAN_STYLE_BOLD) { append(pending) }
                append(TEXT_DELETE_SUFFIX)
            }
        }
        LavenderDialog(
            title = TITLE_DELETE,
            onDismiss = onDismissDelete,
            icon = { CollectionCoverIcon(coverOf(pending)) },
            body = dialogBody,
            confirmText = BUTTON_DELETE,
            onConfirm = onConfirm,
            destructive = true,
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
            com.client.xvideos.common.collectionDB.model.CollectionGridItem(
                name = it.collection,
                previewUrl = it.items.lastOrNull()?.urls?.thumbnail,
                itemsCount = null
            )
        }
    }
    val gridStyle = remember {
        com.client.xvideos.common.collectionDB.model.CollectionsGridStyle(
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
        com.client.xvideos.common.collectionDB.ui.CollectionsGrid(
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

@Composable
private fun CollectionCoverIcon(
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    val size = Theme.DialogLavande.iconSize
    val coverModifier = remember(size) { Modifier.clip(COVER_ICON_SHAPE).size(size) }
    val imageModifier = if (modifier == Modifier) coverModifier else modifier.then(coverModifier)
    if (coverUrl != null) {
        UrlImage(url = coverUrl, modifier = imageModifier)
    } else {
        Box(imageModifier.background(Color.Gray))
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
