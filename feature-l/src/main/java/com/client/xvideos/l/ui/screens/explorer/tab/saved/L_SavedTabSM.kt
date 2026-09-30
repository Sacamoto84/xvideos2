package com.client.xvideos.l.ui.screens.explorer.tab.saved

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.ui.screens.explorer.LNavigationState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

@Stable
class L_SavedTabSM @Inject constructor(
    private val navigationState: LNavigationState,
    val savedL: SavedL
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
