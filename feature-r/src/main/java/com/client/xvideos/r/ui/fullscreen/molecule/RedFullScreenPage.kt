package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.client.xvideos.common.videoplayer.feed.FeedPlayerState
import com.client.xvideos.common.videoplayer.feed.rememberFeedPlayerState
import com.client.xvideos.r.common.UsersRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.expand_menu_video.ExpandMenuVideo
import com.client.xvideos.r.ui.explorer.LocalRNavigationState
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreenSM
import com.client.xvideos.r.ui.fullscreen.redVideoUrl
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.video.RedPooledVideoPlayer

@Composable
fun RedFullScreenPage(
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator,
    feedState: FeedPlayerState,
    downloadedKeys: Set<String>,
    index: Int,
    bottomPadding: Dp,
    play: Boolean,
    isCurrentPage: Boolean,
    showOverlay: Boolean,
    onBuffering: (Boolean) -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
    resetZoomTrigger: Int = 0,
    onBack: () -> Unit = { navigator.pop() },
) {
    val videoUri = remember(item.id, item.userName, downloadedKeys) { redVideoUrl(item, downloadedKeys) }

    Box(Modifier.fillMaxSize()) {
        RedPooledVideoPlayer(
            feedState = feedState,
            index = index,
            url = videoUri,
            modifier = Modifier.padding(bottom = bottomPadding),
            play = play,
            isMute = vm.mute,
            isCurrentPage = isCurrentPage,
            autoRotate = vm.autoRotate,
            timeA = vm.timeA,
            timeB = vm.timeB,
            enableAB = vm.enableAB,
            speed = vm.speed,
            onTimeChanged = { position, duration ->
                if (isCurrentPage) {
                    vm.currentPlayerTime = position
                    vm.currentPlayerDuration = duration
                }
            },
            onPlayerControlsReady = { controls ->
                if (isCurrentPage) {
                    vm.currentPlayerControls = controls
                }
            },
            onPlayerControlsRelease = { controls ->
                // Сравнение по ссылке: страница отзывает только свои controls и не
                // затирает те, что успела выставить пришедшая ей на смену.
                if (vm.currentPlayerControls === controls) vm.currentPlayerControls = null
            },
            onClick = { if (isCurrentPage) vm.togglePlay() },
            onBufferingChanged = { buffering ->
                if (isCurrentPage) {
                    onBuffering(buffering)
                }
            },
            onZoomChanged = onZoomChanged,
            resetZoomTrigger = resetZoomTrigger,
        )

        if (showOverlay) {
            val haptic = LocalHapticFeedback.current
            val downloadList by vm.downloadRed.downloadList.collectAsStateWithLifecycle()
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
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
                        haptic = { haptic.performHapticFeedback(HapticFeedbackType.Confirm) },
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
    }
}

@Preview
@Composable
private fun RedFullScreenPagePreview() {
    val feedState = rememberFeedPlayerState(poolCapacity = 1)
    Box(Modifier.fillMaxSize()) {
        RedPooledVideoPlayer(
            feedState = feedState,
            index = 0,
            url = "",
            play = false,
            isMute = true,
            isCurrentPage = true,
            autoRotate = false,
            timeA = 0f,
            timeB = 0f,
            enableAB = false,
            speed = com.client.xvideos.common.videoplayer.model.PlayerSpeed.DEFAULT,
            onTimeChanged = { _, _ -> },
            onPlayerControlsReady = {},
            onPlayerControlsRelease = {},
            onClick = {},
            onBufferingChanged = {},
            onZoomChanged = {},
            resetZoomTrigger = 0
        )
    }
}
