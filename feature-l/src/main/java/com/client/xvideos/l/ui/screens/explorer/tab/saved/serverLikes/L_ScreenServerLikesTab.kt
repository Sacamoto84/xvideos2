package com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.L_LazyRowPictureDetails

/**
 * Экран лайкнутых картинок пользователя с сервера Luscious.
 * Расположен в графе Savable (L_SavedTab).
 */
object L_ScreenServerLikesTab : Screen {

    override val key: ScreenKey = uniqueScreenKey

    private fun readResolve(): Any = L_ScreenServerLikesTab

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val vm: ScreenLServerLikesSM = getScreenModel()
        val haptic = LocalHapticFeedback.current

        val pictures by vm.pictures.collectAsStateWithLifecycle()
        val isLoading by vm.isLoading.collectAsStateWithLifecycle()
        val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
        val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()

        val column = Settings.l_likesTab_column_current_count.field.collectAsStateWithLifecycle().value
        LaunchedEffect(column) {
            if (column != 0) {
                vm.host.columns = column
            }
        }

        val topInset = getTopInsetDp()
        val pullToRefreshState = rememberPullToRefreshState()

        // Пагинация: автоматическая подгрузка следующей страницы при прокрутке к концу
        val shouldLoadMore by remember {
            derivedStateOf {
                val totalItems = vm.host.state.layoutInfo.totalItemsCount
                val lastVisibleItem = vm.host.state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 1 && lastVisibleItem >= totalItems - 4
            }
        }

        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore && vm.hasMore && !isLoading && errorMessage == null) {
                vm.loadNextPage()
            }
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
                if (pictures.isEmpty() && !isLoading && !isRefreshing) {
                    if (errorMessage != null) {
                        // Состояние ошибки (например, не авторизован)
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
                                text = "Не удалось загрузить лайки",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { vm.loadInitial() }) {
                                Text("Повторить")
                            }
                        }
                    } else {
                        // Пустое состояние
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topInset, start = 24.dp, end = 24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Нет лайкнутых картинок",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Здесь будут отображаться картинки, которые вы лайкнули на сервере Luscious",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { vm.refresh() }) {
                                Text("Обновить")
                            }
                        }
                    }
                } else {
                    L_LazyRowPictureDetails(
                        host = vm.host,
                        expandMenu = ExpandMenuType.SERVER_LIKES,
                        tag = "l_server_likes",
                        itemBefore = {
                            Box(modifier = Modifier.fillMaxWidth().height(topInset))
                        }
                    )
                }

                if (isLoading && pictures.isEmpty() && !isRefreshing) {
                    CircularProgressIndicator(
                        color = Theme.L.red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
