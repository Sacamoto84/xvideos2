package com.client.xvideos.r.ui.explorer

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
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
private val l = persistentListOf(
    Icons.Outlined.Movie,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Group,
    Icons.Outlined.Search
)

class ScreenRedExplorer : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {
        val vm = getScreenModel<ScreenRedExplorerSM>()

        val overlay0 = normalizeRColumnCount(
            Settings.r_explorerGifsTab_column_current_count.field.collectAsStateWithLifecycle().value
        )

        Scaffold(bottomBar = {

            TabRow(
                containerColor = Theme.tabLevel0,
                titlesIcon = l,
                value = vm.screenType,
                onChangeState = {
                    if (it == vm.screenType) {
                        when (it) {
                            0 -> { ColumnSelect_AddRColumn(Settings.r_explorerGifsTab_column_current_count) }
                        }
                    }
                    vm.screenType = it
                },
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
