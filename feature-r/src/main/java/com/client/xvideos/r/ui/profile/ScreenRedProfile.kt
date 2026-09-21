package com.client.xvideos.r.ui.profile

import com.client.xvideos.common.theme.Theme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.common.ui.atom.VerticalScrollbar
import com.client.xvideos.common.ui.scroll.rememberVisibleRangePercentIgnoringFirstNForGrid
import com.client.xvideos.r.ui.profile.atom.RedProfileCreaterInfo
import com.client.xvideos.r.ui.profile.tags.TagsBlock
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123

class ScreenRedProfile(val profileName: String) : Screen {

    override val key: ScreenKey = "RedProfile:$profileName"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<ScreenRedProfileSM, ScreenRedProfileSM.Factory> { factory ->
            factory.create(profileName)
        }

        val isLoading by vm.isLoading.collectAsStateWithLifecycle()

        val tags by vm.tags.collectAsStateWithLifecycle()

        val tagsSelect by vm.tagsSelect.collectAsStateWithLifecycle()

        // Навигация «Назад»:
        // 1. Сброс выбранных тегов фильтрации (если есть).
        // 2. Выход из профиля.
        BackHandler(enabled = tagsSelect.isNotEmpty()) {
            vm.tagsSelect.value = emptySet()
        }

        // Расчет процентов для скролл.
        // Без `by`: см. VerticalScrollbar — чтение позиции скролла здесь
        // перекомпоновывало бы весь экран на каждом кадре прокрутки.
        val scrollPercent = rememberVisibleRangePercentIgnoringFirstNForGrid(
            gridState = vm.likedHost.state, itemsToIgnore = 3, numberOfColumns = 2
        )

        val tagsList = remember(tags) { tags.toList() }
        val tagsSelectList = remember(tagsSelect) { tagsSelect.toList() }

        RedProfileScreenContent(
            profileName = profileName,
            creator = vm.creator,
            tags = tagsList,
            tagsSelect = tagsSelectList,
            isLoading = isLoading,
            scrollPercent = { scrollPercent.value },
            likedHost = vm.likedHost,
            onTagClick = { vm.toggleSelectTag(it) },
            onAppendLoaded = { pager ->
                pager.itemSnapshotList.items.forEach { gifItem ->
                    vm.tagsAdd(gifItem.tags)
                }
            },
            savedRedProvider = { vm.savedRed },
            onBack = {
                if (tagsSelect.isNotEmpty()) {
                    vm.tagsSelect.value = emptySet()
                } else {
                    navigator.pop()
                }
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RedProfileScreenContent(
    profileName: String = "",
    creator: UserInfo?,
    tags: List<String>,
    tagsSelect: List<String>,
    isLoading: Boolean,
    scrollPercent: () -> Pair<Float, Float>,
    likedHost: com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host,
    onTagClick: (String) -> Unit,
    onAppendLoaded: (androidx.paging.compose.LazyPagingItems<com.client.xvideos.r.model.GifsInfo>) -> Unit,
    savedRedProvider: () -> com.client.xvideos.r.common.saved.SavedRed,
    onBack: () -> Unit = {}
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme.background)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = creator?.name?.ifBlank { creator.username } ?: profileName,
                    color = Color.White,
                    fontFamily = Theme.R.fontFamilyPopinsMedium,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        containerColor = Theme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Theme.background)
        ) {
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

            // Индикатор загрузки
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 8.dp
                    )
                }
            }

            // Скролл
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

