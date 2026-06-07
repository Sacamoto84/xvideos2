package com.client.xvideos.r.ui.profile.tikTok

import com.client.xvideos.common.theme.Theme

//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun TikTokStyleVideoFeed(
//    vm: ScreenRedProfileSM,
//    videoItems: List<GifsInfo>,
//    modifier: Modifier = Modifier,
//    onChangeTime: (Pair<Float, Int>) -> Unit,
//    onPageUIElementsVisibilityChange: (Boolean) -> Unit, // Новый колбэк
//
//    onClick: (GifsInfo) -> Unit = {},
//    onLongClick: (GifsInfo) -> Unit = {},
//
//    timeA: Float = 3f,
//    timeB: Float = 6f,
//    enableAB: Boolean = false,
//
//    isMute: Boolean = true, //Глобальный мут
//
//    /**
//     * Вызывается при изменении текущей страницы в пейджере.
//     */
//    onChangePagerPage: (Int) -> Unit = {},
//
//    menuContent: @Composable () -> Unit = {},
//    menuContentWidth: Dp = 192.dp,
//    menuDefaultOpen : Boolean = false,
//    menuOpenChanged : (Boolean) -> Unit,
//
//    initialPage : Int
//
//) {
//
//    val pagerState = rememberPagerState(pageCount = { videoItems.size })
//
//    LaunchedEffect(initialPage) { pagerState.scrollToPage(initialPage) }
//
//    if (videoItems.isEmpty()) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Theme.R.colorCommonBackground),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                "Нет видео для отображения",
//                color = Color.LightGray,
//                fontFamily = Theme.R.fontFamilyPopinsRegular,
//                fontSize = 20.sp
//            )
//        }
//
//        // Сообщаем, что UI не должен быть виден, если список пуст
//        // (или решаем это на уровне вызывающего компонента)
//        LaunchedEffect(Unit) { onPageUIElementsVisibilityChange(false) }
//
//        return
//    }
//
//    LaunchedEffect(pagerState.isScrollInProgress, pagerState.currentPage) {
//        // Мы хотим, чтобы UI элементы были видны, когда прокрутка НЕ идет
//        // и мы находимся на какой-то конкретной странице (не между страницами).
//        onPageUIElementsVisibilityChange(!pagerState.isScrollInProgress)
//        onChangePagerPage(pagerState.currentPage)
//    }
//
//    VerticalPager(
//        state = pagerState,
//        modifier = Modifier.fillMaxSize(),
//        key = { index -> videoItems[index].id } // Ключ для стабильности элементов
//    ) { pageIndex ->
//
//        val videoItem = videoItems[pageIndex] // pageIndex - это индекс текущей отображаемой страницы
//
//        // Управляем воспроизведением в зависимости от того, видима ли страница
//        // и соответствует ли она текущей активной странице в пейджере.
//        val isCurrentPage = pagerState.currentPage == pageIndex
//
//
//        val videoUri: String = remember(videoItem.id, videoItem.userName) {
//            Timber.tag("???").i("Перерачсет videoItem.id = ${videoItem.id}")
//            //Определяем адрес откуда брать видео, из кеша или из сети
//            if (DownloadRed.findVideoInDownload(videoItem.id, videoItem.userName))
//                "${AppPath.cache_download_red}/${videoItem.userName}/${videoItem.id}.mp4"
//            else
//                "https://api.redgifs.com/v2/gifs/${videoItem.id.lowercase()}/hd.m3u8"
//        }
//
//        RedVideoPlayerWithMenuContent(
//            url = videoUri,
//            play = isCurrentPage and vm.play,
//            onChangeTime = onChangeTime,
//            isMute = isMute,
//            onPlayerControlsReady = { controls ->
//                // Этот колбэк вызовется для КАЖДОЙ страницы пейджера,
//                // когда её плеер будет готов. Нам нужен контроллер только для ТЕКУЩЕЙ страницы.
//                if (isCurrentPage) {
//                    vm.currentPlayerControls = controls
//                }
//            },
//            timeA = timeA,
//            timeB = timeB,
//            enableAB = enableAB,
//            onClick = { vm.play = !vm.play },
//            menuContent = menuContent,
//            menuContentWidth = menuContentWidth,
//            menuDefaultOpen = menuDefaultOpen,
//            menuOpenChanged = menuOpenChanged,
//            autoRotate = vm.autoRotate,
//            isCurrentPage = isCurrentPage
//        )
//
//    }
//}
