package com.client.xvideos.r.ui.explorer.tab.saved.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.SavedTabHeader
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.explorer.tab.saved.tab.account.ScreenAccountSavedMedia
import com.client.xvideos.r.ui.profile.ScreenRedProfile
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123
import com.client.xvideos.r.ui.ui.lazyrow123.LazyRow123Host
import com.client.xvideos.ui.theme.XvideosTheme

object R_Screen_Saved_LikesTab : Screen {

    private fun readResolve(): Any = R_Screen_Saved_LikesTab

    override val key: ScreenKey = "R_Screen_Saved_LikesTab"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ScreenSavedLikesSM = getScreenModel()

        val columnSelectRaw by Settings.r_likesTab_column_current_count.field.collectAsStateWithLifecycle()
        val columnSelect = normalizeRColumnCount(columnSelectRaw)

        //Изменение количества отображаемых элементов
        LaunchedEffect(columnSelect) { vm.likedHost.columns = columnSelect }

        val onClickOpenProfile: (String) -> Unit = remember(vm, navigator) {
            { profileName ->
                vm.likedHost.currentIndexGoto = vm.likedHost.currentIndex
                navigator.push(ScreenRedProfile(profileName))
            }
        }

        SavedLikesTabContent(
            host = vm.likedHost,
            topInset = getTopInsetDp(),
            onClickOpenProfile = onClickOpenProfile,
            onOpenAccountLikes = { navigator.push(ScreenAccountSavedMedia()) },
        )
    }
}

@Composable
fun SavedLikesTabContent(
    host: LazyRow123Host?,
    topInset: Dp,
    onClickOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenAccountLikes: () -> Unit = {},
) {
    val contentPadding = remember(topInset) { PaddingValues(top = topInset) }

    Box(modifier = modifier.fillMaxSize().background(Theme.background)) {
        if (host != null) {
            LazyRow123(
                host = host,
                modifier = Modifier.fillMaxSize(),
                onClickOpenProfile = onClickOpenProfile,
                contentPadding = contentPadding,
                // Шапка — первым элементом сетки: на всю ширину и уезжает со
                // списком, как в X. Вырез — отступом списка, под ним и индикатор
                // обновления.
                contentBeforeList = {
                    SavedTabHeader(title = "Лайки") {
                        TextButton(onClick = onOpenAccountLikes) { Text("Лайки RedGIFs") }
                    }
                },
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
