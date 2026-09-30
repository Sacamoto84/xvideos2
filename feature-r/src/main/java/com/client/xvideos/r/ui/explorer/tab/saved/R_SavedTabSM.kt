package com.client.xvideos.r.ui.explorer.tab.saved

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.ui.explorer.RNavigationState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

@Stable
class R_SavedTabSM @Inject constructor(
    private val navigationState: RNavigationState,
    val savedRed: SavedRed
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
