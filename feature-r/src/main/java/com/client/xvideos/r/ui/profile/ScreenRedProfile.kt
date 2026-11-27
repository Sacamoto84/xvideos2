package com.client.xvideos.r.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.translation.TranslationEngine
import com.client.xvideos.common.translation.rememberEnglishToRussianTranslation
import com.client.xvideos.common.ui.adaptive.adaptiveContentWidth
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.model.UserInfo
import com.client.xvideos.r.ui.profile.molecule.RedProfileCreaterInfo
import com.client.xvideos.r.ui.profile.molecule.TagsBlock
import com.client.xvideos.r.ui.ui.MetadataLoadState
import com.client.xvideos.r.ui.ui.atom.MetadataLoadingContent
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.r.ui.ui.lazyrow123.PageableFeed
import com.client.xvideos.r.ui.ui.sortByOrder.RSortOrderIcon
import com.client.xvideos.r.ui.ui.sortByOrder.SortByOrder
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

        val metadata = vm.metadata
        if (metadata !is MetadataLoadState.Ready) {
            MetadataLoadingContent(
                errorMessage = (metadata as? MetadataLoadState.Failed)?.message,
                onRetry = vm::retryMetadata,
            )
            return
        }

        val creator = metadata.value
        val tags by vm.tags.collectAsStateWithLifecycle()

        val tagsSelect by vm.tagsSelect.collectAsStateWithLifecycle()
        val translationEngine by Settings.translation_engine.field.collectAsStateWithLifecycle()

        val onResetTags: () -> Unit = remember(vm) { { vm.resetSelectedTags() } }

        // Навигация «Назад»:
        // 1. Сброс выбранных тегов фильтрации (если есть).
        // 2. Выход из профиля.
        BackHandler(enabled = tagsSelect.isNotEmpty(), onBack = onResetTags)

        val tagsList = remember(tags) { tags.toImmutableList() }
        val tagsSelectList = remember(tagsSelect) { tagsSelect.toImmutableList() }

        val onTagClick: (String) -> Unit = remember(vm) { { vm.toggleSelectTag(it) } }
        val onAppendLoaded: (LazyPagingItems<GifsInfo>) -> Unit = remember(vm) {
            { pager ->
                pager.itemSnapshotList.items.forEach { gifItem ->
                    vm.tagsAdd(gifItem.tags)
                }
            }
        }
        // creators.list — SnapshotStateList: ключом remember не годится (сравнение по ссылке),
        // подписку на изменения списка даёт только derivedStateOf.
        val isFollow by remember(vm, creator.username) {
            derivedStateOf { vm.savedRed.creators.contains(creator.username) }
        }
        val onFollowClick: () -> Unit = remember(isFollow, creator, vm) {
            {
                if (isFollow) vm.savedRed.creators.remove(creator.username) else vm.savedRed.creators.add(creator)
            }
        }

        ScreenRedProfileContent(
            creator = creator,
            tags = tagsList,
            tagsSelect = tagsSelectList,
            likedHost = vm.likedHost,
            sortOrders = vm.orderList,
            onTagClick = onTagClick,
            onAppendLoaded = onAppendLoaded,
            isFollow = isFollow,
            onFollowClick = onFollowClick,
            translationEngine = TranslationEngine.fromStoredValue(translationEngine)
        )
    }

}

@Composable
fun ScreenRedProfileContent(
    creator: UserInfo,
    tags: ImmutableList<String>,
    tagsSelect: ImmutableList<String>,
    likedHost: LazyRow123Host,
    sortOrders: List<Order>,
    onTagClick: (String) -> Unit,
    onAppendLoaded: (LazyPagingItems<GifsInfo>) -> Unit,
    isFollow: Boolean,
    onFollowClick: () -> Unit,
    translationEngine: TranslationEngine = TranslationEngine.NONE
) {
    val currentSort by likedHost.sortType.collectAsStateWithLifecycle()
    val topInset = getTopInsetDp()
    val originalAbout = remember(creator.description) {
        creator.description?.trimMargin()?.takeIf { it.isNotBlank() }
    }
    val about = originalAbout?.let { rememberEnglishToRussianTranslation(it, translationEngine) }

    PageableFeed(
        host = likedHost,
        onAppendLoaded = onAppendLoaded,
        header = { scrollMode ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(topInset))

                RedProfileCreaterInfo(creator, isFollow = isFollow, onFollowClick = onFollowClick, about = about)

                if (tags.isNotEmpty()) {
                    TagsBlock(tags = tags, tagsSelect = tagsSelect, modifier = Modifier.adaptiveContentWidth(), onClick = onTagClick)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier
                        .adaptiveContentWidth()
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        SortByOrder(
                            list = sortOrders,
                            selected = currentSort,
                            onSelect = likedHost::changeSortType,
                            containerColor = Theme.tabLevel0,
                            orderIcon = { RSortOrderIcon(it) },
                            showSelectedLabel = true,
                        )
                    }
                    scrollMode()
                }
            }
        },
    )
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
    }
}
