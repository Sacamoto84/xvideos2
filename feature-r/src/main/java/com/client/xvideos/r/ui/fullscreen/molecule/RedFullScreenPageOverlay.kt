package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.common.search.R_SearchExplorer
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.network.api.RedApi
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideo
import com.client.xvideos.r.ui.explorer.LocalRNavigationState
import com.client.xvideos.r.ui.fullscreen.model.RedFullScreenPlayerState
import com.client.xvideos.r.ui.profile.ScreenRedProfile

@Composable
fun RedFullScreenPageOverlay(
    item: GifsInfo,
    player: RedFullScreenPlayerState,
    savedRed: SavedRed,
    downloadRed: DownloadRed,
    block: BlockRed,
    redApi: RedApi,
    search: R_SearchExplorer,
    navigator: Navigator,
    onBack: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val downloadList by downloadRed.downloadList.collectAsStateWithLifecycle()
    val navigationState = LocalRNavigationState.current
    val user = remember(item.userName) { UsersRed.listAllUsers.firstOrNull { it.username == item.userName } }
    // Списки SavedRed — SnapshotStateList: ключом remember они не годятся (сравнение по ссылке),
    // изменения содержимого отслеживает только derivedStateOf.
    val isInCollection by remember(savedRed, item.id) {
        derivedStateOf { savedRed.collections.collectionList.any { collection -> collection.items.any { it.id == item.id } } }
    }
    val isCreatorFollowed by remember(savedRed, item.userName) {
        derivedStateOf { savedRed.creators.list.any { it.username == item.userName } }
    }
    val isLiked by remember(savedRed, item.id) {
        derivedStateOf { savedRed.likes.list.any { it.id == item.id } }
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
            player.autoRotate = !player.autoRotate
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        },
        onTagClick = { tag ->
            search.searchText.value = TextFieldValue(text = tag, selection = TextRange(tag.length))
            search.searchTextDone.value = tag
            navigationState.rootTab = 0
            navigator.pop()
        },
        actionsMenu = {
            ExpandMenuVideo(
                item = item,
                modifier = Modifier,
                onClick = {},
                haptic = { haptic.performHapticFeedback(HapticFeedbackType.Confirm) },
                onRunLike = {},
                onRefresh = {},
                isCollection = false,
                block = { block },
                redApi = { redApi },
                savedRed = { savedRed },
                downloadRed = { downloadRed }
            )
        }
    )
}

@Preview
@Composable
private fun RedFullScreenPageOverlayPreview() {
    // Зависимости R собираются только через DI, поэтому превью показывает
    // разметку оверлея с готовыми флагами.
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
