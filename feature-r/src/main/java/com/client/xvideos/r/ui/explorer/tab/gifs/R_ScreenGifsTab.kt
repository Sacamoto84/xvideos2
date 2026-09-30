package com.client.xvideos.r.ui.explorer.tab.gifs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.model.Order
import com.client.xvideos.r.ui.explorer.tab.gifs.molecule.GifsTabBottomBar
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.search.RSearchTextField
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123

object R_ScreenGifsTab : Screen {

    private fun readResolve(): Any = R_ScreenGifsTab

    override val key: ScreenKey = "R_ScreenGifsTab"

    @Composable
    override fun Content() {
        val vm: ScreenRedExplorerGifsSM = getScreenModel()
        val navigator = LocalNavigator.current
        val haptic = LocalHapticFeedback.current

        val columnSelectRaw by Settings.r_explorerGifsTab_column_current_count.field.collectAsStateWithLifecycle()
        val columnSelect = normalizeRColumnCount(columnSelectRaw)

        val search = vm.search
        val searchQuery by search.searchTextDone.collectAsStateWithLifecycle()
        val isFocused by search.focused.collectAsStateWithLifecycle()
        val sortType by vm.lazyHost.sortType.collectAsStateWithLifecycle()

        LaunchedEffect(columnSelect) { vm.lazyHost.columns = columnSelect }

        val onSortSelect: (Order) -> Unit = remember(vm) { { order -> vm.lazyHost.changeSortType(order) } }
        val onUpClick: () -> Unit = remember(vm, haptic) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                vm.lazyHost.gotoUp()
            }
        }
        val onClickOpenProfile: (String) -> Unit = remember(vm, navigator) {
            { name ->
                vm.lazyHost.currentIndexGoto = vm.lazyHost.currentIndex
                navigator?.push(ScreenRedProfile(name))
            }
        }

        val searchFieldComposable: @Composable (Modifier) -> Unit = remember(search) {
            { modifier -> RSearchTextField(search, modifier = modifier) }
        }

        val topInset = getTopInsetDp()
        val contentPadding = remember(topInset) { PaddingValues(top = topInset) }

        R_ScreenGifsTabContent(
            bottomBar = {
                GifsTabBottomBar(
                    searchField = searchFieldComposable,
                    searchQuery = searchQuery,
                    isFocused = isFocused,
                    sortType = sortType,
                    onSortSelect = onSortSelect,
                    onUpClick = onUpClick
                )
            },
            lazyContent = {
                LazyRow123(
                    host = vm.lazyHost,
                    modifier = Modifier.fillMaxSize(),
                    onClickOpenProfile = onClickOpenProfile,
                    contentPadding = contentPadding,
                )
            }
        )
    }
}

@Composable
fun R_ScreenGifsTabContent(
    bottomBar: @Composable () -> Unit,
    lazyContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = bottomBar,
        containerColor = Theme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .fillMaxSize()
        ) {
            lazyContent()
        }
    }
}

@Preview
@Composable
private fun R_ScreenGifsTabPreview() {
    R_ScreenGifsTabContent(
        bottomBar = {},
        lazyContent = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Gifs Content Placeholder", color = Color.White)
            }
        }
    )
}
