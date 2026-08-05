package com.client.xvideos.r.ui.profile

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.r.ui.profile.atom.RedProfileCreaterInfo
import com.client.xvideos.r.ui.profile.tags.TagsBlock
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import timber.log.Timber

class ScreenRedProfile(val profileName: String) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenRedProfileSM, ScreenRedProfileSM.Factory> { factory ->
            factory.create(profileName)
        }

        val isLoading by vm.isLoading.collectAsState()

        val tags by vm.tags.collectAsStateWithLifecycle()

        val tagsSelect by vm.tagsSelect.collectAsStateWithLifecycle()

        // Расчет процентов для скролл.
        // Без `by`: см. VerticalScrollbar — чтение позиции скролла здесь
        // перекомпоновывало бы весь экран на каждом кадре прокрутки.
        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(
            gridState = vm.likedHost.state, itemsToIgnore = 3, numberOfColumns = 2
        )





        RedProfileScreenContent(
            creator = vm.creator,
            tags = tags.toList(),
            tagsSelect = tagsSelect.toList(),
            isLoading = isLoading,
            scrollPercent = { scrollPercent.value },
            likedHost = vm.likedHost,
            onTagClick = { vm.toggleSelectTag(it) },
            onAppendLoaded = { pager ->
                Timber.tag("Paging")
                    .d("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!Произошла загрузка следующей страницы!")
                pager.itemSnapshotList.let { it1 ->
                    it1.items.forEach { it2 ->
                        val t = it2.tags
                        vm.tagsAdd(t)
                    }
                }
            },
            savedRedProvider = { vm.savedRed }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RedProfileScreenContent(
    creator: UserInfo?,
    tags: List<String>,
    tagsSelect: List<String>,
    isLoading: Boolean,
    scrollPercent: () -> Pair<Float, Float>,
    likedHost: com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host,
    onTagClick: (String) -> Unit,
    onAppendLoaded: (androidx.paging.compose.LazyPagingItems<com.client.xvideos.r.model.GifsInfo>) -> Unit,
    savedRedProvider: () -> com.client.xvideos.r.common.saved.SavedRed
) {

    Scaffold(containerColor = Theme.background,





        ) {







        Box(modifier = Modifier.fillMaxSize()) {

            LazyRow123(
                host = likedHost,
                modifier = Modifier.fillMaxSize(),
                contentBeforeList = {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        if (creator != null) {
                            RedProfileCreaterInfo(creator, savedRed = savedRedProvider)
                        }

                        if ((creator != null) && (tags.isNotEmpty())) {
                            TagsBlock(tags, tagsSelect, onTagClick)
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                    }
                },
                onAppendLoaded = onAppendLoaded,
            )

            //Индикатор загрузки
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 8.dp
                    )
                }
            }

            //---- Скролл ----
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .width(2.dp)
            ) { VerticalScrollbar(scrollPercent) }

        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF303030)
@Composable
fun ScreenRedProfilePreview() {
    val mockUser = UserInfo(
        name = "Sample Creator",
        username = "sample_user",
        description = "This is a sample description for the profile preview. It can be long and contain various information about the creator.",
        followers = 1234,
        gifs = 56,
        profileImageUrl = null,
        url = "https://www.redgifs.com/users/sample_user"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        RedProfileCreaterInfo(
            item = mockUser,
            isFollow = true,
            onFollowClick = {}
        )

        TagsBlock(
            tags = listOf("Outdoor", "Amateur", "Verified", "Solo", "Big Assets"),
            tagsSelect = listOf("Verified"),
            onClick = {}
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    }
}

