package com.client.xvideos.n.ui.screens.explorer

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
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.n.ui.molecule.N_ExplorerBottomBar
import com.client.xvideos.n.ui.screens.explorer.tab.list.N_ScreenGalleryList
import com.client.xvideos.n.ui.screens.explorer.tab.saved.N_SavedTab
import com.client.xvideos.n.ui.screens.explorer.tab.search.N_ScreenGallerySearch

/**
 * Корневой экран-проводник раздела nhentai (аналог L_ScreenExplorer).
 */
class N_ScreenExplorer : Screen {

    override val key: ScreenKey = "N_ScreenExplorer"

    @Composable
    override fun Content() {
        val vm = getScreenModel<N_ScreenExplorerSM>()

        BackHandler(enabled = vm.screenType != 0) {
            vm.screenType = 0
        }

        val columnGalleriesTab by Settings.n_galleriesTab_column_current_count.field.collectAsStateWithLifecycle()

        val onTabChange: (Int) -> Unit = remember(vm) {
            { tab ->
                if (tab == vm.screenType && tab == 0) {
                    ColumnSelect_AddColumn(
                        Settings.n_galleriesTab_column_current_count,
                        Settings.n_galleriesTab_G_0_4,
                        minCol = 1
                    )
                }
                vm.screenType = tab
            }
        }

        N_ScreenExplorerContent(
            screenType = vm.screenType,
            columnCount = columnGalleriesTab,
            onTabChange = onTabChange
        )
    }
}

@Composable
fun N_ScreenExplorerContent(
    screenType: Int,
    columnCount: Int,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            N_ExplorerBottomBar(
                screenType = screenType,
                percentDownload = 0f,
                columnCount = columnCount,
                onTabChange = onTabChange
            )
        },
        containerColor = Theme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
            when (screenType) {
                0 -> N_ScreenGalleryList.Content()
                1 -> N_SavedTab.Content()
                2 -> N_ScreenGallerySearch.Content()
                else -> N_ScreenGalleryList.Content()
            }
        }
    }
}

@Preview
@Composable
private fun N_ScreenExplorerContentPreview() {
    N_ScreenExplorerContent(
        screenType = 0,
        columnCount = 2,
        onTabChange = {}
    )
}
