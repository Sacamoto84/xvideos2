package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.ui.theme.XvideosTheme
import kotlinx.coroutines.flow.drop

object R_Screen_Saved_LikesTab : Screen {

    private fun readResolve(): Any = R_Screen_Saved_LikesTab

    override val key: ScreenKey = "R_Screen_Saved_LikesTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenSavedLikesSM = getScreenModel()

        val columnSelectRaw by Settings.r_likesTab_column_current_count.field.collectAsStateWithLifecycle()
        val columnSelect = normalizeRColumnCount(columnSelectRaw)

        val pager = vm.likedHost.pager.collectAsLazyPagingItems()

        //Изменение количества отображаемых элементов
        LaunchedEffect(columnSelect) { vm.likedHost.columns = columnSelect }

        // Ключ-ссылка на list никогда не меняется — подписываемся на размер:
        // добавление (в т.ч. приём по P2P) и удаление лайка перезагружают pager.
        LaunchedEffect(Unit) {
            androidx.compose.runtime.snapshotFlow { vm.savedRed.likes.list.size }
                .drop(1)
                .collect { pager.refresh() }
        }

        val onClickOpenProfile: (String) -> Unit = remember(vm, navigator) {
            { profileName ->
                vm.likedHost.currentIndexGoto = vm.likedHost.currentIndex
                navigator.push(ScreenRedProfile(profileName))
            }
        }

        SavedLikesTabContent(
            host = vm.likedHost,
            topInset = getTopInsetDp(),
            onClickOpenProfile = onClickOpenProfile
        )
    }
}

@Composable
fun SavedLikesTabContent(
    host: LazyRow123Host?,
    topInset: Dp,
    onClickOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentPadding = remember(topInset) { PaddingValues(top = topInset) }

    Box(modifier = modifier.fillMaxSize().background(Theme.background)) {
        if (host != null) {
            LazyRow123(
                host = host,
                modifier = Modifier.fillMaxSize(),
                onClickOpenProfile = onClickOpenProfile,
                contentPadding = contentPadding,
                isRunLike = true
            )
        }
    }
}

@Preview
@Composable
private fun SavedLikesTabContentPreview() {
    XvideosTheme(darkTheme = true) {
        SavedLikesTabContent(
            host = null,
            topInset = getTopInsetDp(),
            onClickOpenProfile = {}
        )
    }
}
