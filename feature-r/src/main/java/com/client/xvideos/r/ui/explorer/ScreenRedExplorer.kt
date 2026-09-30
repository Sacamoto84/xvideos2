package com.client.xvideos.r.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.ui.explorer.molecule.ExplorerBottomBar
import com.client.xvideos.r.ui.explorer.tab.gifs.ColumnSelect_AddRColumn
import com.client.xvideos.r.ui.explorer.tab.gifs.R_ScreenGifsTab
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.explorer.tab.niches.R_ScreenNichesTab
import com.client.xvideos.r.ui.explorer.tab.saved.R_ScreenSavedTab
import com.client.xvideos.r.ui.explorer.tab.search.SearchTab

class ScreenRedExplorer : Screen {

    override val key: ScreenKey = "ScreenRedExplorer"

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

        ScreenRedExplorerContent(
            screenType = vm.screenType,
            overlay0 = overlay0,
            onTabChange = onTabChange
        )
    }
}

@Composable
fun ScreenRedExplorerContent(
    screenType: Int,
    overlay0: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ExplorerBottomBar(
                screenType = screenType,
                overlay0 = overlay0,
                onTabChange = onTabChange
            )
        },
        containerColor = Theme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
            when (screenType) {
                0 -> R_ScreenGifsTab.Content()
                1 -> R_ScreenSavedTab.Content()
                2 -> R_ScreenNichesTab.Content()
                3 -> SearchTab.Content()
                else -> R_ScreenGifsTab.Content()
            }
        }
    }
}

@Preview
@Composable
private fun ScreenRedExplorerPreview() {
    ScreenRedExplorerContent(
        screenType = 0,
        overlay0 = 2,
        onTabChange = {}
    )
}
