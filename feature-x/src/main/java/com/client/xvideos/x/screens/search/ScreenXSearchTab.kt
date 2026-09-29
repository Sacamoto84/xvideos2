package com.client.xvideos.x.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.channel.ScreenX_Channel
import com.client.xvideos.x.screens.dashboards.DashboardsPaginatedListContent
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import com.client.xvideos.x.search.model.Channel
import com.client.xvideos.x.search.model.Keyword
import com.client.xvideos.x.search.model.Pornstar
import com.client.xvideos.x.search.model.SearchResult
import kotlinx.collections.immutable.toImmutableList

/**
 * Автономный Voyager-экран вкладки поиска.
 */
class ScreenXSearchTab : Screen {
    override val key: ScreenKey = "ScreenXSearchTab"

    @Composable
    override fun Content() {
        val vm: ScreenXSearchSM = getScreenModel()
        val navigator = LocalNavigator.currentOrThrow
        X_SearchContent(
            vm = vm,
            onOpenVideoPlayer = { item -> navigator.push(ScreenX_VideoPlayer(normalizeXUrl(item.href), item)) },
            onOpenChannel = { slug, isModel -> navigator.push(ScreenX_Channel(slug = slug, isModel = isModel)) }
        )
    }
}

/**
 * Контент вкладки поиска раздела X.
 *
 * Отображает поле ввода, анимированные подсказки автодополнения (фразы, авторы, каналы),
 * локальную историю предыдущих запросов и пагинированную сетку найденных видеороликов.
 *
 * @param vm [ScreenXSearchSM] модели экрана поиска.
 * @param onOpenVideoPlayer Колбэк открытия видеоплеера по карточке ролика.
 * @param onOpenChannel Колбэк перехода на экран профиля автора или студии.
 */
@Composable
fun X_SearchContent(
    vm: ScreenXSearchSM,
    onOpenVideoPlayer: (ItemsX) -> Unit,
    onOpenChannel: (slug: String, isModel: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val uiMode by vm.uiMode.collectAsStateWithLifecycle()
    val isSuggestLoading by vm.isSuggestLoading.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val isVideoLoading by vm.isVideoLoading.collectAsStateWithLifecycle()
    val videoItems by vm.videoItems.collectAsStateWithLifecycle()
    val isSearchError by vm.isSearchError.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    val onSearchKeyword: (String) -> Unit = remember(vm, focusManager) {
        { keyword ->
            focusManager.clearFocus()
            vm.searchKeyword(keyword)
        }
    }

    val onQueryChange: (String) -> Unit = remember(vm) {
        { newQuery -> vm.updateQuery(newQuery) }
    }

    val onClearQuery: () -> Unit = remember(vm) {
        { vm.clearQuery() }
    }

    val onBackClick: () -> Unit = remember(vm) {
        { vm.onBackPress() }
    }

    val onModelClick: (Pornstar) -> Unit = remember(onOpenChannel) {
        { star ->
            val slug = star.profilePath.trim().removePrefix("/").removePrefix("profiles/").removePrefix("channels/")
            if (slug.isNotBlank()) {
                onOpenChannel(slug, true)
            }
        }
    }

    val onChannelClick: (Channel) -> Unit = remember(onOpenChannel) {
        { channel ->
            val slug = channel.profilePath.trim().removePrefix("/").removePrefix("profiles/").removePrefix("channels/")
            if (slug.isNotBlank()) {
                onOpenChannel(slug, false)
            }
        }
    }

    BackHandler(enabled = uiMode == SearchUiMode.RESULTS || query.isNotEmpty()) {
        vm.onBackPress()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1B))
    ) {
        SearchTopBar(
            query = query,
            uiMode = uiMode,
            isLoading = isSuggestLoading || isVideoLoading,
            onQueryChange = onQueryChange,
            onSearch = onSearchKeyword,
            onClear = onClearQuery,
            onBack = onBackClick
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (uiMode) {
                SearchUiMode.HISTORY -> {
                    SearchHistoryView(
                        history = history,
                        onSelect = onSearchKeyword,
                        onDelete = { vm.deleteHistoryItem(it) },
                        onClearAll = { vm.clearAllHistory() }
                    )
                }
                SearchUiMode.SUGGESTIONS -> {
                    SearchSuggestionsView(
                        suggestions = suggestions,
                        isLoading = isSuggestLoading,
                        onSelectKeyword = onSearchKeyword,
                        onSelectModel = onModelClick,
                        onSelectChannel = onChannelClick
                    )
                }
                SearchUiMode.RESULTS -> {
                    SearchResultsView(
                        items = videoItems,
                        isLoading = isVideoLoading,
                        isError = isSearchError,
                        onRetry = { vm.retrySearch() },
                        isFavorite = { vm.isFavorite(it) },
                        onFavoriteAdd = { vm.addFavorite(it) },
                        onFavoriteRemove = { vm.removeFavorite(it) },
                        onDownload = { vm.download(it) },
                        onSaveToGallery = { vm.saveToGallery(it) },
                        openVideoPlayer = onOpenVideoPlayer
                    )
                }
            }
        }
    }
}

/**
 * Верхняя строка поиска X с кнопками действий и индикатором загрузки.
 */
@Composable
private fun SearchTopBar(
    query: String,
    uiMode: SearchUiMode,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .background(Color(0xFF222222))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск видео, моделей или каналов...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = {
                if (uiMode == SearchUiMode.RESULTS) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад к подсказкам",
                            tint = Color(0xFFFF9900)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск",
                        tint = Color.Gray
                    )
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = Color.Gray
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2B2B2B),
                unfocusedContainerColor = Color(0xFF282828),
                focusedBorderColor = Color(0xFFFF9900),
                unfocusedBorderColor = Color(0xFF383838),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFFF9900)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(top = 4.dp),
                color = Color(0xFFFF9900),
                trackColor = Color(0xFF333333)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/**
 * Отображение локальной истории поиска до начала ввода текста.
 */
@Composable
private fun SearchHistoryView(
    history: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Введите запрос для поиска видео, моделей или каналов",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "История поиска",
                        color = Color(0xFFB0B0B0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "Очистить всё",
                            color = Color(0xFFFF9900),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(history, key = { it }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item,
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = { onDelete(item) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Удалить",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
            }
        }
    }
}

/**
 * Отображение структурированных подсказок (фразы, модели, каналы).
 */
@Composable
private fun SearchSuggestionsView(
    suggestions: SearchResult,
    isLoading: Boolean,
    onSelectKeyword: (String) -> Unit,
    onSelectModel: (Pornstar) -> Unit,
    onSelectChannel: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val keywords = suggestions.resolvedKeywords
    val models = suggestions.resolvedPornstars
    val channels = suggestions.resolvedChannels

    if (suggestions.isEmpty && !isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Подсказок не найдено",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Поисковые фразы
            if (keywords.isNotEmpty()) {
                item {
                    SectionHeader(title = "Категории и фразы")
                }
                items(keywords, key = { "kw_${it.name}" }) { kw ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectKeyword(kw.name) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFFF9900),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = kw.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (kw.hasRating) {
                            Text(
                                text = kw.rating,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFF252525), thickness = 0.5.dp)
                }
            }

            // 2. Модели и актрисы
            if (models.isNotEmpty()) {
                item {
                    SectionHeader(title = "Модели")
                }
                items(models, key = { "md_${it.name}_${it.profilePath}" }) { star ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectModel(star) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UrlImage(
                            url = star.avatarUrl,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF333333))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = star.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitleParts = buildList {
                                if (star.hasVideos) add("${star.formatVideos()} видео")
                                if (star.hasValidSubscribers) add("${star.subscribers} подп.")
                            }
                            if (subtitleParts.isNotEmpty()) {
                                Text(
                                    text = subtitleParts.joinToString(" • "),
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF252525), thickness = 0.5.dp)
                }
            }

            // 3. Каналы и студии
            if (channels.isNotEmpty()) {
                item {
                    SectionHeader(title = "Каналы")
                }
                items(channels, key = { "ch_${it.name}_${it.profilePath}" }) { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectChannel(channel) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UrlImage(
                            url = channel.avatarUrl,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF333333))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (channel.isVerified) {
                                    Text(
                                        text = " ✓",
                                        color = Color(0xFFFF9900),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            if (channel.hasSubscribers) {
                                Text(
                                    text = "${channel.subscribers} подписчиков",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF252525), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFFFF9900),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF202020))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

/**
 * Отображение результатов поиска видеороликов.
 */
@Composable
private fun SearchResultsView(
    items: List<ItemsX>,
    isLoading: Boolean,
    isError: Boolean,
    onRetry: () -> Unit,
    isFavorite: (Long) -> Boolean,
    onFavoriteAdd: (ItemsX) -> Unit,
    onFavoriteRemove: (ItemsX) -> Unit,
    onDownload: (ItemsX) -> Unit,
    onSaveToGallery: (ItemsX) -> Unit,
    openVideoPlayer: (ItemsX) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Не удалось загрузить результаты поиска",
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900))
                    ) {
                        Text("Повторить", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items.isEmpty() && !isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "По данному запросу ничего не найдено",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                DashboardsPaginatedListContent(
                    items = items.toImmutableList(),
                    isFavorite = isFavorite,
                    onFavoriteAdd = onFavoriteAdd,
                    onFavoriteRemove = onFavoriteRemove,
                    onDownload = onDownload,
                    openVideoPlayer = openVideoPlayer,
                    onSaveToGallery = onSaveToGallery,
                    contentPadding = PaddingValues(top = 2.dp, bottom = 4.dp)
                )
            }
        }
    }
}
