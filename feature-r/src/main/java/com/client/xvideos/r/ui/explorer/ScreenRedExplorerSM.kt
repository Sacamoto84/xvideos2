package com.client.xvideos.r.ui.explorer

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

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
