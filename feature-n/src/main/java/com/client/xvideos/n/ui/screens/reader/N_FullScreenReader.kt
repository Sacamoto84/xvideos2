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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.client.xvideos.common.ui.scroll.PagerPosition
import com.client.xvideos.common.ui.statusbar.HideStatusBarEffect
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NPage
import com.client.xvideos.n.model.NPicture
import com.client.xvideos.n.ui.molecule.NCollectionDialogs
import com.client.xvideos.n.ui.molecule.NPictureItemExpandMenu
import com.client.xvideos.n.ui.screens.gallery.ScreenNGallery
import com.client.xvideos.n.ui.screens.reader.atom.NReaderTopBar
import com.client.xvideos.n.ui.screens.reader.atom.ReaderImagePage
import com.client.xvideos.n.ui.screens.reader.molecule.NReaderBottomStrip
import com.client.xvideos.n.ui.screens.reader.molecule.ReaderLoadingScreen
import kotlinx.coroutines.launch

/**
 * Полноэкранный просмотрщик (ридер) манги с поддержкой горизонтальной и вертикальной ориентации,
 * лайков отдельных страниц и добавления в коллекции.
 */
class N_FullScreenReader(
    val galleryId: Long,
    val initialPageNumber: Int = 1
) : Screen {

    override val key: ScreenKey = "NReader:$galleryId:$initialPageNumber"

    @Composable
    override fun Content() {
        HideStatusBarEffect()

        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<N_FullScreenReaderSM>()
        val screens = navigator.items
        val album = (screens.getOrNull(screens.lastIndex - 1) as? ScreenNGallery)
            ?.takeIf { it.galleryId == galleryId }
            ?.rememberModel()
        var currentPageNumber by rememberSaveable { mutableIntStateOf(initialPageNumber) }
        var reload by remember { mutableIntStateOf(0) }
        val result by produceState<Result<NGallery>?>(null, galleryId, album, reload) {
            value = null
            value = vm.loadGallery(galleryId, album?.uiState, forceRefresh = album == null && reload > 0)
        }
        fun returnToAlbum(pageNumber: Int) {
            album?.scrollToReaderPage(pageNumber)
            navigator.pop()
        }
        val gallery = result?.getOrNull()
        if (gallery == null) {
            val onBack = { returnToAlbum(currentPageNumber) }
            BackHandler(onBack = onBack)
            ReaderLoadingScreen(result?.exceptionOrNull(), onBack, onRetry = {
                album?.loadGallery()
                reload++
            })
            return
        }

        val coroutineScope = rememberCoroutineScope()
        var isVertical by remember {
            mutableStateOf(Settings.n_fullscreen_vertical_pager.field.value)
        }
        var showControls by remember { mutableStateOf(true) }

        val position = remember(gallery) {
            PagerPosition(gallery.pages.indexOfFirst { it.pageNumber == currentPageNumber }.coerceAtLeast(0))
        }
        val pagerState = position.rememberState { gallery.pages.size }
        val onBack: () -> Unit = {
            returnToAlbum(gallery.pages.getOrNull(pagerState.currentPage)?.pageNumber ?: currentPageNumber)
        }
        BackHandler(onBack = onBack)
        LaunchedEffect(pagerState) {
            snapshotFlow { gallery.pages.getOrNull(pagerState.currentPage)?.pageNumber }.collect { number ->
                if (number != null) currentPageNumber = number
            }
        }

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

        val isLiked = gallery.pages.getOrNull(pagerState.currentPage)?.let { vm.isPageLiked(gallery.id, it.pageNumber) }

        N_FullScreenReaderContent(
            gallery = gallery,
            pagerState = pagerState,
            isVertical = isVertical,
            isLiked = isLiked,
            showControls = showControls,
            onToggleControls = { showControls = !showControls },
            onBack = onBack,
            onToggleOrientation = onToggleOrientation,
            onSetLiked = vm::setLiked,
            onAddToCollection = vm::openCollectionPicker,
            onPageSelected = onPageSelected
        )

        NCollectionDialogs(
            isSelectOpen = vm.isSelectCollectionOpen,
            isCreateOpen = vm.isCreateCollectionOpen,
            collections = vm.saved.collection.collections,
            onDismiss = vm::dismissCollectionPicker,
            onOpenCreate = {
                vm.isSelectCollectionOpen = false
                vm.isCreateCollectionOpen = true
            },
            onCreateCollection = { name -> vm.addToCollection(name, create = true) },
            onSelectCollection = { name -> vm.addToCollection(name) },
            coverUrlProvider = vm::getCollectionCoverUrl
        )
    }
}

@Composable
fun N_FullScreenReaderContent(
    gallery: NGallery,
    pagerState: PagerState,
    isVertical: Boolean,
    isLiked: Boolean?,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onBack: () -> Unit,
    onToggleOrientation: () -> Unit,
    onSetLiked: (NPicture, Boolean) -> Unit,
    onAddToCollection: (NPicture) -> Unit,
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
                            onSetLiked = onSetLiked,
                            onAddToCollection = { onAddToCollection(currentPicture) }
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
        onSetLiked = { _, _ -> },
        onAddToCollection = {},
        onPageSelected = {}
    )
}
