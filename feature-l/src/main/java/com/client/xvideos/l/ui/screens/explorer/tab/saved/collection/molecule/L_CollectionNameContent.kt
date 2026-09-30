package com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.matchesCollectionSearch
import timber.log.Timber

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                tag = "lCollection",
                isCollection = true
            )
        }
    }
}

@Preview
@Composable
private fun L_CollectionNameContentPreview() {
    LCollectionDetailTopBar(
        collectionName = "Favorites",
        searchQuery = "",
        searchVisible = false,
        onSearchChange = {},
        onToggleSearch = {},
        onExitCollection = {}
    )
}
