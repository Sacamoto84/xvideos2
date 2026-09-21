package com.client.xvideos.l.ui.screens.explorer

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.LSession
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.LLoginContent
import com.client.xvideos.l.ui.screens.explorer.tab.albumSearch.L_ScreenAlbumSearch
import com.client.xvideos.l.ui.screens.explorer.tab.albumTopHits.L_ScreenAlbumTopHits
import com.client.xvideos.l.ui.screens.explorer.tab.saved.L_SavedTab
import com.client.xvideos.l.ui.screens.screenAlbumList.L_ScreenAlbumList
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.common.ui.atom.TabBarPoints
import com.client.xvideos.common.navigation.rememberNavigationDepth
import com.client.xvideos.common.ui.atom.DownloadIndicator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

private val EXPLORER_ICONS = persistentListOf(
    Icons.AutoMirrored.Outlined.FormatListBulleted,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Topic,
    Icons.Outlined.Search,
)

private val EXPLORER_TAGS = persistentListOf(
    "",
    "",
    "bBookMark",
    ""
)

class L_ScreenExplorer : Screen {

    override val key: ScreenKey = "L_ScreenExplorer"

    @OptIn(ExperimentalMaterial3Api::class)
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

        val savedLogin = Settings.l_login.field.collectAsStateWithLifecycle().value
        val savedPassword = Settings.l_pass.field.collectAsStateWithLifecycle().value

        if ((savedLogin.isBlank() || savedPassword.isBlank()) && !LSession.loginSkipped) {
            LLoginContent(
                initialLogin = savedLogin,
                initialPassword = savedPassword,
                onSaved = {},
                onBack = { navigator.pop() },
                onSkip = { LSession.loginSkipped = true }
            )
            return
        }

        val percentDownload = savedL.likes.percentDownload.collectAsStateWithLifecycle().value

        val columnR_ScreenGifsTab = Settings.l_gifsTab_column_current_count.field.collectAsStateWithLifecycle().value

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

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
            Column {
                DownloadIndicator(percentDownload)
                TabRow(
                    containerColor = Theme.tabLevel0,
                    titlesIcon = EXPLORER_ICONS,
                    value = vm.screenType,
                    onChangeState = onTabChange,
                    overlay0 = { TabBarPoints(columnR_ScreenGifsTab, vm.screenType == 0) },
                    tags = EXPLORER_TAGS
                )
            }
        }, containerColor = Theme.background) { paddingValues ->
            Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                when (vm.screenType) {
                    0 -> L_ScreenAlbumList.Content()
                    1 -> L_SavedTab.Content()
                    2 -> L_ScreenAlbumTopHits.Content()
                    3 -> L_ScreenAlbumSearch.Content()
                    else -> L_SavedTab.Content()
                }
            }
        }

    }
}

/**
 * ScreenModel L-раздела. Держит ссылку на singleton [SavedL], чтобы
 * корневой L-экран мог показывать общие диалоги (создание/добавление коллекции)
 * и индикатор загрузок без обращения к глобальному состоянию.
 */
@Stable
class L_ScreenExplorerSM @Inject constructor(
    val savedL: SavedL,
    private val navigationState: LNavigationState
) : ScreenModel {
    /** Текущая вкладка верхнего уровня L-раздела (раньше — статика в Companion). */
    var screenType: Int
        get() = navigationState.rootTab
        set(value) {
            navigationState.rootTab = value
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class L_ScreenExplorerModule {
    @Binds
    @IntoMap
    @ScreenModelKey(L_ScreenExplorerSM::class)
    abstract fun bindL_ScreenExplorerSM(sm: L_ScreenExplorerSM): ScreenModel
}

