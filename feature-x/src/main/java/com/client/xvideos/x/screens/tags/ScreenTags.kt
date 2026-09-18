package com.client.xvideos.x.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.x.screens.common.bottomKeyboard.BottomListDashBoardNavigationButtons2
import com.client.xvideos.x.screens.tags.atom.TagsPaginatedListScreen
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer
import com.client.xvideos.x.normalizeXUrl
import kotlinx.coroutines.launch

class ScreenTags(private val tag: String) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {

        val vm = getScreenModel<ScreenTagsViewModel, ScreenTagsViewModel.Factory> { factory -> factory.create(tag) }
        val navigator = LocalNavigator.currentOrThrow
        val job = rememberCoroutineScope()

        // Число страниц приходит с нулевой страницей; до её разбора пейджер
        // держит одну. pageCount читается лениво, поэтому рост с 1 до 149
        // пейджер подхватывает без пересоздания состояния.
        val pagerState = rememberPagerState(initialPage = 0) { vm.screen.lastPage.coerceAtLeast(1) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Theme.L.grey6,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Theme.L.grey6)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                        )
                    }
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = tag,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (vm.screen.title0.isNotBlank() || vm.screen.title1.isNotBlank()) {
                            Row {
                                if (vm.screen.title0.isNotBlank()) {
                                    Text(
                                        text = vm.screen.title0 + " ",
                                        color = Color(0xFFB0B0B0),
                                        fontSize = 12.sp,
                                    )
                                }
                                if (vm.screen.title1.isNotBlank()) {
                                    Text(
                                        text = vm.screen.title1,
                                        color = Color(0xFF787878),
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Без кнопки страны, в отличие от ленты раздела: адрес
                // /tags/<тег>/N от страны не зависит.
                BottomListDashBoardNavigationButtons2(
                    value = pagerState.currentPage,
                    onChange = { job.launch { pagerState.animateScrollToPage(it) } },
                    max = vm.screen.lastPage,
                )
            },
        ) { padding ->
            // Раньше padding игнорировался (`{ _ -> }`) — список рисовался под
            // topBar'ом, и его первые строки оказывались перекрыты заголовком.
            Box(modifier = Modifier.padding(padding)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { pageIndex ->
                    TagsPaginatedListScreen(
                        pageIndex = pageIndex,
                        loadPage = { vm.loadPage(it).items },
                        onOpenVideo = { navigator.push(ScreenX_VideoPlayer(normalizeXUrl(it.href), it)) },
                    )
                }
            }
        }
    }

}
