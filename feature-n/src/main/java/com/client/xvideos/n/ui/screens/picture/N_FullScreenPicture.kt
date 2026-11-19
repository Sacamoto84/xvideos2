package com.client.xvideos.n.ui.screens.picture

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.ui.statusbar.HideStatusBarEffect
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.atom.NPictureViewerPage
import com.client.xvideos.n.ui.atom.NPictureViewerTopBar
import com.client.xvideos.n.ui.molecule.NCollectionDialogs
import com.client.xvideos.n.ui.molecule.NPictureItemExpandMenu
import com.client.xvideos.n.ui.molecule.NPictureViewerBottomStrip
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery
import com.client.xvideos.n.ui.screens.explorer.N_ScreenExplorer
import com.client.xvideos.n.ui.screens.explorer.N_ScreenExplorerSM
import com.client.xvideos.n.ui.screens.explorer.tab.saved.N_SavedTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.N_SavedTabSM
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.N_ScreenSavedCollectionsTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.collections.ScreenSavedNCollectionsSM
import com.client.xvideos.n.ui.screens.explorer.tab.saved.likes.N_ScreenSavedLikesTab
import com.client.xvideos.n.ui.screens.explorer.tab.saved.likes.ScreenSavedNLikesSM
import kotlinx.coroutines.launch

/**
 * Полноэкранный просмотрщик сохранённых картинок (из лайков и коллекций).
 */
class N_FullScreenPicture(
    val initialPicture: NPicture,
    val collectionName: String? = null
) : Screen {

    override val key: ScreenKey = "NPicture:${initialPicture.id}:${collectionName.orEmpty()}"

    @Composable
    override fun Content() {
        HideStatusBarEffect()

        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_FullScreenPictureSM>()

        val savedTab = N_SavedTab.getScreenModel<N_SavedTabSM>()
        val screens = navigator.items
        val explorer = (screens.getOrNull(screens.lastIndex - 1) as? N_ScreenExplorer)
            ?.getScreenModel<N_ScreenExplorerSM>()
        val likes = if (collectionName == null) N_ScreenSavedLikesTab.getScreenModel<ScreenSavedNLikesSM>() else null
        val collections = if (collectionName != null) {
            N_ScreenSavedCollectionsTab.getScreenModel<ScreenSavedNCollectionsSM>()
        } else null

        fun returnToSource(pictureId: String, index: Int) {
            explorer?.screenType = 1
            if (collectionName == null) {
                savedTab.screenType = 0
                likes?.showPicture(pictureId, index)
            } else {
                savedTab.screenType = 2
                collections?.showPicture(collectionName, pictureId, index)
            }
            navigator.pop()
        }

        var currentPictureId by rememberSaveable { mutableStateOf(initialPicture.id) }
        val loaded by produceState<NSavedPicturePages?>(null, vm, initialPicture, collectionName) {
            value = vm.loadPictures(collectionName, initialPicture, currentPictureId)
        }
        val pages = loaded
        if (pages == null) {
            BackHandler { returnToSource(currentPictureId, 0) }
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        val coroutineScope = rememberCoroutineScope()
        var isVertical by remember {
            mutableStateOf(Settings.n_fullscreen_vertical_pager.field.value)
        }

        val pictures = pages.pictures
        val pagerState = pages.position.rememberState { pictures.size }
        LaunchedEffect(pagerState) {
            snapshotFlow { pictures.getOrNull(pagerState.currentPage)?.id }.collect { id ->
                if (id != null) currentPictureId = id
            }
        }
        val onBack: () -> Unit = {
            val index = pagerState.currentPage
            val picture = pictures.getOrNull(index) ?: initialPicture
            returnToSource(picture.id, index)
        }
        BackHandler(onBack = onBack)

        val onToggleOrientation = remember {
            {
                val next = !isVertical
                isVertical = next
                Settings.n_fullscreen_vertical_pager.setValue(next)
            }
        }

        val onPictureSelected: (Int) -> Unit = remember(pagerState, coroutineScope) {
            { targetIndex ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetIndex)
                }
            }
        }

        val currentPicture = pictures.getOrNull(pagerState.currentPage)
        val isLiked = currentPicture != null && vm.isPictureLiked(currentPicture)

        val onToggleLike: () -> Unit = remember(vm, currentPicture) {
            {
                currentPicture?.let { vm.toggleLike(it) }
            }
        }

        val onAddToCollection: () -> Unit = remember(vm) {
            {
                vm.isSelectCollectionOpen = true
            }
        }

        val onOpenGallery: () -> Unit = remember(navigator, currentPicture) {
            {
                currentPicture?.let {
                    navigator.push(ScreenNGallery(it.galleryId))
                }
            }
        }

        N_FullScreenPictureContent(
            pictures = pictures,
            pagerState = pagerState,
            isVertical = isVertical,
            isLiked = isLiked,
            onBack = onBack,
            onOpenGallery = onOpenGallery,
            onToggleOrientation = onToggleOrientation,
            onToggleLike = onToggleLike,
            onAddToCollection = onAddToCollection,
            onPictureSelected = onPictureSelected
        )

        NCollectionDialogs(
            isSelectOpen = vm.isSelectCollectionOpen,
            isCreateOpen = vm.isCreateCollectionOpen,
            collections = vm.saved.collection.collections,
            onDismiss = {
                vm.isSelectCollectionOpen = false
                vm.isCreateCollectionOpen = false
            },
            onOpenCreate = {
                vm.isSelectCollectionOpen = false
                vm.isCreateCollectionOpen = true
            },
            onCreateCollection = { name ->
                currentPicture?.let { vm.addToCollection(it, name, create = true) }
                vm.isCreateCollectionOpen = false
            },
            onSelectCollection = { name ->
                currentPicture?.let { vm.addToCollection(it, name) }
                vm.isSelectCollectionOpen = false
            },
            coverUrlProvider = vm::getCollectionCoverUrl
        )
    }
}

@Composable
fun N_FullScreenPictureContent(
    pictures: List<NPicture>,
    pagerState: PagerState,
    isVertical: Boolean,
    isLiked: Boolean,
    onBack: () -> Unit,
    onOpenGallery: () -> Unit,
    onToggleOrientation: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToCollection: () -> Unit,
    onPictureSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    val onToggleControls = remember { { showControls = !showControls } }
    val topInset = getTopInsetDp()
    val interactionSource = remember { MutableInteractionSource() }
    val currentPicture = pictures.getOrNull(pagerState.currentPage)
    val title = remember(currentPicture) {
        if (currentPicture != null) {
            "${currentPicture.galleryTitle} • Стр. ${currentPicture.pageNumber}"
        } else {
            ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleControls
            )
    ) {
        if (pictures.isNotEmpty()) {
            if (isVertical) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { index -> pictures[index].id }
                ) { pageIndex ->
                    NPictureViewerPage(
                        picture = pictures[pageIndex],
                        isSettledPage = pagerState.settledPage == pageIndex,
                        onTap = onToggleControls
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { index -> pictures[index].id }
                ) { pageIndex ->
                    NPictureViewerPage(
                        picture = pictures[pageIndex],
                        isSettledPage = pagerState.settledPage == pageIndex,
                        onTap = onToggleControls
                    )
                }
            }
        }

        // Верхняя панель управления
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            NPictureViewerTopBar(
                title = title,
                currentIndex = pagerState.currentPage + 1,
                totalCount = pictures.size,
                isVertical = isVertical,
                onBack = onBack,
                onToggleOrientation = onToggleOrientation,
                topInset = topInset,
                menuContent = {
                    currentPicture?.let { pic ->
                        NPictureItemExpandMenu(
                            picture = pic,
                            isLiked = isLiked,
                            onOpenGallery = onOpenGallery,
                            onToggleLike = onToggleLike,
                            onAddToCollection = onAddToCollection
                        )
                    }
                }
            )
        }

        // Нижняя лента миниатюр
        AnimatedVisibility(
            visible = showControls && pictures.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NPictureViewerBottomStrip(
                pictures = pictures,
                currentIndex = pagerState.currentPage,
                onPictureSelected = onPictureSelected
            )
        }
    }
}

@Preview
@Composable
private fun N_FullScreenPictureContentPreview() {
    val dummy = listOf(
        NPicture(id = "1_1", galleryId = 1L, mediaId = 1L, pageNumber = 1, galleryTitle = "Preview Manga"),
        NPicture(id = "1_2", galleryId = 1L, mediaId = 1L, pageNumber = 2, galleryTitle = "Preview Manga")
    )
    val pagerState = rememberPagerState(initialPage = 0) { dummy.size }
    N_FullScreenPictureContent(
        pictures = dummy,
        pagerState = pagerState,
        isVertical = false,
        isLiked = true,
        onBack = {},
        onOpenGallery = {},
        onToggleOrientation = {},
        onToggleLike = {},
        onAddToCollection = {},
        onPictureSelected = {}
    )
}
