package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection

import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails
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
import timber.log.Timber

private val ZERO_WINDOW_INSETS = WindowInsets(0, 0, 0, 0)
private val TOP_BAR_HORIZONTAL_PADDING = 8.dp
private val TOP_BAR_VERTICAL_PADDING = 4.dp
private val TOP_BAR_TITLE_SIZE = 18.sp
private const val CD_BACK = "Назад"
private const val CD_CLOSE_SEARCH = "Закрыть поиск"
private const val CD_SEARCH_COLLECTION = "Поиск в коллекции"
private const val LABEL_SEARCH_COLLECTION = "Поиск в коллекции"
private const val TAG_L_COLLECTION = "lCollection"

class ScreenCollectionName(
    val collectionName: String,
    private val popOnBack: Boolean = false
) : Screen {

    override val key: ScreenKey = "LCollection:$collectionName:$popOnBack"

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenLCollectionNameSM, ScreenLCollectionNameSM.Factory> { factory -> factory.create(collectionName) }
        val navigator = LocalNavigator.currentOrThrow

        val onExitCollection: () -> Unit = remember(vm.savedL, popOnBack, navigator) {
            {
                vm.savedL.collection.exitCollection()
                if (popOnBack) {
                    navigator.pop().let {}
                }
            }
        }

        L_CollectionNameContent(
            collectionName = collectionName,
            savedL = vm.savedL,
            host = vm.host,
            onExitCollection = onExitCollection
        )

    }
}

@Composable
fun L_CollectionNameContent(
    collectionName: String,
    savedL: SavedL,
    modifier: Modifier = Modifier,
    host: LazyRowPictureDetailsHost = remember(collectionName) { LazyRowPictureDetailsHost(collectionName) },
    onExitCollection: (() -> Unit)? = null
) {
    val searchQuery = host.collectionSearchQuery

    val collectionItemsState = savedL.collection.getCollectionItems(collectionName)
        .collectAsStateWithLifecycle()
    val collectionItems = collectionItemsState.value

    LaunchedEffect(host, searchQuery, collectionItems) {
        val filtered = (collectionItems ?: emptyList()).filter { it.matchesCollectionSearch(searchQuery) }
        host.replaceFilteredPictures(filtered)
    }

    var searchVisible by rememberSaveable(collectionName) { mutableStateOf(false) }

    val selectedCollection = savedL.collection.currentCollectionName

    val handleExit: () -> Unit = remember(onExitCollection, savedL) {
        {
            Timber.d("BackHandler SavedCollectionTab")
            onExitCollection?.invoke() ?: savedL.collection.exitCollection()
        }
    }

    val onSearchChange: (String) -> Unit = remember(host) {
        { query -> host.collectionSearchQuery = query }
    }

    val onToggleSearch: () -> Unit = remember(host, searchVisible) {
        {
            if (searchVisible) {
                host.collectionSearchQuery = ""
                searchVisible = false
            } else {
                searchVisible = true
            }
        }
    }

    val onExitTopBar: () -> Unit = remember(searchQuery, searchVisible, host, handleExit) {
        {
            if (searchQuery.isNotEmpty()) {
                host.collectionSearchQuery = ""
            } else if (searchVisible) {
                searchVisible = false
            } else {
                handleExit()
            }
        }
    }

    val onClearSearchQuery = remember(host) { { host.collectionSearchQuery = "" } }
    val onHideSearch = remember { { searchVisible = false } }

    // Иерархия «Назад»:
    // 1. Очистить текст поискового запроса, если введен
    // 2. Скрыть поле поиска, если панель открыта
    // 3. Выйти из коллекции
    BackHandler(enabled = searchQuery.isNotEmpty(), onBack = onClearSearchQuery)
    BackHandler(enabled = searchQuery.isEmpty() && searchVisible, onBack = onHideSearch)
    BackHandler(enabled = searchQuery.isEmpty() && !searchVisible, onBack = handleExit)

    val columnSelect by Settings.l_collectionTab_column_current_count.field.collectAsStateWithLifecycle()

    // Изменение количества отображаемых элементов
    LaunchedEffect(columnSelect) { host.columns = columnSelect }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = ZERO_WINDOW_INSETS,
        topBar = {
            LCollectionDetailTopBar(
                collectionName = selectedCollection ?: collectionName,
                searchQuery = searchQuery,
                searchVisible = searchVisible,
                onSearchChange = onSearchChange,
                onToggleSearch = onToggleSearch,
                onExitCollection = onExitTopBar
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            L_LazyRowPictureDetails(
                host = host,
                expandMenu = ExpandMenuType.LIKES,
                tag = TAG_L_COLLECTION,
                isCollection = true
            )
        }
    }
}

@Composable
private fun LCollectionDetailTopBar(
    collectionName: String,
    searchQuery: String,
    searchVisible: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onExitCollection: (() -> Unit)? = null
) {
    val topInset = getTopInsetDp()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.background)
            .padding(top = topInset)
            .padding(horizontal = TOP_BAR_HORIZONTAL_PADDING, vertical = TOP_BAR_VERTICAL_PADDING)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onExitCollection != null) {
                IconButton(onClick = onExitCollection) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = CD_BACK,
                        tint = Theme.L.primaryColor
                    )
                }
            }
            Text(
                collectionName,
                modifier = Modifier.weight(1f),
                color = Theme.L.primaryColor,
                fontSize = TOP_BAR_TITLE_SIZE,
                fontFamily = Theme.L.fontFamilyPopinsRegular
            )
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (searchVisible) CD_CLOSE_SEARCH else CD_SEARCH_COLLECTION,
                    tint = Theme.L.primaryColor
                )
            }
        }

        AnimatedVisibility(searchVisible) {
            val searchTextStyle = remember { Theme.L.Type.body.copy(color = Theme.L.textColor) }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(LABEL_SEARCH_COLLECTION) },
                textStyle = searchTextStyle
            )
        }
    }
}

private val WHITESPACE_REGEX = Regex("\\s+")

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
        .split(WHITESPACE_REGEX)
        .all { it in haystack }
}


@Stable
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
        savedL.collection.getCollectionItems(collectionName)
    }

    fun delete(item: PicsDetails) {
        savedL.collection.remove(item, collectionName)
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
