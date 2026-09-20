package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum

/**
 * Экран подписанных альбомов пользователя с сервера Luscious.
 * Расположен в графе Savable (L_SavedTab).
 * Автоматически использует user_id текущей авторизованной сессии.
 */
object L_ScreenSubscribedAlbumsTab : Screen {

    override val key: ScreenKey = "L_ScreenSubscribedAlbumsTab"

    private fun readResolve(): Any = L_ScreenSubscribedAlbumsTab

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLSubscribedAlbumsSM = getScreenModel()
        val haptic = LocalHapticFeedback.current

        val albums by vm.albums.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
        val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()

        val state = vm.state
        val topInset = getTopInsetDp()
        val pullToRefreshState = rememberPullToRefreshState()

        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(state, itemsToIgnore = 1)

        // Пагинация: автоматическая подгрузка следующей страницы при приближении к концу списка
        val shouldLoadMore by remember {
            derivedStateOf {
                val totalItems = state.layoutInfo.totalItemsCount
                val lastVisibleItem = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 1 && lastVisibleItem >= totalItems - 4
            }
        }

        val canLoadMore = vm.hasMore && !isLoading && errorMessage == null
        LaunchedEffect(shouldLoadMore, canLoadMore) {
            if (shouldLoadMore && canLoadMore) {
                vm.loadNextPage()
            }
        }

        var itemPendingServerUnlike by remember { mutableStateOf<AlbumDetails?>(null) }
        val scope = rememberCoroutineScope()

        BackHandler(enabled = itemPendingServerUnlike != null) {
            itemPendingServerUnlike = null
        }
        BackHandler(enabled = itemPendingServerUnlike == null && state.firstVisibleItemIndex > 0) {
            scope.launch { state.animateScrollToItem(0) }
        }

        itemPendingServerUnlike?.let { pending ->
            SubscribedAlbumUnlikeDialog(
                album = pending,
                onConfirm = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    vm.unlikeAlbum(pending)
                    itemPendingServerUnlike = null
                },
                onDismiss = { itemPendingServerUnlike = null }
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                vm.refresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.background),
            state = pullToRefreshState,
            indicator = {
                Indicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topInset),
                    isRefreshing = isRefreshing,
                    containerColor = Theme.tabLevel1,
                    color = Theme.L.red,
                    state = pullToRefreshState
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (albums.isEmpty() && !isLoading && !isRefreshing) {
                    SubscribedAlbumsEmptyOrErrorState(
                        topInset = topInset,
                        errorMessage = errorMessage,
                        onRetry = { vm.loadInitial() },
                        onRefresh = { vm.refresh() }
                    )
                } else {
                    SubscribedAlbumsGrid(
                        state = state,
                        albums = albums,
                        topInset = topInset,
                        isLoading = isLoading,
                        onAlbumClick = { albumId ->
                            if (albumId != null) {
                                navigator.push(ScreenLAlbum(albumId))
                            } else {
                                SnackBar.error("Не удалось открыть альбом: пустой id")
                            }
                        },
                        onAlbumLongClick = { item ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            itemPendingServerUnlike = item
                        }
                    )

                    // Скроллбар
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterEnd)
                            .width(2.dp)
                    ) {
                        VerticalScrollbar { scrollPercent.value }
                    }
                }

                if (isLoading && albums.isEmpty() && !isRefreshing) {
                    CircularProgressIndicator(
                        color = Theme.L.red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscribedAlbumUnlikeDialog(
    album: AlbumDetails,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить альбом с сервера?") },
        text = { Text("Удалить «${album.title}» из подписок на сервере Luscious?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun SubscribedAlbumsEmptyOrErrorState(
    topInset: androidx.compose.ui.unit.Dp,
    errorMessage: String?,
    onRetry: () -> Unit,
    onRefresh: () -> Unit
) {
    if (errorMessage != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .width(64.dp)
                    .height(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Не удалось загрузить подписки",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Text("Повторить")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Subscriptions,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .width(64.dp)
                    .height(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Нет подписанных альбомов",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Здесь отображаются альбомы, на которые вы подписаны в Luscious",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRefresh) {
                Text("Обновить")
            }
        }
    }
}

@Composable
private fun SubscribedAlbumsGrid(
    state: androidx.compose.foundation.lazy.grid.LazyGridState,
    albums: List<AlbumDetails>,
    topInset: androidx.compose.ui.unit.Dp,
    isLoading: Boolean,
    onAlbumClick: (Long?) -> Unit,
    onAlbumLongClick: (AlbumDetails) -> Unit
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(modifier = Modifier.height(topInset))
        }

        itemsIndexed(albums, key = { index, item -> "${item.id}#$index" }) { _, item ->
            val albumId = item.id.toLongOrNull()
            AlbumListItem(
                title = item.title,
                coverUrl = item.cover?.url.orEmpty(),
                numberOfAnimatedPictures = item.number_of_animated_pictures,
                numberOfPictures = item.number_of_pictures,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                onLongClick = { onAlbumLongClick(item) }
            ) {
                onAlbumClick(albumId)
            }
        }

        if (isLoading && albums.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Theme.L.red)
                }
            }
        }
    }
}
