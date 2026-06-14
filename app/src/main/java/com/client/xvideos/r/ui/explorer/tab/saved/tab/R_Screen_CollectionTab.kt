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

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
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
import kotlinx.coroutines.DelicateCoroutinesApi
import timber.log.Timber
import javax.inject.Inject

object R_Screen_CollectionTab : Screen {

    private fun readResolve(): Any = R_Screen_CollectionTab

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenSavedCollectionSM>()

        val savedRed = vm.savedRed

        val selectedCollection = savedRed.collections.selectedCollection.collectAsStateWithLifecycle().value

        BackHandler(enabled = selectedCollection != null) {
            Timber.i("iii BackHandler SavedCollectionTab")
            savedRed.collections.selectedCollection.value = null
        }

        var itemPendingAction by remember { mutableStateOf<String?>(null) }
        var itemPendingRename by remember { mutableStateOf<String?>(null) }
        var renameValue by remember { mutableStateOf("") }
        var itemPendingDelete by remember { mutableStateOf<String?>(null) }

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
                    androidx.compose.material3.Text(pending, fontSize = 16.sp, color = Theme.L.b0)
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text("Переименовать", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = {
                            renameValue = pending
                            itemPendingRename = pending
                            itemPendingAction = null
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Theme.L.DialogLavande.buttonBackground) }
                    )
                    DropdownMenuItem(
                        enabled = false,
                        text = { androidx.compose.material3.Text("Поделиться (P2P) — скоро", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = { },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Theme.L.DialogLavande.buttonBackground) }
                    )
                    DropdownMenuItem(
                        text = { androidx.compose.material3.Text("Удалить коллекцию", style = Theme.L.Type.menuItem.copy(color = Color.Black)) },
                        onClick = {
                            itemPendingDelete = pending
                            itemPendingAction = null
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Theme.L.DialogLavande.buttonBackground) }
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
                            focusedTextColor = Theme.L.DialogLavande.buttonBackground,
                            unfocusedTextColor = Theme.L.DialogLavande.buttonBackground,
                            cursorColor = Theme.L.DialogLavande.buttonBackground,
                            focusedBorderColor = Theme.L.DialogLavande.buttonBackground,
                            unfocusedBorderColor = Theme.L.DialogLavande.buttonBackground,
                            focusedLabelColor = Theme.L.DialogLavande.buttonBackground,
                            unfocusedLabelColor = Theme.L.DialogLavande.buttonBackground,
                        ),
                    )
                },
                confirmText = "Сохранить",
                onConfirm = {
                    if (savedRed.collections.renameCollection(pending, renameValue)) {
                        itemPendingRename = null
                    }
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
                    savedRed.collections.deleteCollection(pending)
                    itemPendingDelete = null
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
    val size = Theme.L.DialogLavande.iconSize
    if (coverUrl != null) {
        UrlImage(url = coverUrl, modifier = Modifier.clip(RoundedCornerShape(8.dp)).size(size))
    } else {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).size(size).background(Color.Gray))
    }
}


class ScreenSavedCollectionSM @Inject constructor(
    val block: BlockRed,
    val savedRed: SavedRed,
) : ScreenModel {

    val gridState = LazyGridState()
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
