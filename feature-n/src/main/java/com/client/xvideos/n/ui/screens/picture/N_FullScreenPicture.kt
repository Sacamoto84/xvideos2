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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.client.xvideos.n.ui.molecule.NPictureViewerBottomStrip
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery
import kotlinx.coroutines.launch

/**
 * Полноэкранный просмотрщик сохранённых картинок (из лайков и коллекций).
 */
class N_FullScreenPicture(
    val pictures: List<NPicture>,
    val initialIndex: Int = 0
) : Screen {

    override val key: ScreenKey =
        "NPicture:${pictures.getOrNull(initialIndex)?.id ?: "empty"}:$initialIndex"

    @Composable
    override fun Content() {
        HideStatusBarEffect()

        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_FullScreenPictureSM>()

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }
        BackHandler(onBack = onBack)

        val coroutineScope = rememberCoroutineScope()
        var isVertical by remember {
            mutableStateOf(Settings.n_fullscreen_vertical_pager.field.value)
        }

        val safeInitialIndex = remember(pictures.size, initialIndex) {
            initialIndex.coerceIn(0, (pictures.size - 1).coerceAtLeast(0))
        }

        val pagerState = rememberPagerState(
            initialPage = safeInitialIndex,
            pageCount = { pictures.size }
        )

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
                currentPicture?.let { vm.createCollectionAndAdd(it, name) }
                vm.isCreateCollectionOpen = false
            },
            onSelectCollection = { name ->
                currentPicture?.let { vm.addToCollection(it, name) }
                vm.isSelectCollectionOpen = false
            }
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
                isLiked = isLiked,
                onBack = onBack,
                onOpenGallery = onOpenGallery,
                onToggleLike = onToggleLike,
                onAddToCollection = onAddToCollection,
                onToggleOrientation = onToggleOrientation,
                topInset = topInset
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
