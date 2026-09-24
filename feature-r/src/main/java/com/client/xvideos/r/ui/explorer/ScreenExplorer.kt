package com.client.xvideos.r.ui.explorer

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.ui.TabRow
import kotlinx.collections.immutable.persistentListOf
import com.client.xvideos.r.ui.explorer.tab.gifs.ColumnSelect_AddRColumn
import com.client.xvideos.r.ui.explorer.tab.gifs.R_ScreenGifsTab
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.explorer.tab.niches.R_ScreenNichesTab
import com.client.xvideos.r.ui.explorer.tab.saved.R_ScreenSavedTab
import com.client.xvideos.r.ui.explorer.tab.search.SearchTab

import com.client.xvideos.common.ui.atom.TabBarPoints

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

// persistentListOf, а не listOf: обычный List для Compose нестабилен, и TabRow
// перекомпоновывался чаще, чем нужно.
private val EXPLORER_TAB_ICONS = persistentListOf(
    Icons.Outlined.Movie,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Group,
    Icons.Outlined.Search
)

class ScreenRedExplorer : Screen {

    override val key: ScreenKey = "ScreenRedExplorer"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val vm = getScreenModel<ScreenRedExplorerSM>()

        BackHandler(enabled = vm.screenType != 0) {
            vm.screenType = 0
        }

        val columnGifsTab by Settings.r_explorerGifsTab_column_current_count.field.collectAsStateWithLifecycle()
        val overlay0 = normalizeRColumnCount(columnGifsTab)

        val onTabChange: (Int) -> Unit = remember(vm) {
            { tab ->
                if (tab == vm.screenType) {
                    when (tab) {
                        0 -> { ColumnSelect_AddRColumn(Settings.r_explorerGifsTab_column_current_count) }
                    }
                }
                vm.screenType = tab
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {

            TabRow(
                containerColor = Theme.tabLevel0,
                titlesIcon = EXPLORER_TAB_ICONS,
                value = vm.screenType,
                onChangeState = onTabChange,
                overlay0 = { TabBarPoints(overlay0, vm.screenType == 0) },
            )


        }, containerColor = Theme.background) { paddingValues ->

            Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                when (vm.screenType) {
                    0 -> R_ScreenGifsTab.Content()
                    1 -> R_ScreenSavedTab.Content()
                    2 -> R_ScreenNichesTab.Content()
                    3 -> SearchTab.Content()
                    else -> R_ScreenGifsTab.Content()
                }
            }

        }

    }
}

@Stable
class ScreenRedExplorerSM @Inject constructor(
    private val navigationState: RNavigationState
) : ScreenModel {
    var screenType: Int
        get() = navigationState.rootTab
        set(value) {
            navigationState.rootTab = value
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenRedExplorerModule {
    @Binds
    @IntoMap
    @ScreenModelKey(ScreenRedExplorerSM::class)
    abstract fun bindScreenRedExplorerSM(sm: ScreenRedExplorerSM): ScreenModel
}
