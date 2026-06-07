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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.ui.screens.TabRow
import com.client.xvideos.r.ui.explorer.tab.gifs.ColumnSelect_AddRColumn
import com.client.xvideos.r.ui.explorer.tab.gifs.R_ScreenGifsTab
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import com.client.xvideos.r.ui.explorer.tab.niches.R_ScreenNichesTab
import com.client.xvideos.r.ui.explorer.tab.saved.R_ScreenSavedTab
import com.client.xvideos.r.ui.explorer.tab.search.SearchTab

import com.client.xvideos.r.ui.ui.atom.TabBarPoints

private val l = listOf(
    Icons.Outlined.Movie,
    Icons.Outlined.Group,
    Icons.Outlined.BookmarkBorder,
    Icons.Outlined.Search
)

class ScreenRedExplorer : Screen {

    override val key: ScreenKey = uniqueScreenKey

    companion object {
        var screenType by mutableIntStateOf(0)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val overlay0 = normalizeRColumnCount(
            Settings.r_explorerGifsTab_column_current_count.field.collectAsStateWithLifecycle().value
        )

        Scaffold(bottomBar = {

            TabRow(
                containerColor = Theme.tabLevel0,
                titlesIcon = l,
                value = screenType,
                onChangeState = {
                    if (it == screenType) {
                        when (it) {
                            0 -> { ColumnSelect_AddRColumn(Settings.r_explorerGifsTab_column_current_count) }
                        }
                    }
                    screenType = it
                },
                overlay0 = { TabBarPoints(overlay0, screenType == 0) },
            )


        }, containerColor = Theme.background) { paddingValues ->
            Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                when (screenType) {
                    0 -> R_ScreenGifsTab.Content()
                    1 -> R_ScreenNichesTab.Content()
                    2 -> R_ScreenSavedTab.Content()
                    3 -> SearchTab.Content()
                    else -> R_ScreenGifsTab.Content()
                }
            }
        }

    }
}
