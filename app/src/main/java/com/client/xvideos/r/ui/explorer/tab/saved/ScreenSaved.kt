package com.client.xvideos.r.ui.explorer.tab.saved

import com.client.xvideos.common.theme.Theme

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.HorizontalDivider
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
import com.client.xvideos.r.ui.explorer.tab.FavoritesTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CollectionTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CreatorsTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_DownloadTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_LikesTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.SavedNichesTab
import com.client.xvideos.r.ui.explorer.tab.gifs.ColumnSelect_AddRColumn
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_SubscriptionsTab
import com.client.xvideos.r.ui.ui.atom.TabBarPoints
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import kotlinx.collections.immutable.persistentListOf

object R_ScreenSavedTab : Screen {

    private fun readResolve(): Any = R_ScreenSavedTab

    override val key: ScreenKey = uniqueScreenKey

    var screenType by mutableIntStateOf(0)

    val l = persistentListOf(
        Icons.Outlined.FavoriteBorder,
        Icons.Outlined.Person,
        Icons.Outlined.Group,
        Icons.Outlined.Save,
        //Icons.Outlined.Dataset,
        //Icons.Outlined.Folder,
        Icons.Outlined.Apps,
        Icons.Outlined.Subscriptions,

    )

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {

        val overlay0 = normalizeRColumnCount(
            Settings.r_likesTab_column_current_count.field.collectAsStateWithLifecycle().value
        )

        val overlay4 = normalizeRColumnCount(
            Settings.r_collectionTab_column_current_count.field.collectAsStateWithLifecycle().value
        )

        Scaffold(
            bottomBar = {
                Column {
                    HorizontalDivider()
                    TabRow(
                        value = screenType,
                        containerColor = Theme.tabLevel1,
                        //containerColor = Theme.R.colorBottomBarBackground,
                        titlesIcon = l,
                        onChangeState = {
                            if (it == screenType) {
                                when (it) {
                                    0 -> { ColumnSelect_AddRColumn(Settings.r_likesTab_column_current_count) }
                                    4 -> { ColumnSelect_AddRColumn(Settings.r_collectionTab_column_current_count) }
                                }
                            }
                            screenType = it
                        },
                        overlay0 = { TabBarPoints( overlay0, screenType == 0 ) },
                        overlay4 = { TabBarPoints( overlay4, screenType == 4 ) },
                    )
                }
            },

            modifier = Modifier.fillMaxSize(),
            containerColor = Theme.background
        ) { paddingValues ->

            Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                when (screenType) {
                    0 -> R_Screen_Saved_LikesTab.Content()
                    1 -> R_Screen_CreatorsTab.Content()
                    3 -> R_Screen_Saved_DownloadTab.Content()
                    2 -> SavedNichesTab.Content()
                    4 -> R_Screen_CollectionTab.Content()
                    5 -> R_Screen_Saved_SubscriptionsTab.Content()
                    else -> FavoritesTab.Content()
                }
            }
        }
    }
}
