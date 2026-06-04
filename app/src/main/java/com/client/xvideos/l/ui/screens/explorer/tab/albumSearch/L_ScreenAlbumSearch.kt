package com.client.xvideos.l.ui.screens.explorer.tab.albumSearch

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.model.Landing_page_albumSection
import com.client.xvideos.l.model.Landing_page_albumType
import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import com.client.xvideos.l.net.Luscious
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.l.ui.element.AlbumListItem
import com.client.xvideos.l.ui.screens.screenAlbum.ScreenLAlbum
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


object L_ScreenAlbumSearch : Screen {

    override val key: ScreenKey = uniqueScreenKey

    private fun readResolve(): Any = L_ScreenAlbumSearch

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenLAlbumSearchSM = getScreenModel()
        val keyboard = LocalSoftwareKeyboardController.current

        val searchText = vm.searchText.collectAsState().value
        val result = vm.result.collectAsState().value
        val isLoading = vm.isLoading.collectAsState().value
        val sections = result?.sections

        Scaffold(
            containerColor = ThemeL.greyBackground,
            modifier = Modifier.fillMaxSize()
        ) {

            LazyColumn(state = vm.state, modifier = Modifier.fillMaxSize()) {

                item {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { vm.searchText.value = it },
                        modifier = Modifier
                            .displayCutoutPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        singleLine = true,
                        label = { Text("Search") },
                        textStyle = ThemeL.Type.body.copy(color = ThemeL.textColor),
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { vm.searchText.value = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = ThemeL.textColor)
                                }
                            } else {
                                IconButton(onClick = { vm.search(); keyboard?.hide() }) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = ThemeL.textColor)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.search(); keyboard?.hide() })
                    )
                }

                item {
                    if (result?.title != null) {
                        Text(
                            result.title,
                            color = ThemeL.textColor,
                            fontSize = 32.sp,
                            fontFamily = ThemeL.fontFamilyKarla,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }

                items(sections?.size ?: 0) { index ->
                    val section = sections?.get(index) ?: return@items

                    Text(
                        section.title,
                        color = ThemeL.textColor,
                        fontSize = 24.sp,
                        fontFamily = ThemeL.fontFamilyKarla,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp)
                    )

                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

                    FlowRow(
                        maxItemsInEachRow = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val itemWidth = (screenWidth - 8.dp) / 3
                        section.items.take(9).forEach { album ->
                            Box(
                                modifier = Modifier
                                    .width(itemWidth)
                                    .padding(vertical = 2.dp)
                            ) {
                                AlbumListItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    title = album.title,
                                    coverUrl = album.cover.url,
                                    numberOfAnimatedPictures = album.numberOfAnimatedPictures,
                                    numberOfPictures = album.numberOfPictures,
                                    onClick = { navigator.push(ScreenLAlbum(album.id.toLong())) }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth()
                            .height(40.dp)
                            .border(2.dp, ThemeL.grey3, RoundedCornerShape(8.dp))
                            .clickable(onClick = {
                                navigator.push(
                                    L_ScreenAlbumList.create(
                                        filter = vm.createFilter(section),
                                        title = "Search: ${vm.searchText.value}"
                                    )
                                )
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "See All >",
                            color = ThemeL.textColor,
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp,
                            fontFamily = ThemeL.fontFamilyKarla,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
    }
}


class ScreenLAlbumSearchSM @Inject constructor(
    val luscious: Luscious
) : ScreenModel {

    val state = LazyListState()

    val searchText = MutableStateFlow("")
    val result = MutableStateFlow<Landing_page_albumType?>(null)
    val isLoading = MutableStateFlow(false)

    fun search() {
        val query = searchText.value.trim()
        if (query.isBlank()) return
        screenModelScope.launch {
            isLoading.value = true
            result.value = luscious.getLandingPageAlbumSearch(query).getOrElse {
                Timber.e(it, "!!! eee ScreenLAlbumSearchSM search")
                null
            }
            isLoading.value = false
        }
    }

    fun createFilter(section: Landing_page_albumSection): AlbumListFilter {
        val albumType = when (section.title) {
            "Manga" -> AlbumType.Manga
            "Picture Sets" -> AlbumType.Pictures
            else -> AlbumType.Pictures
        }

        return AlbumListFilter(
            display = "search_score",
            album_type = albumType,
            content_id = ContentId.All,
            searchQuery = searchText.value.trim()
        )
    }

    override fun onDispose() {
        super.onDispose()
        Timber.i("iii ScreenLAlbumSearchSM onDispose")
    }
}


@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleLAlbumSearch {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenLAlbumSearchSM::class)
    abstract fun bindHiltSearchScreenModelFactory(hiltListScreenModel: ScreenLAlbumSearchSM): ScreenModel
}
