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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionTypeFilter
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.lCollectionVisibleItems
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun L_CollectionNameContent(
    collectionName: String,
    savedL: SavedL,
    modifier: Modifier = Modifier,
    host: LazyRowPictureDetailsHost = remember(collectionName) { LazyRowPictureDetailsHost(collectionName) },
    onExitCollection: (() -> Unit)? = null
) {
    val collectionItemsState = savedL.collection.getCollectionItems(collectionName)
        .collectAsStateWithLifecycle()
    val collectionItems = collectionItemsState.value

    // Отбор и порядок — на время, пока коллекция открыта.
    var typeFilter by rememberSaveable(collectionName) { mutableStateOf(LCollectionTypeFilter.ALL) }
    var newestFirst by rememberSaveable(collectionName) { mutableStateOf(true) }

    LaunchedEffect(host, collectionItems, typeFilter, newestFirst) {
        host.replaceFilteredPictures(lCollectionVisibleItems(collectionItems ?: emptyList(), typeFilter, newestFirst))
    }

    // Новый отбор или порядок — с начала списка: на прежней позиции оказались бы
    // другие элементы. Прокрутка — только по нажатию, не при возврате на экран.
    val scope = rememberCoroutineScope()
    val onTypeChange: (LCollectionTypeFilter) -> Unit = remember(host, scope) {
        { type ->
            if (type != typeFilter) {
                typeFilter = type
                scope.launch { host.state.scrollToItem(0) }
            }
        }
    }
    val onNewestFirstChange: (Boolean) -> Unit = remember(host, scope) {
        { value ->
            if (value != newestFirst) {
                newestFirst = value
                scope.launch { host.state.scrollToItem(0) }
            }
        }
    }
    val filterBar: @Composable () -> Unit = remember(typeFilter, newestFirst, onTypeChange, onNewestFirstChange) {
        {
            LCollectionFilterBar(
                typeFilter = typeFilter,
                newestFirst = newestFirst,
                onTypeChange = onTypeChange,
                onNewestFirstChange = onNewestFirstChange
            )
        }
    }

    val selectedCollection = savedL.collection.currentCollectionName

    val handleExit: () -> Unit = remember(onExitCollection, savedL) {
        {
            Timber.d("BackHandler SavedCollectionTab")
            onExitCollection?.invoke() ?: savedL.collection.exitCollection()
        }
    }

    BackHandler(onBack = handleExit)

    val columnSelect by Settings.l_collectionTab_column_current_count.field.collectAsStateWithLifecycle()

    // Изменение количества отображаемых элементов
    LaunchedEffect(columnSelect) { host.columns = columnSelect }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LCollectionDetailTopBar(
                collectionName = selectedCollection ?: collectionName,
                onExitCollection = handleExit
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            L_LazyRowPictureDetails(
                host = host,
                itemBefore = filterBar,
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
        onExitCollection = {}
    )
}
