package com.client.xvideos.l.ui.screens.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.navigation.rememberNavigationDepth
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.LSession
import com.client.xvideos.l.ui.screens.explorer.molecule.L_ExplorerBottomBar
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.L_ScreenAlbumSearch
import com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.L_ScreenAlbumTopHits
import com.client.xvideos.l.ui.screens.explorer.tab.saved.L_SavedTab
import com.client.xvideos.l.ui.screens.molecule.LLoginForm
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList

class L_ScreenExplorer : Screen {

    override val key: ScreenKey = "L_ScreenExplorer"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm = getScreenModel<L_ScreenExplorerSM>()

        BackHandler(enabled = vm.screenType != 0) {
            vm.screenType = 0
        }

        val savedL = vm.savedL
        val navigationDepth = rememberNavigationDepth()

        LaunchedEffect(Unit) { navigationDepth.depth = 0 }

        val savedProfile by Settings.l_profile.field.collectAsStateWithLifecycle()
        val percentDownload by savedL.likes.percentDownload.collectAsStateWithLifecycle()
        val columnGifsTab by Settings.l_gifsTab_column_current_count.field.collectAsStateWithLifecycle()

        val onPop: () -> Unit = remember(navigator) { { navigator.pop() } }
        // Набранное стираем, как только форма больше не нужна: пароль незачем
        // держать в памяти, пока раздел открыт.
        val loginForm = vm.loginForm
        val onSkipLogin = remember(loginForm) {
            {
                LSession.loginSkipped = true
                loginForm.clear()
            }
        }
        val onSavedLogin = remember(loginForm) { { loginForm.clear() } }
        val onLoginChange = remember(loginForm) { loginForm::updateLogin }
        val onPasswordChange = remember(loginForm) { loginForm::updatePassword }

        if (!savedProfile.isComplete && !LSession.loginSkipped) {
            LLoginForm(
                login = loginForm.login,
                password = loginForm.password,
                onLoginChange = onLoginChange,
                onPasswordChange = onPasswordChange,
                onSaved = onSavedLogin,
                onBack = onPop,
                onSkip = onSkipLogin
            )
            return
        }

        val onTabChange: (Int) -> Unit = remember(vm) {
            { tab ->
                if (tab == vm.screenType) {
                    when (tab) {
                        0 -> { ColumnSelect_AddColumn(Settings.l_gifsTab_column_current_count, Settings.l_gifsTab_G_0_4) }
                    }
                }
                vm.screenType = tab
            }
        }

        L_ScreenExplorerContent(
            screenType = vm.screenType,
            percentDownload = percentDownload,
            columnGifsTab = columnGifsTab,
            onTabChange = onTabChange
        )
    }
}

@Composable
fun L_ScreenExplorerContent(
    screenType: Int,
    percentDownload: Float,
    columnGifsTab: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            L_ExplorerBottomBar(
                screenType = screenType,
                percentDownload = percentDownload,
                columnGifsTab = columnGifsTab,
                onTabChange = onTabChange
            )
        },
        containerColor = Theme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
            when (screenType) {
                0 -> L_ScreenAlbumList.Content()
                1 -> L_SavedTab.Content()
                2 -> L_ScreenAlbumTopHits.Content()
                3 -> L_ScreenAlbumSearch.Content()
                else -> L_SavedTab.Content()
            }
        }
    }
}

@Preview
@Composable
private fun L_ScreenExplorerContentPreview() {
    L_ScreenExplorerContent(
        screenType = 0,
        percentDownload = 0f,
        columnGifsTab = 1,
        onTabChange = {}
    )
}
