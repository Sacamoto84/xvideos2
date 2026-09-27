package com.client.xvideos.x.screens.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.icons.IconFavorite18
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.core.R
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.SelectedXCreator
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.channel.ScreenX_Channel
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Контент экрана подписок раздела X (вкладки «Каналы» и «Актрисы»).
 *
 * Отображает:
 * 1. Горизонтальный/мультистрочный блок авторов (чипы с аватаром и именем, tap — фильтр, long-press — отписка, tap на аватар — открытие профиля).
 * 2. Объединённую ленту свежих видео от выбранных авторов в 2 колонки.
 * 3. Меню видеороликов (избранное, загрузка, галерея).
 *
 * @param saved Фасад локальных данных X ([SavedX]).
 * @param isModel `true` для вкладки актрис/моделей, `false` для вкладки каналов.
 */
@Composable
fun X_SubscriptionsContent(
    saved: SavedX,
    isModel: Boolean,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    val selectedList = if (isModel) {
        saved.subscriptions.selectedListModels
    } else {
        saved.subscriptions.selectedListChannels
    }

    var creatorToDelete by remember { mutableStateOf<SelectedXCreator?>(null) }
    var videos by remember { mutableStateOf<List<ItemsX>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val anySelected = selectedList.any { it.isSelected }
    val anyUnselected = selectedList.any { !it.isSelected }

    // Закрытие диалога или сброс фильтров при нажатии «Назад»
    BackHandler(enabled = creatorToDelete != null) {
        creatorToDelete = null
    }
    BackHandler(enabled = creatorToDelete == null && anyUnselected && selectedList.isNotEmpty()) {
        for (i in selectedList.indices) {
            selectedList[i] = selectedList[i].copy(isSelected = true)
        }
    }

    // Загрузка ленты при изменении фильтров
    val selectionSignature = selectedList.joinToString(",") { "${it.slug}:${it.isSelected}" }
    LaunchedEffect(selectionSignature, isModel) {
        if (!anySelected) {
            videos = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        try {
            videos = saved.subscriptions.fetchAggregatedVideos(isModel)
        } catch (e: Exception) {
            Timber.e(e, "X_SubscriptionsContent: сбой загрузки ленты подписок")
        } finally {
            isLoading = false
        }
    }

    val onToggleCreator = remember(selectedList) {
        { slug: String ->
            val idx = selectedList.indexOfFirst { it.slug == slug }
            if (idx >= 0) {
                val current = selectedList[idx]
                selectedList[idx] = current.copy(isSelected = !current.isSelected)
            }
        }
    }

    val onOpenProfile = remember(navigator, isModel) {
        { slug: String ->
            navigator.push(ScreenX_Channel(slug = slug, isModel = isModel))
        }
    }

    val onOpenVideo = remember(navigator) {
        { item: ItemsX ->
            navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item))
        }
    }

    val onSelectAll = remember(selectedList) {
        {
            for (i in selectedList.indices) {
                selectedList[i] = selectedList[i].copy(isSelected = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040404))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = rememberLazyGridState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = getTopInsetDp(), bottom = 24.dp)
        ) {
            // 1. Панель авторов-подписок
            if (selectedList.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    XCreatorsHeader(
                        listCreators = selectedList,
                        isModel = isModel,
                        onCreatorClick = onToggleCreator,
                        onAvatarClick = onOpenProfile,
                        onLongClick = { creator -> creatorToDelete = creator },
                    )
                }
            }

            // 2. Индикатор загрузки
            if (isLoading) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = if (isModel) Color(0xFFDE2600) else Color(0xFF1E88E5)
                        )
                    }
                }
            } else if (selectedList.isEmpty()) {
                // 3. Нет подписок
                item(span = { GridItemSpan(2) }) {
                    SubscriptionsEmptyState(isModel = isModel)
                }
            } else if (!anySelected) {
                // 4. Все авторы отключены
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Все авторы отключены в фильтре",
                            color = Color.Gray,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onSelectAll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isModel) Color(0xFFDE2600) else Color(0xFF1E88E5)
                            )
                        ) {
                            Text("Включить всех", color = Color.White)
                        }
                    }
                }
            } else if (videos.isEmpty()) {
                // 5. Видео не найдены
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "У выбранных авторов пока нет опубликованных видео",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // 6. Сетка видеороликов
                items(
                    items = videos,
                    key = { it.id }
                ) { video ->
                    SubscriptionVideoGridCell(
                        cell = video,
                        isFavorite = saved.favorites.contains(video.id),
                        openVideoPlayer = onOpenVideo,
                        onFavoriteAdd = { saved.favorites.add(video) },
                        onFavoriteRemove = { saved.favorites.remove(video) },
                        onDownload = { saved.downloads.download(video) },
                        onSaveToGallery = { saved.downloads.saveToGallery(video) },
                        onOpenAuthor = { onOpenProfile(video.nameProfile.ifBlank { video.channel }) }
                    )
                }
            }
        }

        // Диалог подтверждения удаления
        DialogXSubscriptionDelete(
            creator = creatorToDelete,
            onDismiss = { creatorToDelete = null },
            onConfirm = { creator ->
                scope.launch {
                    if (creator.isModel) {
                        saved.subscriptions.removeModel(creator.slug)
                    } else {
                        saved.subscriptions.removeChannel(creator.slug)
                    }
                    creatorToDelete = null
                }
            }
        )
    }
}

/**
 * Блок чипов с авторами в верхней части ленты подписок.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XCreatorsHeader(
    listCreators: List<SelectedXCreator>,
    isModel: Boolean,
    onCreatorClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onLongClick: (SelectedXCreator) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listCreators.forEach { creator ->
            key(creator.slug) {
                XCreatorChip(
                    creator = creator,
                    isModel = isModel,
                    onClick = { onCreatorClick(creator.slug) },
                    onAvatarClick = { onAvatarClick(creator.slug) },
                    onLongClick = { onLongClick(creator) },
                )
            }
        }
    }
}

/**
 * Интерактивный круглый/скруглённый чип автора с аватаром.
 */
@Composable
private fun XCreatorChip(
    creator: SelectedXCreator,
    isModel: Boolean,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeBorderColor = if (isModel) Color(0xFFDE2600) else Color(0xFF1E88E5)
    val activeBgColor = if (isModel) Color(0xFF2E1216) else Color(0xFF102034)
    val inactiveBgColor = Color(0xFF16161A)
    val inactiveBorderColor = Color(0xFF333338)

    val bgColor = if (creator.isSelected) activeBgColor else inactiveBgColor
    val borderColor = if (creator.isSelected) activeBorderColor else inactiveBorderColor
    val textColor = if (creator.isSelected) Color.White else Color(0xFF888888)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 3.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватарка автора (нажатие открывает профиль)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF222226))
                .combinedClickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            val avatarUrl = creator.avatarUrl
            if (!avatarUrl.isNullOrBlank()) {
                UrlImage(url = avatarUrl, modifier = Modifier.fillMaxSize())
            } else if (isModel) {
                Text(
                    text = "\uE9B8",
                    style = TextStyle(
                        color = Color(0xFFDE2600),
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.iconfont))
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF1E88E5)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = creator.name,
            fontSize = 12.sp,
            fontWeight = if (creator.isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Карточка видео в сетке подписок.
 */
@Composable
private fun SubscriptionVideoGridCell(
    cell: ItemsX,
    isFavorite: Boolean,
    openVideoPlayer: (ItemsX) -> Unit,
    onFavoriteAdd: () -> Unit,
    onFavoriteRemove: () -> Unit,
    onDownload: () -> Unit,
    onSaveToGallery: () -> Unit,
    onOpenAuthor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(cell, openVideoPlayer) { { openVideoPlayer(cell) } }
    val durationText = remember(cell.duration) { cell.duration.trim().removeSuffix(".") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(352f / 198f)
            .padding(1.dp)
            .background(Color.DarkGray)
    ) {
        UrlVideoImageAndLongClickX(
            cell,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
        ) {
            // Длительность
            if (durationText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = durationText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // Имя автора
            val authorName = cell.channel.ifBlank { cell.nameProfile }
            if (authorName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xB3000000))
                        .combinedClickable(onClick = onOpenAuthor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Просмотры
            if (cell.views.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = cell.views,
                        fontSize = 11.sp,
                        color = Color(0xFFDDDDDD)
                    )
                }
            }

            // Иконка избранного
            if (isFavorite) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 48.dp, bottom = 4.dp)
                ) {
                    IconFavorite18()
                }
            }

            // Кнопка меню с тремя точками
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                X_DashboardExpandMenu(
                    isFavorite = isFavorite,
                    onFavoriteAdd = onFavoriteAdd,
                    onFavoriteRemove = onFavoriteRemove,
                    onDownload = onDownload,
                    onSaveToGallery = onSaveToGallery,
                )
            }
        }
    }
}

/**
 * Экран пустого состояния для подписок.
 */
@Composable
private fun SubscriptionsEmptyState(
    isModel: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = if (isModel) Icons.Outlined.Person else Icons.Outlined.Tv,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isModel) "У вас нет подписок на актрис" else "У вас нет подписок на каналы",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Перейдите на страницу ${if (isModel) "актрисы" else "канала"} и нажмите кнопку «Подписаться»",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
