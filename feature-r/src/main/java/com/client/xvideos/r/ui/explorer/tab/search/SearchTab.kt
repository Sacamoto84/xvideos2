package com.client.xvideos.r.ui.explorer.tab.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.common.util.runCatchingCancellable
import com.client.xvideos.common.util.toPrettyCount
import timber.log.Timber
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemNichesResponse
import com.client.xvideos.r.model.search.SearchItemTagsResponse
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.r.ui.profile.ScreenRedProfile

object SearchTab : Screen {

    private fun readResolve(): Any = SearchTab

    override val key: ScreenKey = "RedSearchTab"

    @Composable
    override fun Content() {
        val vm: ScreenRedExplorerSearchSM = getScreenModel()
        val navigator = LocalNavigator.currentOrThrow

        val searchText = vm.searchText.collectAsStateWithLifecycle().value
        val isLoading = vm.isLoading.collectAsStateWithLifecycle().value

        SearchTabContent(
            searchText = searchText,
            isLoading = isLoading,
            onSearchTextChange = { vm.searchText.value = it },
            creatorsList = vm.creatorsList,
            onCreatorClick = { handle ->
                navigator.push(ScreenRedProfile(handle))
            }
        )
    }

}

@Composable
fun SearchTabContent(
    searchText: String,
    isLoading: Boolean,
    onSearchTextChange: (String) -> Unit,
    creatorsList: List<SearchItemCreatorsResponse>,
    onCreatorClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = searchText.isNotEmpty()) {
        onSearchTextChange("")
    }
    BackHandler(enabled = searchText.isEmpty() && listState.firstVisibleItemIndex > 0) {
        coroutineScope.launch { listState.animateScrollToItem(0) }
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск авторов...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Поиск", tint = Color.Gray)
                    },
                    trailingIcon = {
                        if (searchText.isNotBlank()) {
                            IconButton(onClick = { onSearchTextChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = Color(0xFFE5A00D)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (creatorsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (searchText.isNotBlank() && !isLoading) {
                    Text("Ничего не найдено", color = Color.Gray)
                } else if (searchText.isBlank()) {
                    Text("Введите имя автора для поиска", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(creatorsList, key = { index, item -> "${item.text}_${item.name}_$index" }) { _, item ->
                    val handle = item.text.removePrefix("@").ifBlank { item.name }
                    SearchCreatorItem(item = item, onClick = { onCreatorClick(handle) })
                }
            }
        }
    }
}

@Composable
fun SearchCreatorItem(
    item: SearchItemCreatorsResponse,
    onClick: () -> Unit
) {
    val handle = item.text.removePrefix("@").ifBlank { item.name }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val image = item.image
        if (!image.isNullOrBlank()) {
            UrlImage(
                image,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(56.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(56.dp)
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name.ifBlank { handle },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                if (item.verified) {
                    Text(
                        text = " ✓",
                        color = Color(0xFFE5A00D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                text = "@$handle",
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
            if (item.followers > 0) {
                Text(
                    text = "Подписчиков: ${item.followers.toPrettyCount()}",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFF303030)
@Composable
fun SearchTabPreview() {
    SearchTabContent(
        searchText = "Ana",
        isLoading = false,
        onSearchTextChange = {},
        creatorsList = listOf(
            SearchItemCreatorsResponse(
                name = "Ana",
                image = null,
                followers = 1234
            ),
            SearchItemCreatorsResponse(
                name = "Elf Sandi",
                image = "https://userpic.redgifs.com/5/3f/53f9367f4b1d523a032f5fa2475de70d.png",
                followers = 274
            )
        ),
        onCreatorClick = {}
    )
}

@Stable
class ScreenRedExplorerSearchSM @Inject constructor(
    val redApi: RedApi
) : ScreenModel {

    val searchText = MutableStateFlow<String>("")
    val isLoading = MutableStateFlow(false)

    val creatorsList = mutableStateListOf<SearchItemCreatorsResponse>()
    val nichesList = mutableStateListOf<SearchItemNichesResponse>()
    val tagsList = mutableStateListOf<SearchItemTagsResponse>()

    init {
        screenModelScope.launch {
            @OptIn(FlowPreview::class)
            searchText
                .debounce(300)
                .collectLatest { rawText ->
                    val text = rawText.trim()
                    if (text.isBlank()) {
                        creatorsList.clear()
                        isLoading.value = false
                        return@collectLatest
                    }

                    isLoading.value = true
                    try {
                        runCatchingCancellable { redApi.search.searchCreatorsShort(text).getOrThrow() }
                            .onSuccess { creatorsList.replaceWith(it.items) }
                            .onFailure { Timber.w(it, "Поиск авторов не удался: %s", text) }
                    } finally {
                        isLoading.value = false
                    }
                }
        }
    }

}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleRedExplorerSearch {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedExplorerSearchSM::class)
    abstract fun bindScreenRedExplorerSearchSreenModel(hiltListScreenModel: ScreenRedExplorerSearchSM): ScreenModel
}
