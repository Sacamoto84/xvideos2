package com.client.xvideos.r.ui.profile

import com.client.xvideos.common.theme.Theme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.profile.atom.RedProfileCreaterInfo
import com.client.xvideos.r.ui.profile.tags.TagsBlock
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.common.util.getTopInsetDp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class ScreenRedProfile(val profileName: String) : Screen {

    override val key: ScreenKey = "RedProfile:$profileName"

    @Composable
    override fun Content() {
        val vm = getScreenModel<ScreenRedProfileSM, ScreenRedProfileSM.Factory> { factory ->
            factory.create(profileName)
        }

        val isLoading by vm.isLoading.collectAsStateWithLifecycle()

        val tags by vm.tags.collectAsStateWithLifecycle()

        val tagsSelect by vm.tagsSelect.collectAsStateWithLifecycle()

        val onResetTags: () -> Unit = remember(vm) { { vm.resetSelectedTags() } }

        // Навигация «Назад»:
        // 1. Сброс выбранных тегов фильтрации (если есть).
        // 2. Выход из профиля.
        BackHandler(enabled = tagsSelect.isNotEmpty(), onBack = onResetTags)

        val tagsList = remember(tags) { tags.toImmutableList() }
        val tagsSelectList = remember(tagsSelect) { tagsSelect.toImmutableList() }

        val onTagClick: (String) -> Unit = remember(vm) { { vm.toggleSelectTag(it) } }
        val onAppendLoaded: (androidx.paging.compose.LazyPagingItems<com.client.xvideos.r.model.GifsInfo>) -> Unit = remember(vm) {
            { pager ->
                pager.itemSnapshotList.items.forEach { gifItem ->
                    vm.tagsAdd(gifItem.tags)
                }
            }
        }
        val savedRedProvider: () -> com.client.xvideos.r.common.saved.SavedRed = remember(vm) { { vm.savedRed } }

        RedProfileScreenContent(
            creator = vm.creator,
            tags = tagsList,
            tagsSelect = tagsSelectList,
            isLoading = isLoading,
            likedHost = vm.likedHost,
            onTagClick = onTagClick,
            onAppendLoaded = onAppendLoaded,
            savedRedProvider = savedRedProvider
        )
    }

}

@Composable
fun RedProfileScreenContent(
    creator: UserInfo?,
    tags: ImmutableList<String>,
    tagsSelect: ImmutableList<String>,
    isLoading: Boolean,
    likedHost: com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host,
    onTagClick: (String) -> Unit,
    onAppendLoaded: (androidx.paging.compose.LazyPagingItems<com.client.xvideos.r.model.GifsInfo>) -> Unit,
    savedRedProvider: () -> com.client.xvideos.r.common.saved.SavedRed
) {
    val topInset = getTopInsetDp()

    val renderContentBeforeList: @Composable () -> Unit = remember(
        topInset,
        creator,
        tags,
        tagsSelect,
        onTagClick,
        savedRedProvider
    ) {
        {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(topInset))

                if (creator != null) {
                    RedProfileCreaterInfo(creator, savedRed = savedRedProvider)
                }

                if ((creator != null) && (tags.isNotEmpty())) {
                    TagsBlock(tags, tagsSelect, onTagClick)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                contentBeforeList = renderContentBeforeList,
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
