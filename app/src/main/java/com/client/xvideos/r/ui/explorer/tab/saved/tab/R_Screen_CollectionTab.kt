package com.client.xvideos.r.ui.explorer.tab.saved.tab

import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.theme.LavenderDialog

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

        /**  ➜ сюда запоминаем элемент, который пользователь хочет удалить  */
        var itemPendingDelete by remember { mutableStateOf<String?>(null) }
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
            onCollectionLongClick = { itemPendingDelete = it },
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
