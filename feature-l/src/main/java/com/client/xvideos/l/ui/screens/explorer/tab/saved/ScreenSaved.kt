package com.client.xvideos.l.ui.screens.explorer.tab.saved

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.ScreenModelKey
import cafe.adriel.voyager.hilt.getScreenModel
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.l.ui.screens.explorer.LNavigationState
import com.client.xvideos.common.ui.TabRow
import com.client.xvideos.l.ui.screens.explorer.tab.saved.albums.L_ScreenSavedAlbumsTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.collection.L_Screen_CollectionTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.L_ScreenSavedLikesTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.likes.L_ScreenSavedLikesTab_AddColumn
import com.client.xvideos.l.ui.screens.explorer.tab.saved.serverLikes.L_ScreenServerLikesTab
import com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.L_ScreenSubscribedAlbumsTab
import com.client.xvideos.common.settings.ColumnSelect_AddColumn
import com.client.xvideos.common.ui.atom.TabBarPoints
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

object L_SavedTab : Screen {

    private fun readResolve(): Any = L_SavedTab

    override val key: ScreenKey = "L_SavedTab"

    private val SAVED_TAB_ICONS = persistentListOf(
        Icons.Outlined.Save,
        Icons.Outlined.Folder,
        Icons.Outlined.Apps,
        Icons.Outlined.Subscriptions,
        Icons.Outlined.FavoriteBorder,
    )

    @Composable
    override fun Content() {

        val vm = getScreenModel<L_SavedTabSM>()
        val screenType = vm.screenType

        BackHandler(enabled = vm.screenType != 0) {
            vm.screenType = 0
        }

        val columnLikes = Settings.l_likesTab_column_current_count.field.collectAsStateWithLifecycle().value

        val columnCollection = Settings.l_collectionTab_column_current_count.field.collectAsStateWithLifecycle().value

        Scaffold(
            bottomBar = {
                Column {
                    HorizontalDivider()
                    TabRow(
                        value = screenType,
                        containerColor = Theme.tabLevel1,
                        //containerColor = Theme.R.colorBottomBarBackground,
                        titlesIcon = SAVED_TAB_ICONS,
                        onChangeState = {
                            if (it == screenType) {
                                when (it) {
                                    0 -> L_ScreenSavedLikesTab_AddColumn()
                                    2 -> { ColumnSelect_AddColumn(Settings.l_collectionTab_column_current_count, Settings.l_collectionTab_G_0_4) }
                                    4 -> L_ScreenSavedLikesTab_AddColumn()
                                }
                            }
                            vm.screenType = it
                        },
                        overlay0 = { TabBarPoints(columnLikes, screenType == 0) },
                        overlay2 = { TabBarPoints(columnCollection, screenType == 2) },
                        overlay4 = { TabBarPoints(columnLikes, screenType == 4) }
                    )
                }
            },

            modifier = Modifier.fillMaxSize(),
            containerColor = Theme.background
        ) { paddingValues ->

            Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
                when (screenType) {
                    0 -> L_ScreenSavedLikesTab.Content()
                    1 -> L_ScreenSavedAlbumsTab.Content()
                    2 -> L_Screen_CollectionTab.Content()
                    3 -> L_ScreenSubscribedAlbumsTab.Content()
                    4 -> L_ScreenServerLikesTab.Content()
                    else -> {}
                }
            }
        }
    }
}

@Stable
class L_SavedTabSM @Inject constructor(
    private val navigationState: LNavigationState
) : ScreenModel {
    var screenType: Int
        get() = navigationState.savedTab
        set(value) {
            navigationState.savedTab = value
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class L_SavedTabModule {
    @Binds
    @IntoMap
    @ScreenModelKey(L_SavedTabSM::class)
    abstract fun bindL_SavedTabSM(sm: L_SavedTabSM): ScreenModel
}
