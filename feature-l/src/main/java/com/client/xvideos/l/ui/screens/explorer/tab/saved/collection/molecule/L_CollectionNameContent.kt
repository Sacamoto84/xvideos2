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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionItemsOrder
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.LCollectionTypeFilter
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.lCollectionVisibleItems
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.model.LCollectionViewState
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Открытая коллекция L.
 *
 * @param view Сетка, отбор и порядок коллекции из ScreenModel.
 */
@Composable
fun L_CollectionNameContent(
    view: LCollectionViewState,
    savedL: SavedL,
    modifier: Modifier = Modifier,
    onExitCollection: (() -> Unit)? = null
) {
    val collectionName = view.collectionName
    val host = view.host
    val collectionItemsState = savedL.collection.getCollectionItems(collectionName)
        .collectAsStateWithLifecycle()
    val collectionItems = collectionItemsState.value

    // Отбор и порядок — на время, пока коллекция открыта.
    val typeFilter = view.typeFilter
    val order = view.order

    LaunchedEffect(host, collectionItems, typeFilter, order) {
        host.replaceFilteredPictures(lCollectionVisibleItems(collectionItems ?: emptyList(), typeFilter, order))
    }

    // Новый отбор или порядок — с начала списка: на прежней позиции оказались бы
    // другие элементы. Прокрутка — только по нажатию, не при возврате на экран.
    val scope = rememberCoroutineScope()
    val onTypeChange: (LCollectionTypeFilter) -> Unit = remember(view, scope) {
        { type ->
            if (type != view.typeFilter) {
                view.typeFilter = type
                scope.launch { view.host.state.scrollToItem(0) }
            }
        }
    }
    val onOrderChange: (LCollectionItemsOrder) -> Unit = remember(view, scope) {
        { value ->
            if (value != view.order) {
                view.order = value
                scope.launch { view.host.state.scrollToItem(0) }
            }
        }
    }
    val filterBar: @Composable () -> Unit = remember(typeFilter, order, onTypeChange, onOrderChange) {
        {
            LCollectionFilterBar(
                typeFilter = typeFilter,
                order = order,
                onTypeChange = onTypeChange,
                onOrderChange = onOrderChange
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
