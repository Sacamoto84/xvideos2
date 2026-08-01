package com.client.xvideos.r.ui.explorer.tab.search

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.connectivityObserver.ConnectivityObserver
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.model.search.SearchItemCreatorsResponse
import com.client.xvideos.r.model.search.SearchItemNichesResponse
import com.client.xvideos.r.model.search.SearchItemTagsResponse
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

object SearchTab : Screen {

    private fun readResolve(): Any = SearchTab

    override val key: ScreenKey = uniqueScreenKey

    @SuppressLint(
        "UnusedMaterialScaffoldPaddingParameter",
        "UnusedMaterial3ScaffoldPaddingParameter"
    )
    @Composable
    override fun Content() {
        val vm: ScreenRedExplorerSearchSM = getScreenModel()

        val searchText = vm.searchText.collectAsStateWithLifecycle().value

        SearchTabContent(
            searchText = searchText,
            onSearchTextChange = { vm.searchText.value = it },
            creatorsList = vm.creatorsList
        )
    }

}

@Composable
fun SearchTabContent(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    creatorsList: List<SearchItemCreatorsResponse>
) {
    Scaffold(
        modifier = Modifier,
        bottomBar = {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier.padding(8.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(creatorsList) { item ->
                SearchCreatorItem(item)
            }
        }
    }
}

@Composable
fun SearchCreatorItem(item: SearchItemCreatorsResponse) {
    Row(modifier = Modifier.border(1.dp, Color.White).padding(8.dp)) {
        if (item.image != null) {
            UrlImage(
                item.image!!, modifier = Modifier
                    .clip(CircleShape)
                    .size(64.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(64.dp)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(item.name, color = Color.White)
            Text("Followers: ${item.followers}", color = Color.White)
        }
    }
}

@Preview(backgroundColor = 0xFF303030)
@Composable
fun SearchTabPreview() {
    SearchTabContent(
        searchText = "Ana",
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
        )
    )
}

class ScreenRedExplorerSearchSM @Inject constructor(
    connectivityObserver: ConnectivityObserver,
    val redApi: RedApi
) : ScreenModel {

    val searchText = MutableStateFlow<String>("Ana")

    val creatorsList = mutableStateListOf<SearchItemCreatorsResponse>()
    val nichesList = mutableStateListOf<SearchItemNichesResponse>()
    val tagsList = mutableStateListOf<SearchItemTagsResponse>()

    init {


        screenModelScope.launch {

            searchText.collect { text ->

                SnackBar.info(text)

                if (text == "") {
                    creatorsList.clear()
                    return@collect
                }

                val creator = redApi.search.searchCreatorsShort(text).getOrThrow()

                creatorsList.replaceWith(creator.items)

            }
//            val niches = RedGifs.searchNiches("Ana")
//            nichesList.clear()
//            nichesList.addAll(niches)

//            val tags = RedGifs.searchTags("Ana")
//            tagsList.clear()
//            tagsList.addAll(tags)
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
