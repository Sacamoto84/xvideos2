package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.featured.saved.LCollectionDuplicateGroup
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.featured.saved.lPicsDetailsIdentityKey
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

class ScreenCollectionName(
    val collectionName: String,
    private val popOnBack: Boolean = false
) : Screen {

    override val key: ScreenKey = "LCollection:$collectionName:$popOnBack"

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenLCollectionNameSM, ScreenLCollectionNameSM.Factory> { factory -> factory.create(collectionName) }
        val navigator = LocalNavigator.currentOrThrow

        L_CollectionNameContent(
            collectionName = collectionName,
            savedL = vm.savedL,
            host = vm.host,
            onExitCollection = {
                vm.savedL.collection.currentCollectionName = null
                if (popOnBack) {
                    navigator.pop()
                }
            }
        )

    }
}

@Composable
fun L_CollectionNameContent(
    collectionName: String,
    savedL: SavedL
) {
    val host = remember(collectionName) { LazyRowPictureDetailsHost(collectionName) }
    L_CollectionNameContent(
        collectionName = collectionName,
        savedL = savedL,
        host = host
    )
}

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun L_CollectionNameContent(
    collectionName: String,
    savedL: SavedL,
    host: LazyRowPictureDetailsHost,
    onExitCollection: (() -> Unit)? = null
) {
    val searchQuery = host.collectionSearchQuery
    val duplicateDialogVisible = host.collectionDuplicateDialogVisible

    LaunchedEffect(host, searchQuery) {
        snapshotFlow { savedL.collection.listUrl.toList() }
            .collectLatest { items ->
                host.replaceFilteredPictures(items.filter { it.matchesCollectionSearch(searchQuery) })
            }
    }

    val selectedCollection = savedL.collection.currentCollectionName
    val duplicateGroups = savedL.collection.duplicateGroups.toList()

    if (duplicateDialogVisible) {
        LCollectionDuplicatesDialog(
            groups = duplicateGroups,
            onDismiss = { host.collectionDuplicateDialogVisible = false },
            onRemoveDuplicates = {
                savedL.collection.removeDuplicateItems(collectionName)
                host.collectionDuplicateDialogVisible = false
            }
        )
    }

    BackHandler {
        Timber.i("iii BackHandler SavedCollectionTab")
        onExitCollection?.invoke() ?: run {
            savedL.collection.currentCollectionName = null
        }
    }

    val columnSelect = Settings.l_collectionTab_column_current_count.field.collectAsStateWithLifecycle().value

    //Изменение количества отображаемых элементов
    LaunchedEffect(columnSelect) { host.columns = columnSelect }

    Scaffold(topBar = {
        LCollectionDetailTopBar(
            collectionName = selectedCollection ?: collectionName,
            searchQuery = searchQuery,
            onSearchChange = { host.collectionSearchQuery = it },
            duplicateCount = duplicateGroups.sumOf { it.items.size - 1 },
            onDuplicatesClick = { host.collectionDuplicateDialogVisible = true }
        )
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center){
            L_LazyRowPictureDetails(
                host = host,
                expandMenu = ExpandMenuType.LIKES,
                tag = "lCollection",
                isCollection = true
            )
        }
    }
}

@Composable
private fun LCollectionDetailTopBar(
    collectionName: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    duplicateCount: Int,
    onDuplicatesClick: () -> Unit
) {
    var searchVisible by rememberSaveable(collectionName) { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ThemeL.greyBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ">Коллекция>$collectionName",
                modifier = Modifier.weight(1f),
                color = ThemeL.primaryColor,
                fontSize = 18.sp,
                fontFamily = ThemeL.fontFamilyPopinsRegular
            )
            if (duplicateCount > 0) {
                TextButton(onClick = onDuplicatesClick) {
                    Text("Дубли: $duplicateCount", color = ThemeL.primaryColor, style = ThemeL.Type.button)
                }
            }
            IconButton(onClick = { searchVisible = !searchVisible }) {
                Icon(
                    imageVector = if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = null,
                    tint = ThemeL.textColor
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = ThemeL.textColor)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = ThemeL.grey5
                ) {
                    DropdownMenuItem(
                        text = { Text("Проверить дубли", color = ThemeL.textColor, style = ThemeL.Type.menuItem) },
                        onClick = {
                            onDuplicatesClick()
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        AnimatedVisibility(searchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск в коллекции") },
                textStyle = ThemeL.Type.body.copy(color = ThemeL.textColor)
            )
        }
    }
}

@Composable
private fun LCollectionDuplicatesDialog(
    groups: List<LCollectionDuplicateGroup>,
    onDismiss: () -> Unit,
    onRemoveDuplicates: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Детектор дублей", color = ThemeL.textColor, style = ThemeL.Type.dialogTitle)
        },
        text = {
            if (groups.isEmpty()) {
                Text("Дубли не найдены", color = ThemeL.grey2, style = ThemeL.Type.body)
            } else {
                Column {
                    Text(
                        "Найдено групп: ${groups.size}. При очистке останется самый новый элемент в каждой группе.",
                        color = ThemeL.grey2,
                        style = ThemeL.Type.body
                    )
                    Spacer(Modifier.height(8.dp))
                    groups.take(6).forEach { group ->
                        Text(
                            "• ${group.items.size} элемента: ${lPicsDetailsIdentityKey(group.items.first())}",
                            color = Color.White,
                            style = ThemeL.Type.rowSubtitle
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (groups.isNotEmpty()) {
                TextButton(onClick = onRemoveDuplicates) {
                    Text("Удалить дубли", color = ThemeL.red, style = ThemeL.Type.button)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = ThemeL.primaryColor, style = ThemeL.Type.button)
            }
        },
        containerColor = ThemeL.grey5,
        titleContentColor = ThemeL.textColor,
        textContentColor = ThemeL.textColor
    )
}

private fun PicsDetails.matchesCollectionSearch(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true

    val haystack = buildList {
        add(album.orEmpty())
        add(url_to_original.orEmpty())
        add(url_to_video.orEmpty())
        thumbnails?.forEach { add(it.url.orEmpty()) }
    }.joinToString(" ").lowercase()

    return normalized
        .lowercase()
        .split(Regex("\\s+"))
        .all { it in haystack }
}


class ScreenLCollectionNameSM @AssistedInject constructor(
    @Assisted val collectionName: String,
    val savedL: SavedL,
) : ScreenModel {

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(collectionName: String): ScreenLCollectionNameSM
    }

    val host = LazyRowPictureDetailsHost(collectionName)

    init {
        ensureCollectionLoaded()
    }

    fun ensureCollectionLoaded() {
        if (savedL.collection.currentCollectionName != collectionName || savedL.collection.listUrl.isEmpty()) {
            savedL.collection.setCollection(collectionName)
        }
        syncItems(savedL.collection.listUrl.toList())
    }

    fun syncItems(items: List<PicsDetails>) {
        host.replaceFilteredPictures(items)
    }

    fun delete(item: PicsDetails) {
        savedL.collection.remove(item, collectionName)
        syncItems(savedL.collection.listUrl.toList())
    }

}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLSavedCollectionName {
    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenLCollectionNameSM.Factory::class)
    abstract fun bindScreenLSavedCollectionNameScreenModel(hiltDetailsScreenModelFactory: ScreenLCollectionNameSM.Factory): ScreenModelFactory
}
