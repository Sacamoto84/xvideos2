package com.client.xvideos.n.ui.screens.reader

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
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.atom.NReaderTopBar
import com.client.xvideos.n.ui.atom.ReaderImagePage
import com.client.xvideos.n.ui.molecule.NCollectionDialogs
import com.client.xvideos.n.ui.molecule.NPictureItemExpandMenu
import com.client.xvideos.n.ui.molecule.NReaderBottomStrip
import kotlinx.coroutines.launch

/**
 * Полноэкранный просмотрщик (ридер) манги с поддержкой горизонтальной и вертикальной ориентации,
 * лайков отдельных страниц и добавления в коллекции.
 */
class N_FullScreenReader(
    val gallery: NGallery,
    val initialPage: Int = 0
) : Screen {

    override val key: ScreenKey = "NReader:${gallery.id}:$initialPage"

    @Composable
    override fun Content() {
        HideStatusBarEffect()

        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_FullScreenReaderSM>()

        val onBack: () -> Unit = remember(navigator) { { navigator.pop() } }
        BackHandler(onBack = onBack)

        val coroutineScope = rememberCoroutineScope()
        var isVertical by remember {
            mutableStateOf(Settings.n_fullscreen_vertical_pager.field.value)
        }
        var showControls by remember { mutableStateOf(true) }

        val safeInitialPage = remember(gallery.pages.size, initialPage) {
            initialPage.coerceIn(0, (gallery.pages.size - 1).coerceAtLeast(0))
        }

        val pagerState = rememberPagerState(
            initialPage = safeInitialPage,
            pageCount = { gallery.pages.size }
        )

        val onToggleOrientation = remember {
            {
                val next = !isVertical
                isVertical = next
                Settings.n_fullscreen_vertical_pager.setValue(next)
            }
        }

        val onPageSelected: (Int) -> Unit = remember(pagerState, coroutineScope) {
            { targetIndex ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetIndex)
                }
            }
        }

        val currentPageNumber = gallery.pages.getOrNull(pagerState.currentPage)?.pageNumber ?: (pagerState.currentPage + 1)
        val isLiked = vm.isPageLiked(gallery.id, currentPageNumber)

        val onToggleLike: () -> Unit = remember(vm, gallery, pagerState) {
            {
                vm.toggleLikePage(gallery, pagerState.currentPage)
            }
        }

        val onAddToCollection: () -> Unit = remember(vm) {
            {
                vm.isSelectCollectionOpen = true
            }
        }

        N_FullScreenReaderContent(
            gallery = gallery,
            pagerState = pagerState,
            isVertical = isVertical,
            isLiked = isLiked,
            showControls = showControls,
            onToggleControls = { showControls = !showControls },
            onBack = onBack,
            onToggleOrientation = onToggleOrientation,
            onToggleLike = onToggleLike,
            onAddToCollection = onAddToCollection,
            onPageSelected = onPageSelected
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
                vm.createCollectionAndAdd(gallery, pagerState.currentPage, name)
                vm.isCreateCollectionOpen = false
            },
            onSelectCollection = { name ->
                vm.addToCollection(gallery, pagerState.currentPage, name)
                vm.isSelectCollectionOpen = false
            }
        )
    }
}

@Composable
fun N_FullScreenReaderContent(
    gallery: NGallery,
    pagerState: PagerState,
    isVertical: Boolean,
    isLiked: Boolean,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onBack: () -> Unit,
    onToggleOrientation: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToCollection: () -> Unit,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val topInset = getTopInsetDp()
    val interactionSource = remember { MutableInteractionSource() }

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
        if (gallery.pages.isNotEmpty()) {
            if (isVertical) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { index -> gallery.pages[index].pageNumber }
                ) { pageIndex ->
                    ReaderImagePage(
                        page = gallery.pages[pageIndex],
                        mediaId = gallery.mediaId,
                        isSettledPage = pagerState.settledPage == pageIndex,
                        onTap = onToggleControls
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { index -> gallery.pages[index].pageNumber }
                ) { pageIndex ->
                    ReaderImagePage(
                        page = gallery.pages[pageIndex],
                        mediaId = gallery.mediaId,
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
            val currentPageObj = gallery.pages.getOrNull(pagerState.currentPage)
            val currentPicture = remember(gallery, currentPageObj) {
                currentPageObj?.let { NPicture.fromPage(gallery.id, gallery.mediaId, gallery.title, it) }
            }

            NReaderTopBar(
                title = gallery.title,
                currentPage = pagerState.currentPage + 1,
                totalPages = gallery.pages.size,
                isVertical = isVertical,
                onBack = onBack,
                onToggleOrientation = onToggleOrientation,
                topInset = topInset,
                menuContent = {
                    if (currentPicture != null) {
                        NPictureItemExpandMenu(
                            picture = currentPicture,
                            isLiked = isLiked,
                            onToggleLike = onToggleLike,
                            onAddToCollection = onAddToCollection
                        )
                    }
                }
            )
        }

        // Нижняя лента миниатюр
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NReaderBottomStrip(
                pages = gallery.pages,
                mediaId = gallery.mediaId,
                currentPageIndex = pagerState.currentPage,
                onPageSelected = onPageSelected
            )
        }
    }
}

@Preview
@Composable
private fun N_FullScreenReaderContentPreview() {
    val dummyGallery = NGallery(
        id = 12345L,
        mediaId = 67890L,
        title = "Preview Reader Manga",
        pages = listOf(
            NPage(pageNumber = 1, ext = "jpg"),
            NPage(pageNumber = 2, ext = "jpg"),
            NPage(pageNumber = 3, ext = "jpg")
        )
    )
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    N_FullScreenReaderContent(
        gallery = dummyGallery,
        pagerState = pagerState,
        isVertical = false,
        isLiked = true,
        showControls = true,
        onToggleControls = {},
        onBack = {},
        onToggleOrientation = {},
        onToggleLike = {},
        onAddToCollection = {},
        onPageSelected = {}
    )
}
