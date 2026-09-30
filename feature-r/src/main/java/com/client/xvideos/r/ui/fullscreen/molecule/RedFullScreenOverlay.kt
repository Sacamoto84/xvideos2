package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideo
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideoTags
import com.client.xvideos.r.ui.explorer.LocalRNavigationState
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreenSM
import com.client.xvideos.r.ui.fullscreen.atom.UserAvatarWithBadges
import com.client.xvideos.r.ui.profile.ScreenRedProfile

@Composable
fun RedFullScreenOverlay(
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator,
    downloadList: List<GifsInfo>,
    haptic: () -> Unit,
    onBack: () -> Unit = { navigator.pop() }
) {
    val navigationState = LocalRNavigationState.current
    val user = remember(item.userName) { UsersRed.listAllUsers.firstOrNull { it.username == item.userName } }
    val isInCollection = remember(vm.savedRed.collections.collectionList, item.id) {
        vm.savedRed.collections.collectionList.any { collection -> collection.items.any { it.id == item.id } }
    }
    val isCreatorFollowed = remember(vm.savedRed.creators.list, item.userName) {
        vm.savedRed.creators.list.any { it.username == item.userName }
    }
    val isLiked = remember(vm.savedRed.likes.list, item.id) {
        vm.savedRed.likes.list.any { it.id == item.id }
    }
    val isDownloaded = remember(downloadList, item.id) {
        downloadList.any { it.id == item.id }
    }

    RedFullScreenOverlay(
        item = item,
        profileImageUrl = user?.profileImageUrl,
        isInCollection = isInCollection,
        isCreatorFollowed = isCreatorFollowed,
        isLiked = isLiked,
        isDownloaded = isDownloaded,
        onAvatarClick = { navigator.push(ScreenRedProfile(item.userName)) },
        onBack = onBack,
        onToggleRotate = {
            vm.autoRotate = !vm.autoRotate
            haptic()
        },
        onTagClick = { tag ->
            vm.search.searchText.value = TextFieldValue(text = tag, selection = TextRange(tag.length))
            vm.search.searchTextDone.value = tag
            navigationState.rootTab = 0
            navigator.pop()
        },
        actionsMenu = {
            ExpandMenuVideo(
                item = item,
                modifier = Modifier,
                onClick = {},
                haptic = haptic,
                onRunLike = {},
                onRefresh = {},
                isCollection = false,
                block = { vm.block },
                redApi = { vm.redApi },
                savedRed = { vm.savedRed },
                downloadRed = { vm.downloadRed }
            )
        }
    )
}

@Composable
fun RedFullScreenOverlay(
    item: GifsInfo,
    profileImageUrl: String?,
    isInCollection: Boolean,
    isCreatorFollowed: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onAvatarClick: () -> Unit,
    onBack: () -> Unit,
    onToggleRotate: () -> Unit,
    onTagClick: (String) -> Unit,
    actionsMenu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            UserAvatarWithBadges(
                profileImageUrl = profileImageUrl,
                isInCollection = isInCollection,
                isCreatorFollowed = isCreatorFollowed,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                onClick = onAvatarClick
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleRotate) {
                Icon(
                    Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            ExpandMenuVideoTags(
                item = item,
                modifier = Modifier,
                onClick = onTagClick,
            )

            actionsMenu()
        }
    }
}

@Preview
@Composable
private fun RedFullScreenOverlayPreview() {
    RedFullScreenOverlay(
        item = GifsInfo(id = "test", userName = "TestCreator"),
        profileImageUrl = null,
        isInCollection = false,
        isCreatorFollowed = true,
        isLiked = true,
        isDownloaded = false,
        onAvatarClick = {},
        onBack = {},
        onToggleRotate = {},
        onTagClick = {},
        actionsMenu = {}
    )
}
