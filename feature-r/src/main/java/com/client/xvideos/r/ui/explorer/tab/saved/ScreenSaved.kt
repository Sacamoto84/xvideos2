package com.client.xvideos.r.ui.explorer.tab.saved

import com.client.xvideos.common.theme.Theme

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CollectionTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_CreatorsTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_DownloadTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_LikesTab
import com.client.xvideos.r.ui.explorer.tab.saved.tab.savedNiche.SavedNichesTab
import com.client.xvideos.r.ui.explorer.tab.gifs.ColumnSelect_AddRColumn
import com.client.xvideos.r.ui.explorer.tab.saved.tab.R_Screen_Saved_SubscriptionsTab
import com.client.xvideos.common.ui.atom.TabBarPoints
import com.client.xvideos.r.ui.explorer.tab.gifs.normalizeRColumnCount
import kotlinx.collections.immutable.persistentListOf

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.r.ui.explorer.RNavigationState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

object R_ScreenSavedTab : Screen {

    private fun readResolve(): Any = R_ScreenSavedTab

    override val key: ScreenKey = "R_ScreenSavedTab"

    private val SAVED_TAB_ICONS = persistentListOf(
        Icons.Outlined.FavoriteBorder,
        Icons.Outlined.Person,
        Icons.Outlined.Group,
        Icons.Outlined.Save,
        //Icons.Outlined.Dataset,
        //Icons.Outlined.Folder,
        Icons.Outlined.Apps,
        Icons.Outlined.Subscriptions,

    )

    @Composable
    override fun Content() {
        val vm = getScreenModel<R_SavedTabSM>()

        BackHandler(enabled = vm.screenType != 0) {
            vm.screenType = 0
        }

        val overlay0 = normalizeRColumnCount(
            Settings.r_likesTab_column_current_count.field.collectAsStateWithLifecycle().value
        )

        val overlay4 = normalizeRColumnCount(
            Settings.r_collectionTab_column_current_count.field.collectAsStateWithLifecycle().value
        )

        val onTabChange: (Int) -> Unit = remember(vm) {
            { tab ->
                if (tab == vm.screenType) {
                    when (tab) {
                        0 -> { ColumnSelect_AddRColumn(Settings.r_likesTab_column_current_count) }
                        4 -> { ColumnSelect_AddRColumn(Settings.r_collectionTab_column_current_count) }
                    }
                }
                vm.screenType = tab
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column {
                    HorizontalDivider()
                    TabRow(
                        value = vm.screenType,
                        containerColor = Theme.tabLevel1,
                        //containerColor = Theme.R.colorBottomBarBackground,
                        titlesIcon = SAVED_TAB_ICONS,
                        onChangeState = onTabChange,
                        overlay0 = { TabBarPoints( overlay0, vm.screenType == 0 ) },
                        overlay4 = { TabBarPoints( overlay4, vm.screenType == 4 ) },
                    )
                }
            },

            modifier = Modifier.fillMaxSize(),
            containerColor = Theme.background
        ) { paddingValues ->

            Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                when (vm.screenType) {
                    0 -> R_Screen_Saved_LikesTab.Content()
                    1 -> R_Screen_CreatorsTab.Content()
                    3 -> R_Screen_Saved_DownloadTab.Content()
                    2 -> SavedNichesTab.Content()
                    4 -> R_Screen_CollectionTab.Content()
                    5 -> R_Screen_Saved_SubscriptionsTab.Content()
                    else -> R_Screen_Saved_LikesTab.Content()
                }
            }
        }
    }
}

@Stable
class R_SavedTabSM @Inject constructor(
    private val navigationState: RNavigationState
) : ScreenModel {
    var screenType: Int
        get() = navigationState.savedTab
        set(value) {
            navigationState.savedTab = value
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class R_SavedTabModule {
    @Binds
    @IntoMap
    @ScreenModelKey(R_SavedTabSM::class)
    abstract fun bindR_SavedTabSM(sm: R_SavedTabSM): ScreenModel
}
